package io.jans.adapter.error;

import io.jans.kernel.DomainError;

/**
 * What one bounded context's errors mean to its clients.
 *
 * <p>{@link Violations} owns the mechanics of collecting a request's failures and {@link Problem}
 * the shape of the response; everything that varies by context lives behind this interface. Each
 * context supplies its own implementation because none of it is shareable: the error types differ,
 * the codes and statuses are that context's published contract, and the request-body field a domain
 * field corresponds to is knowable only there.
 *
 * <p>Implementations must be safe to share across threads — one instance typically serves a whole
 * module.
 */
public interface ErrorTranslation {

    /**
     * How {@code error} appears to a client. Returns {@link KernelErrorCodes#UNEXPECTED} for an
     * error the context has not registered.
     */
    ProblemTranslation translationFor(DomainError error);

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

    /**
     * The stable, machine-readable code clients branch on.
     */
    default String codeFor(DomainError error) {

        return translationFor(error).code();
    }

    /**
     * The short human title for this failure, as {@code Problem.title}.
     */
    default String titleFor(DomainError error) {

        return translationFor(error).title();
    }

    /**
     * The HTTP status this failure is reported with, as {@code Problem.status}.
     */
    default int statusFor(DomainError error) {

        return translationFor(error).status();
    }

    /**
     * Prose describing {@code error} as it applies to {@code field}. Built from the context's own
     * templates — never from {@link DomainError#getMessage()}, which names domain internals.
     */
    default String messageFor(DomainError error, String field) {

        return translationFor(error).message(field);
    }

    /**
     * {@code error} as a complete problem response, ready for the transport layer to add the
     * request URI via {@link Problem#withInstance(String)}.
     *
     * <p>A {@link RequestValidationFailed} is the boundary's own envelope rather than a context
     * error, so it is rendered here: it always reports as
     * {@link KernelErrorCodes#VALIDATION_FAILED} and carries its field-level violations.
     */
    default Problem problemFor(DomainError error) {

        if (error instanceof RequestValidationFailed) {

            return KernelErrorCodes.VALIDATION_FAILED.toProblem()
                .withViolations(((RequestValidationFailed) error).getViolations());
        }

        ProblemTranslation translation = translationFor(error);

        return translation.toProblem().withDetail(translation.message(fieldFor(error)));
    }
}
