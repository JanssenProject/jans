package io.jans.shibboleth.trust.config.error;

import io.jans.kernel.DomainError;


public abstract class TrustError extends DomainError {

    protected TrustError(String message) {

        super(message);
    }
}
