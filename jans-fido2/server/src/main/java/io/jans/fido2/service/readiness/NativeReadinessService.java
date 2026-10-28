/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.Map;

import org.slf4j.Logger;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.RequestedParty;
import io.jans.fido2.service.readiness.WellKnownFetchResult.Failure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Runs the native app readiness checks for one configured relying party: feature gate, lookup, RP ID
 * validation, per-RP result cache, rate limits, then the reachability and content checks.
 * <p>
 * The only input that reaches the network layer is the RP ID stored in the configuration, validated by
 * {@link RpIdValidator}. The caller supplies an id to select a configured entry, never a host, URL, port or
 * path, so an id that is not configured is answered with {@link NotFound} and no outbound work.
 * <p>
 * <b>Limits.</b> The configured response size and timeout are clamped to {@link #MAX_RESPONSE_BYTES_LIMIT}
 * and {@link #TIMEOUT_MS_LIMIT}; a value that is not positive falls back to its default. A mis-set property
 * therefore cannot remove the cap.
 * <p>
 * <b>Cache.</b> In memory, per node, one entry per configured RP, so its size is bounded by the
 * configuration. Entries live for {@code nativeReadinessCacheTtlSeconds}, but never less than
 * {@link ReadinessRateLimiter#MIN_RECHECK_INTERVAL_MS}. In a cluster that means up to one fetch per node per
 * TTL, which {@link ReadinessRateLimiter} keeps bounded.
 * <p>
 * <b>Audit.</b> One log line per executed check with {@code rpId}, {@code check}, {@code requestedBy},
 * {@code timestamp}, {@code result} and {@code resolvedIpClass}; a cache hit is one line and no fetch. The
 * IP class is a coarse label (never the address). {@code requestedBy} is whatever the caller forwarded: the
 * FIDO2 server does not authenticate callers, so the value can be forged by anything that reaches it
 * directly, and is informational until the Config API hop that sets it is trusted.
 */
@ApplicationScoped
public class NativeReadinessService {

    /** The fixed upper bound for {@code nativeReadinessMaxResponseBytes}. */
    public static final int MAX_RESPONSE_BYTES_LIMIT = 256 * 1024;
    /** The fixed upper bound for {@code nativeReadinessTimeoutMs}. */
    public static final int TIMEOUT_MS_LIMIT = 10_000;

    static final int DEFAULT_MAX_RESPONSE_BYTES = 131072;
    static final int DEFAULT_TIMEOUT_MS = 3000;
    static final int DEFAULT_CACHE_TTL_SECONDS = 3600;

    private static final String UNKNOWN = "unknown";
    private static final int MAX_AUDIT_VALUE_LENGTH = 128;
    private static final String ID_CONFIG_RP_ID = "config.rp-id";
    private static final String ID_CONFIG_ANDROID = "config.android";
    private static final String ID_CONFIG_IOS = "config.ios";
    private static final String ID_FETCH = "fetch.skipped";
    private static final String IP_NONE = "none";

    /** The requested relying party is not configured. */
    public static final class NotFound extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public NotFound() {
            super("relying party not configured", null, false, false);
        }
    }

    /** A rate limit refused the request; {@link #retryAfterSeconds()} says when to ask again. */
    public static final class RateLimited extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final long retryAfterSeconds;

        public RateLimited(long retryAfterSeconds) {
            super("rate limited", null, false, false);
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long retryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    private static final class Entry {
        private final NativeReadinessResult result;
        private final long expiresAt;

        Entry(NativeReadinessResult result, long expiresAt) {
            this.result = result;
            this.expiresAt = expiresAt;
        }
    }

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ReachabilityChecker reachabilityChecker;

    @Inject
    private ContentChecker contentChecker;

    @Inject
    private ReadinessRateLimiter rateLimiter;

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    /** Package-visible so a test can move time. */
    LongSupplier clock = System::currentTimeMillis;

    /**
     * @param rpConfigId  the RP id exactly as configured in {@code requestedParties}
     * @param recheck     skip the cache (still rate limited)
     * @param requestedBy who asked, as forwarded by the caller; null when not known
     * @throws NotFound     when no configured RP has that id
     * @throws RateLimited  when a rate limit refuses a check that would fetch
     */
    public NativeReadinessResult check(String rpConfigId, boolean recheck, String requestedBy) {
        Fido2Configuration config = appConfiguration.getFido2Configuration();
        if (config == null || !config.isNativeReadinessEnabled()) {
            return NativeReadinessResult.disabled();
        }
        RequestedParty party = find(config, rpConfigId);
        if (party == null) {
            throw new NotFound();
        }
        String who = sanitize(requestedBy);
        String rp = sanitize(party.getId());

        RpIdValidator.Result validated = RpIdValidator.validate(party.getId());
        if (!validated.isValid()) {
            return networkFree(party, who, rp, config, new CheckResult(ID_CONFIG_RP_ID, CheckStatus.FAIL,
                    "the relying party ID is not a host name that can be checked"));
        }
        int files = fileCount(party);
        if (!config.isNativeReadinessAllowExternalFetch() || files == 0) {
            return networkFree(party, who, rp, config, null);
        }

        if (!recheck) {
            Entry hit = cache.get(party.getId());
            if (hit != null && hit.expiresAt > clock.getAsLong()) {
                audit(rp, "cache.hit", who, CheckStatus.PASS, IP_NONE);
                return hit.result.asCached();
            }
        }
        return fetchAndCheck(config, party, validated.rpId(), files, who, rp);
    }

    private NativeReadinessResult fetchAndCheck(Fido2Configuration config, RequestedParty party,
            ValidatedRpId rpId, int files, String who, String rp) {
        long retryMs = rateLimiter.tryAcquire(party.getId(), files);
        if (retryMs > 0) {
            throw new RateLimited((retryMs + 999) / 1000);
        }
        try {
            WellKnownFetcher fetcher = fetcher(config);
            List<CheckResult> android = androidChecks(party, rpId, fetcher, who, rp);
            List<CheckResult> ios = iosChecks(party, rpId, fetcher, who, rp);
            long now = clock.getAsLong();
            NativeReadinessResult result = NativeReadinessResult.of(true, android, ios, now,
                    rateLimiter.nextAllowedAt(party.getId()));
            cache.put(party.getId(), new Entry(result, now + ttlMs(config)));
            return result;
        } finally {
            rateLimiter.release();
        }
    }

    private List<CheckResult> androidChecks(RequestedParty party, ValidatedRpId rpId, WellKnownFetcher fetcher,
            String who, String rp) {
        if (ExpectedAssociationFileBuilder.androidApps(party).isEmpty()) {
            return noApps(ID_CONFIG_ANDROID, "no Android apps configured; assetlinks.json was not fetched", who, rp);
        }
        ReachabilityReport report = reachabilityChecker.checkAssetLinks(rpId, fetcher);
        List<CheckResult> checks = new ArrayList<>(report.checks());
        if (readable(report.fetch())) {
            checks.addAll(contentChecker.checkAssetLinks(party, report.fetch().body()));
        }
        auditAll(rp, who, checks, ipClass(report.fetch()));
        return checks;
    }

    private List<CheckResult> iosChecks(RequestedParty party, ValidatedRpId rpId, WellKnownFetcher fetcher,
            String who, String rp) {
        if (ExpectedAssociationFileBuilder.iosApps(party).isEmpty()) {
            return noApps(ID_CONFIG_IOS, "no iOS apps configured; apple-app-site-association was not fetched",
                    who, rp);
        }
        ReachabilityReport report = reachabilityChecker.checkAppleAppSiteAssociation(rpId, fetcher);
        List<CheckResult> checks = new ArrayList<>(report.checks());
        if (readable(report.fetch())) {
            checks.addAll(contentChecker.checkAppleAppSiteAssociation(party, report.fetch().body()));
        }
        auditAll(rp, who, checks, ipClass(report.fetch()));
        return checks;
    }

    private List<CheckResult> noApps(String id, String reason, String who, String rp) {
        List<CheckResult> checks = List.of(new CheckResult(id, CheckStatus.PASS, reason));
        auditAll(rp, who, checks, IP_NONE);
        return checks;
    }

    /** The answer when nothing is fetched: the RP ID is unusable, fetching is off, or no app is configured. */
    private NativeReadinessResult networkFree(RequestedParty party, String who, String rp, Fido2Configuration config,
            CheckResult problem) {
        List<CheckResult> android = new ArrayList<>();
        List<CheckResult> ios = new ArrayList<>();
        if (problem != null) {
            android.add(problem);
        }
        int androidApps = ExpectedAssociationFileBuilder.androidApps(party).size();
        int iosApps = ExpectedAssociationFileBuilder.iosApps(party).size();
        android.add(new CheckResult(ID_CONFIG_ANDROID, androidApps > 0 ? CheckStatus.PASS : CheckStatus.WARN,
                androidApps + " Android app(s) configured"));
        ios.add(new CheckResult(ID_CONFIG_IOS, iosApps > 0 ? CheckStatus.PASS : CheckStatus.WARN,
                iosApps + " iOS app(s) configured"));
        if (!config.isNativeReadinessAllowExternalFetch()) {
            android.add(new CheckResult(ID_FETCH, CheckStatus.WARN,
                    "external fetch is disabled; the deployed files were not checked"));
        }
        auditAll(rp, who, android, IP_NONE);
        auditAll(rp, who, ios, IP_NONE);
        long now = clock.getAsLong();
        return NativeReadinessResult.of(config.isNativeReadinessAllowExternalFetch(), android, ios, now, now);
    }

    private static boolean readable(WellKnownFetchResult fetch) {
        return fetch.hasResponse() && fetch.status() == 200 && !fetch.truncated();
    }

    private static int fileCount(RequestedParty party) {
        return (ExpectedAssociationFileBuilder.androidApps(party).isEmpty() ? 0 : 1)
                + (ExpectedAssociationFileBuilder.iosApps(party).isEmpty() ? 0 : 1);
    }

    static RequestedParty find(Fido2Configuration config, String id) {
        if (id == null || config.getRequestedParties() == null) {
            return null;
        }
        for (RequestedParty party : config.getRequestedParties()) {
            if (party != null && id.equals(party.getId())) {
                return party;
            }
        }
        return null;
    }

    static WellKnownFetcher fetcher(Fido2Configuration config) {
        int bytes = clamp(config.getNativeReadinessMaxResponseBytes(), DEFAULT_MAX_RESPONSE_BYTES,
                MAX_RESPONSE_BYTES_LIMIT);
        int timeout = clamp(config.getNativeReadinessTimeoutMs(), DEFAULT_TIMEOUT_MS, TIMEOUT_MS_LIMIT);
        return new WellKnownFetcher(bytes, timeout, timeout, timeout);
    }

    static int clamp(int configured, int fallback, int limit) {
        if (configured <= 0) {
            return fallback;
        }
        return Math.min(configured, limit);
    }

    private static long ttlMs(Fido2Configuration config) {
        int seconds = config.getNativeReadinessCacheTtlSeconds() > 0 ? config.getNativeReadinessCacheTtlSeconds()
                : DEFAULT_CACHE_TTL_SECONDS;
        return Math.max(seconds * 1000L, ReadinessRateLimiter.MIN_RECHECK_INTERVAL_MS);
    }

    private static String ipClass(WellKnownFetchResult fetch) {
        Failure failure = fetch.failure();
        if (failure == Failure.UNRESOLVABLE) {
            return "unresolved";
        }
        if (failure == Failure.BLOCKED_ADDRESS) {
            return "blocked";
        }
        return failure == Failure.ERROR ? UNKNOWN : "public";
    }

    private void auditAll(String rp, String who, List<CheckResult> checks, String ipClass) {
        for (CheckResult check : checks) {
            audit(rp, check.id(), who, check.status(), ipClass);
        }
    }

    private void audit(String rp, String check, String who, CheckStatus result, String ipClass) {
        log.info("native-readiness-audit rpId={} check={} requestedBy={} timestamp={} result={} resolvedIpClass={}",
                rp, check, who, Instant.ofEpochMilli(clock.getAsLong()), result.name().toLowerCase(Locale.ROOT),
                ipClass);
    }

    /** Keeps a value safe to write on one log line: a short run of plain characters. */
    static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return UNKNOWN;
        }
        String cut = value.length() > MAX_AUDIT_VALUE_LENGTH ? value.substring(0, MAX_AUDIT_VALUE_LENGTH) : value;
        StringBuilder out = new StringBuilder(cut.length());
        for (int i = 0; i < cut.length(); i++) {
            char c = cut.charAt(i);
            boolean plain = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '.' || c == '-' || c == '_' || c == '@' || c == ':' || c == '/';
            out.append(plain ? c : '_');
        }
        return out.toString();
    }
}
