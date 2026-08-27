package io.jans.shibboleth.trust.api.problem;

import io.jans.kernel.Result;

public final class Results {

    private Results() {
    }

    public static <T> T unwrap(Result<T> result) {

        if (result.isFailure()) {

            throw new DomainException(result.getError());
        }
        return result.getValue();
    }
}
