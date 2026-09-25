package io.jans.shibboleth.trust.dto.error;

import io.jans.adapter.error.ErrorTranslation;
import io.jans.adapter.error.ProblemTranslation;
import io.jans.kernel.DomainError;

/**
 * What the trust context's errors mean to its clients.
 *
 * <p>Two independent questions, answered by the two tables this delegates to: {@link ErrorCodes}
 * says what kind of failure happened — its code, title, status and wording — and
 * {@link DtoFieldNames} says which request-body field it happened to. Keeping them apart matters
 * because they change for different reasons: a new error type touches the first, a renamed DTO
 * field the second.
 */
public final class TrustErrorTranslation implements ErrorTranslation {

    public static final TrustErrorTranslation INSTANCE = new TrustErrorTranslation();

    private TrustErrorTranslation() {
    }

    @Override
    public ProblemTranslation translationFor(DomainError error) {

        return ErrorCodes.translationFor(error);
    }

    @Override
    public boolean isFieldScoped(DomainError error) {

        return ErrorCodes.isFieldScoped(error);
    }

    @Override
    public String fieldFor(DomainError error) {

        return DtoFieldNames.resolve(error);
    }
}
