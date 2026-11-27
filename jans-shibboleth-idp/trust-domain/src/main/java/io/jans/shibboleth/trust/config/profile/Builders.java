package io.jans.shibboleth.trust.config.profile;

import io.jans.kernel.DomainError;
import io.jans.kernel.Result;

final class Builders {

    private Builders() {
    }

    static <T> T require(Result<T> step) {

        if (step.isFailure()) {

            throw new Failure(step.getError());
        }

        return step.getValue();
    }

    static final class Failure extends RuntimeException {

        final DomainError error;

        Failure(DomainError error) {

            super(null, null, false, false);
            this.error = error;
        }
    }
}
