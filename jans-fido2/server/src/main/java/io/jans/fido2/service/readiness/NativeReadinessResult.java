/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The structured answer of the native app readiness endpoint.
 * <p>
 * It is built only from {@link CheckResult}s, whose reasons are fixed strings, so it holds no fetched
 * content, response headers, resolved addresses or exception text. Serialised by getter.
 */
public final class NativeReadinessResult {

    /** One check as it appears in the response: {@code status} is {@code pass}, {@code warn} or {@code fail}. */
    public static final class Check {
        private final String check;
        private final String status;
        private final String reason;

        Check(CheckResult result) {
            this.check = result.id();
            this.status = result.status().name().toLowerCase(Locale.ROOT);
            this.reason = result.reason();
        }

        public String getCheck() {
            return check;
        }

        public String getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }
    }

    private final boolean enabled;
    private final boolean externalFetch;
    private final Map<String, List<Check>> platforms;
    private final String checkedAt;
    private final boolean fromCache;
    private final String nextRecheckAllowedAt;

    private NativeReadinessResult(boolean enabled, boolean externalFetch, Map<String, List<Check>> platforms,
            String checkedAt, boolean fromCache, String nextRecheckAllowedAt) {
        this.enabled = enabled;
        this.externalFetch = externalFetch;
        this.platforms = platforms;
        this.checkedAt = checkedAt;
        this.fromCache = fromCache;
        this.nextRecheckAllowedAt = nextRecheckAllowedAt;
    }

    /** The fixed answer while {@code nativeReadinessEnabled} is false. */
    static NativeReadinessResult disabled() {
        return new NativeReadinessResult(false, false, new LinkedHashMap<>(), null, false, null);
    }

    static NativeReadinessResult of(boolean externalFetch, List<CheckResult> android, List<CheckResult> ios,
            long checkedAtMs, long nextRecheckAllowedAtMs) {
        Map<String, List<Check>> platforms = new LinkedHashMap<>();
        platforms.put("android", convert(android));
        platforms.put("ios", convert(ios));
        return new NativeReadinessResult(true, externalFetch, platforms, Instant.ofEpochMilli(checkedAtMs).toString(),
                false, Instant.ofEpochMilli(nextRecheckAllowedAtMs).toString());
    }

    NativeReadinessResult asCached() {
        return new NativeReadinessResult(enabled, externalFetch, platforms, checkedAt, true, nextRecheckAllowedAt);
    }

    private static List<Check> convert(List<CheckResult> results) {
        List<Check> checks = new ArrayList<>();
        for (CheckResult result : results) {
            checks.add(new Check(result));
        }
        return List.copyOf(checks);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isExternalFetch() {
        return externalFetch;
    }

    public Map<String, List<Check>> getPlatforms() {
        return platforms;
    }

    public String getCheckedAt() {
        return checkedAt;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public String getNextRecheckAllowedAt() {
        return nextRecheckAllowedAt;
    }
}
