/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import java.util.List;

/**
 * Reachability checks for one association file, together with the fetch they were derived from.
 * <p>
 * The fetch is returned so that the content checks can read the body without fetching a second time.
 * It holds raw remote content: use it for parsing only, never put it in a response.
 */
public final class ReachabilityReport {

    private final WellKnownFetchResult fetch;
    private final List<CheckResult> checks;

    ReachabilityReport(WellKnownFetchResult fetch, List<CheckResult> checks) {
        this.fetch = fetch;
        this.checks = List.copyOf(checks);
    }

    public WellKnownFetchResult fetch() {
        return fetch;
    }

    public List<CheckResult> checks() {
        return checks;
    }
}
