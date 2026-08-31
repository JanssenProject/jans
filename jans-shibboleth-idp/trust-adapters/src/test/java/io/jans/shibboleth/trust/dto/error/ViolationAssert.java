package io.jans.shibboleth.trust.dto.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.jans.adapter.error.RequestValidationFailed;
import io.jans.adapter.error.Violation;
import io.jans.kernel.Result;

import java.util.List;

/**
 * Assertions over the violations a mapper reports, so tests state the contract a client actually
 * sees — the request field and the stable code — rather than which domain error happened to be
 * raised behind the boundary.
 */
public final class ViolationAssert {

    private ViolationAssert() {
    }

    public static List<Violation> violationsOf(Result<?> result) {

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequestValidationFailed.class);

        return ((RequestValidationFailed) result.getError()).getViolations();
    }

    /**
     * Asserts the result reports exactly one violation, against {@code field} with {@code code}.
     */
    public static void assertOnlyViolation(Result<?> result, String field, String code) {

        assertThat(violationsOf(result))
            .extracting(Violation::getField, Violation::getCode)
            .containsExactly(tuple(field, code));
    }

    /**
     * Asserts the result reports a violation against {@code field} with {@code code}, among others.
     */
    public static void assertViolation(Result<?> result, String field, String code) {

        assertThat(violationsOf(result))
            .extracting(Violation::getField, Violation::getCode)
            .contains(tuple(field, code));
    }
}
