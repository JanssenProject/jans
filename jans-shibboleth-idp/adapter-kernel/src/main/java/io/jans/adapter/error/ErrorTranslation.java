package io.jans.adapter.error;

import io.jans.kernel.DomainError;

/**
 * What one bounded context's errors mean to its clients.
 *
 * <p>{@link Violations} owns the mechanics of collecting a request's failures; everything that
 * varies by context lives behind this interface. Each context supplies its own implementation
 * because none of it is shareable: the error types differ, the codes are that context's published
 * contract, and the request-body field a domain field corresponds to is knowable only there.
 *
 * <p>Implementations must be safe to share across threads — one instance typically serves a whole
 * module.
 */
public interface ErrorTranslation {

    /**
     * The stable, machine-readable code clients branch on. Returns
     * {@link KernelErrorCodes#UNEXPECTED} for an error the context has not registered.
     */
    String codeFor(DomainError error);

    /**
     * Prose describing {@code error} as it applies to {@code field}. Built from the context's own
     * templates — never from {@link DomainError#getMessage()}, which names domain internals.
     */
    String messageFor(DomainError error, String field);

    /**
     * Whether {@code error} describes one field of the request body, and so can be reported as a
     * {@link Violation} against that field. Errors describing the request as a whole are handed
     * back to the caller untouched.
     */
    boolean isFieldScoped(DomainError error);

    /**
     * The request-body field {@code error} itself names, or {@link #NO_FIELD} when it names none.
     * This is where a domain field becomes a name the client recognises.
     */
    String fieldFor(DomainError error);

    /**
     * Returned by {@link #fieldFor(DomainError)} when an error names no request-body field. Callers
     * report against the request as a whole rather than inventing a name.
     */
    String NO_FIELD = "";
}
