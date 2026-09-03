package io.jans.staging.error;

import io.jans.kernel.DomainError;

/**
 * Root of the file-staging context's error family. Extends the shared-kernel {@link DomainError}
 * so staging failures flow through the same {@code Result} type as every other context.
 */
public class StagingError extends DomainError {

    protected StagingError(String message) {

        super(message);
    }
}
