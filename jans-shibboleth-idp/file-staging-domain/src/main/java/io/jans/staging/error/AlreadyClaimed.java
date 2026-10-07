package io.jans.staging.error;

/**
 * The staged file has already been claimed to a different destination; a re-claim can only be
 * idempotent to the same destination. Maps to HTTP 409.
 */
public final class AlreadyClaimed extends StagingError {

    private AlreadyClaimed() {

        super("The staged file has already been claimed");
    }

    public static AlreadyClaimed instance() {

        return new AlreadyClaimed();
    }
}
