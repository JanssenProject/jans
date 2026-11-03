/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

/**
 * What {@link WellKnownFetcher} got back from a relying party's server.
 * <p>
 * Either a {@link Failure} (nothing usable came back) or an HTTP response. A redirect is a response: its
 * status and {@code Location} are reported as data and never followed. Failures are coarse on purpose, so
 * that a caller cannot use them to probe what is reachable from the server's network.
 */
public final class WellKnownFetchResult {

    /** Why no response was obtained. {@code NONE} means a response, of any status, was received. */
    public enum Failure {
        NONE, UNRESOLVABLE, BLOCKED_ADDRESS, TIMEOUT, TLS_FAILED, CONNECTION_FAILED, ERROR
    }

    private final Failure failure;
    private final int status;
    private final String contentType;
    private final String location;
    private final byte[] body;
    private final boolean truncated;

    private WellKnownFetchResult(Failure failure, int status, String contentType, String location, byte[] body,
            boolean truncated) {
        this.failure = failure;
        this.status = status;
        this.contentType = contentType;
        this.location = location;
        this.body = body;
        this.truncated = truncated;
    }

    static WellKnownFetchResult failed(Failure failure) {
        return new WellKnownFetchResult(failure, 0, null, null, new byte[0], false);
    }

    static WellKnownFetchResult response(int status, String contentType, String location, byte[] body,
            boolean truncated) {
        return new WellKnownFetchResult(Failure.NONE, status, contentType, location, body, truncated);
    }

    public Failure failure() {
        return failure;
    }

    /** True when an HTTP response of any status was received. */
    public boolean hasResponse() {
        return failure == Failure.NONE;
    }

    /** HTTP status, or 0 when there was no response. */
    public int status() {
        return status;
    }

    /** The {@code Content-Type} header, or null when absent or when there was no response. */
    public String contentType() {
        return contentType;
    }

    /** The {@code Location} header of a 3xx response, reported as data only; null otherwise. */
    public String location() {
        return location;
    }

    /** The response body, cut at the size cap. Empty when there was no response. */
    public byte[] body() {
        return body.clone();
    }

    /** True when the body was longer than the size cap and was cut. */
    public boolean truncated() {
        return truncated;
    }
}
