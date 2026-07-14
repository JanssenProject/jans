package io.jans.shibboleth.trust.shared;

/**
 * Root of every domain error across the trust subdomain. Both context error
 * families ({@code TrustError}, {@code ActivationError}) and any neutral
 * shared-kernel error extend it, so a single {@code Result<T>} can carry the
 * error of either context.
 *
 * <p>The {@code message} is {@code protected} and non-final: a few errors
 * compose their message after {@code super(...)}, so subclasses may set it.
 */
public abstract class DomainError {

    protected String message;

    protected DomainError(String message) {

        this.message = message;
    }

    public String getMessage() {

        return message;
    }

    @Override
    public String toString() {

        return message;
    }
}
