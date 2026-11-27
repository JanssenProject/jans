/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.server.attestation;

/**
 * Outcome of {@link AkBindingVerifier}.
 */
public class AkBindingResult {

    private final boolean shown;
    private final String method;
    private final String detail;

    private AkBindingResult(boolean shown, String method, String detail) {
        this.shown = shown;
        this.method = method;
        this.detail = detail;
    }

    public static AkBindingResult shown(String method) {
        return new AkBindingResult(true, method, null);
    }

    public static AkBindingResult notShown(String detail) {
        return new AkBindingResult(false, null, detail);
    }

    public boolean isShown() {
        return shown;
    }

    public String getMethod() {
        return method;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        return "AkBindingResult{shown=" + shown + ", method='" + method + "', detail='" + detail + "'}";
    }
}
