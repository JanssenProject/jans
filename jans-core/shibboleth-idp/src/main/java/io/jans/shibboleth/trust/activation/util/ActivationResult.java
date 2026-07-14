package io.jans.shibboleth.trust.activation.util;

import io.jans.common.Result;
import io.jans.shibboleth.trust.activation.error.ActivationError;

public class ActivationResult<T> extends Result<T> {

    private ActivationResult(T value, ActivationError error, boolean isSuccess) {

        super(value, error, isSuccess);
    }

    public static <T> ActivationResult<T> success(T value) {

        return new ActivationResult<>(value, null, true);
    }

    public static <T> ActivationResult<T> failure(ActivationError error) {

        return new ActivationResult<>(null, error, false);
    }

    @Override
    public ActivationError getError() {

        return (ActivationError) super.getError();
    }
}
