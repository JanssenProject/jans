package io.jans.shibboleth.model.util;

import io.jans.common.Result;
import io.jans.shibboleth.model.error.TrustError;

public class TrustResult<T> extends Result<T> {

    private TrustResult(T value, TrustError error, boolean isSuccess) {

        super(value,error,isSuccess);
    }

    public static <T> TrustResult<T> success(T value) {

        return new TrustResult<>(value,null,true);
    }

    public static <T> TrustResult<T> failure(TrustError error) {

        return new TrustResult<>(null,error,false);
    }

    @Override
    public TrustError getError() {

        Object error = super.getError();
        return (TrustError) error;
    }
}