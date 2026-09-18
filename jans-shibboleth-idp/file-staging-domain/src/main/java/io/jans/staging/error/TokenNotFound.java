package io.jans.staging.error;

/**
 * No staged file exists for the presented token (never staged, or already reaped). Maps to HTTP 404.
 */
public final class TokenNotFound extends StagingError {

    private TokenNotFound() {

        super("No staged file exists for the token");
    }

    public static TokenNotFound instance() {

        return new TokenNotFound();
    }
}
