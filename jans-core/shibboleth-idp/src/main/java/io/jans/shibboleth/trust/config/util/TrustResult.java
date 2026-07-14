package io.jans.shibboleth.trust.config.util;

import io.jans.common.Result;
import io.jans.shibboleth.trust.config.error.TrustError;

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