/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

/**
 * One readiness check: a stable id, a status and a short reason.
 * <p>
 * The reason is a fixed string written in code, plus at most a value the administrator already configured
 * (the RP ID) or an HTTP status code. It never carries fetched content, response headers or exception text,
 * so a result is safe to return to the caller as is.
 */
public final class CheckResult {

    private final String id;
    private final CheckStatus status;
    private final String reason;

    CheckResult(String id, CheckStatus status, String reason) {
        this.id = id;
        this.status = status;
        this.reason = reason;
    }

    public String id() {
        return id;
    }

    public CheckStatus status() {
        return status;
    }

    public String reason() {
        return reason;
    }

    @Override
    public String toString() {
        return id + " " + status + ": " + reason;
    }
}
