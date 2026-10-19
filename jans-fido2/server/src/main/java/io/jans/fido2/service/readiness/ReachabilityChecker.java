/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.jans.fido2.service.readiness.WellKnownFetchResult.Failure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Decides whether a relying party's association files are reachable the way Android and iOS need them:
 * answered with 200 by the RP ID host itself, with a JSON content type, within the size and time limits.
 * <p>
 * One fetch is made per file, and every check is derived from that single result. Check ids and rules:
 * <ul>
 * <li>{@code reachability.status}: PASS for HTTP 200. FAIL for any other status (the code is stated), for a
 * redirect ("not followed"), and for a failed fetch (fixed reason: not resolvable, address not allowed,
 * TLS failure, connection failure).</li>
 * <li>{@code reachability.redirect} (3xx only): FAIL, stating whether the target is the same host, the same
 * host with {@code www.} added, or another host. Neither platform follows redirects. The {@code Location}
 * value itself is never reported.</li>
 * <li>{@code reachability.host} (any non-redirect response): PASS, naming the RP ID host that answered.</li>
 * <li>{@code reachability.content-type} (HTTP 200 only; the media type is compared, parameters such as
 * {@code charset} are ignored): PASS for {@code application/json}. A missing header is FAIL. Any other type
 * is FAIL for {@code assetlinks.json}, which Google documents as requiring {@code application/json}, and
 * WARN for {@code apple-app-site-association}, where the {@code application/json} requirement is a
 * convention rather than something known to be enforced.</li>
 * <li>{@code reachability.limits}: FAIL for a body over the size limit or a timeout, PASS for a response
 * within the size limit.</li>
 * </ul>
 * The size and time limits are those of {@link WellKnownFetcher}'s defaults.
 */
@ApplicationScoped
public class ReachabilityChecker {

    private static final String JSON = "application/json";

    @Inject
    private WellKnownFetcher fetcher;

    public ReachabilityReport checkAssetLinks(ValidatedRpId rpId) {
        return report(rpId, fetcher.fetchAssetLinks(rpId), CheckStatus.FAIL);
    }

    public ReachabilityReport checkAppleAppSiteAssociation(ValidatedRpId rpId) {
        return report(rpId, fetcher.fetchAppleAppSiteAssociation(rpId), CheckStatus.WARN);
    }

    private static ReachabilityReport report(ValidatedRpId rpId, WellKnownFetchResult fetch,
            CheckStatus wrongContentType) {
        List<CheckResult> checks = new ArrayList<>();
        if (!fetch.hasResponse()) {
            checks.add(failure(fetch.failure()));
            return new ReachabilityReport(fetch, checks);
        }

        int status = fetch.status();
        boolean redirect = status >= 300 && status < 400;
        if (status == 200) {
            checks.add(new CheckResult("reachability.status", CheckStatus.PASS, "HTTP 200"));
        } else if (redirect) {
            checks.add(new CheckResult("reachability.status", CheckStatus.FAIL,
                    "HTTP " + status + ": redirects are not followed, and Android and iOS do not follow them"));
            checks.add(new CheckResult("reachability.redirect", CheckStatus.FAIL,
                    redirectReason(rpId.hostname(), fetch.location())));
        } else {
            checks.add(new CheckResult("reachability.status", CheckStatus.FAIL, "HTTP " + status));
        }

        if (!redirect) {
            checks.add(new CheckResult("reachability.host", CheckStatus.PASS, "answered by " + rpId.hostname()));
        }
        if (status == 200) {
            checks.add(contentType(fetch.contentType(), wrongContentType));
        }
        checks.add(fetch.truncated()
                ? new CheckResult("reachability.limits", CheckStatus.FAIL, "response larger than limit")
                : new CheckResult("reachability.limits", CheckStatus.PASS, "response within size limit"));
        return new ReachabilityReport(fetch, checks);
    }

    private static CheckResult failure(Failure failure) {
        switch (failure) {
        case TIMEOUT:
            return new CheckResult("reachability.limits", CheckStatus.FAIL, "no response within limit");
        case UNRESOLVABLE:
            return fail("host name could not be resolved");
        case BLOCKED_ADDRESS:
            return fail("host resolves to an address that is not allowed");
        case TLS_FAILED:
            return fail("TLS connection failed");
        case CONNECTION_FAILED:
            return fail("could not connect to the host");
        default:
            return fail("request failed");
        }
    }

    private static CheckResult fail(String reason) {
        return new CheckResult("reachability.status", CheckStatus.FAIL, reason);
    }

    private static CheckResult contentType(String header, CheckStatus wrongType) {
        if (header == null || header.trim().isEmpty()) {
            return new CheckResult("reachability.content-type", CheckStatus.FAIL, "no Content-Type header");
        }
        String mediaType = header.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (JSON.equals(mediaType)) {
            return new CheckResult("reachability.content-type", CheckStatus.PASS, "Content-Type is " + JSON);
        }
        return new CheckResult("reachability.content-type", wrongType, "Content-Type is not " + JSON);
    }

    private static String redirectReason(String rpHost, String location) {
        String target = hostOf(location);
        if (target == null) {
            return "redirect to a path on the same host";
        }
        if (target.isEmpty()) {
            return "redirect target could not be determined";
        }
        if (target.equals(rpHost)) {
            return "redirect to a path on the same host";
        }
        if (target.equals("www." + rpHost)) {
            return "redirect to www." + rpHost + ", the same name with www. added";
        }
        return "redirect to a different host";
    }

    /** The lower-case host of an absolute location, null for a relative one, "" when it cannot be read. */
    private static String hostOf(String location) {
        if (location == null) {
            return "";
        }
        try {
            URI uri = new URI(location.trim());
            if (uri.getHost() != null) {
                return uri.getHost().toLowerCase(Locale.ROOT);
            }
            return uri.isAbsolute() || uri.getRawAuthority() != null ? "" : null;
        } catch (java.net.URISyntaxException e) {
            return "";
        }
    }
}
