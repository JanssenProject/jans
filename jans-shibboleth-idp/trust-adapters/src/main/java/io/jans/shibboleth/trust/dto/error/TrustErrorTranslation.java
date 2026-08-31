package io.jans.shibboleth.trust.dto.error;

import io.jans.adapter.error.ErrorTranslation;
import io.jans.kernel.DomainError;

/**
 * What the trust context's errors mean to its clients.
 *
 * <p>Two independent questions, answered by the two tables this delegates to: {@link ErrorCodes}
 * says what kind of failure happened and how to word it, {@link DtoFieldNames} says which
 * request-body field it happened to. Keeping them apart matters because they change for different
 * reasons — a new error type touches the first, a renamed DTO field the second.
 */
public final class TrustErrorTranslation implements ErrorTranslation {

    public static final TrustErrorTranslation INSTANCE = new TrustErrorTranslation();

    private TrustErrorTranslation() {
    }

    @Override
    public String codeFor(DomainError error) {

        return ErrorCodes.codeFor(error);
    }

    @Override
    public String messageFor(DomainError error, String field) {

        return ErrorCodes.messageFor(error, field);
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
