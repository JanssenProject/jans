package io.jans.shibboleth.trust.api.problem;

import io.jans.kernel.DomainError;

public class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient DomainError error;

    public DomainException(DomainError error) {

        super(error != null ? error.getMessage() : null);
        this.error = error;
    }

    public DomainError getError() {

        return error;
    }
}
