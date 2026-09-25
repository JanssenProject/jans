package io.jans.kernel;

/**
 * Root of every domain error. Each consuming module's error families extend it, so a single
 * {@link Result} can carry the error of any bounded context.
 *
 * <p>The {@code message} is {@code protected} and non-final: a few errors compose their message
 * after {@code super(...)}, so subclasses may set it.
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
