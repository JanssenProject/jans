package io.jans.staging.error;

/**
 * The staged file's token has expired and can no longer be claimed. Maps to HTTP 409.
 */
public final class TokenExpired extends StagingError {

    private TokenExpired() {

        super("The staged file's token has expired");
    }

    public static TokenExpired instance() {

        return new TokenExpired();
    }
}
