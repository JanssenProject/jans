package io.jans.shibboleth.trust.activation.error;

import io.jans.kernel.DomainError;

public abstract class ActivationError extends DomainError {

    protected ActivationError(String message) {

        super(message);
    }
}
