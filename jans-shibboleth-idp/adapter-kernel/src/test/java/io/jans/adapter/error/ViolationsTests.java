package io.jans.adapter.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import io.jans.kernel.DomainError;
import io.jans.kernel.FieldPath;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Violations — collecting a request's field-level failures")
public class ViolationsTests {

    private static final class Owner {
    }

    /** A failure about one field of the request body. */
    private static final class FieldProblem extends DomainError {

        private FieldProblem() {

            super("a field is wrong");
        }
    }

    /** A failure about the request as a whole. */
    private static final class RequestProblem extends DomainError {

        private RequestProblem() {

            super("the request is not allowed");
        }
    }

    /**
     * Stands in for a bounded context's translation. Deliberately does not know the trust or staging
     * contexts — {@link Violations} must work knowing nothing about either.
     */
    private static final ErrorTranslation TRANSLATION = new ErrorTranslation() {

        @Override
        public String codeFor(DomainError error) {

            if (error instanceof RequiredValueMissing) {

                return KernelErrorCodes.REQUIRED_VALUE_MISSING;
            }
            if (error instanceof FieldProblem) {

                return "field_problem";
            }
            if (error instanceof RequestProblem) {

                return "request_problem";
            }
            return KernelErrorCodes.UNEXPECTED;
        }

        @Override
        public String messageFor(DomainError error, String field) {

            return String.format("%s: %s", codeFor(error), field);
        }

        @Override
        public boolean isFieldScoped(DomainError error) {

            return error instanceof RequiredValueMissing || error instanceof FieldProblem;
        }

        @Override
        public String fieldFor(DomainError error) {

            if (error instanceof RequiredValueMissing) {

                RequiredValueMissing missing = (RequiredValueMissing) error;
                return missing.namesField() ? "mapped_" + missing.getFieldName() : "mapped_owner";
            }
            return NO_FIELD;
        }
    };

    private static Violations collector() {

        return Violations.create(TRANSLATION);
    }

    @Test
    @DisplayName("GIVEN a translation WHEN none is supplied THEN the misuse is rejected")
    public void requiresATranslation() {

        assertThatThrownBy(() -> Violations.create(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("GIVEN a successful value WHEN taken THEN it is returned and nothing is recorded")
    public void takingASuccessRecordsNothing() {

        Violations violations = collector();

        String value = violations.take(Result.success("ok"), "display_name");

        assertThat(value).isEqualTo("ok");
        assertThat(violations.isEmpty()).isTrue();
        assertThat(violations.any()).isFalse();
    }

    @Test
    @DisplayName("GIVEN a failed value WHEN taken THEN null is returned and the field is recorded")
    public void takingAFailureRecordsTheField() {

        Violations violations = collector();

        String value = violations.take(Result.<String>failure(new FieldProblem()), "display_name");

        assertThat(value).isNull();
        assertThat(violations.any()).isTrue();
        assertThat(violations.asList())
            .extracting(Violation::field, Violation::code, Violation::message)
            .containsExactly(tuple("display_name", "field_problem", "field_problem: display_name"));
    }

    @Test
    @DisplayName("GIVEN several failures WHEN collected THEN all are reported in one failure")
    public void everyFailureIsReportedTogether() {

        Violations violations = collector();

        violations.take(Result.failure(new FieldProblem()), "display_name");
        violations.take(Result.failure(new FieldProblem()), "uri");

        Result<String> failure = violations.asFailure();

        assertThat(failure.getError()).isInstanceOf(RequestValidationFailed.class);
        assertThat(((RequestValidationFailed) failure.getError()).getViolations())
            .extracting(Violation::field)
            .containsExactly("display_name", "uri");
        assertThat(failure.getError().getMessage()).contains("2 invalid fields");
    }

    @Test
    @DisplayName("GIVEN one failure WHEN reported THEN the count reads as singular")
    public void oneFailureReadsAsSingular() {

        Violations violations = collector();
        violations.take(Result.failure(new FieldProblem()), "uri");

        assertThat(violations.asFailure().getError().getMessage()).contains("1 invalid field");
    }

    @Test
    @DisplayName("GIVEN a nested location WHEN taken under an outer field THEN the path nests")
    public void nestedLocationsNestUnderTheOuterField() {

        Violations violations = collector();

        violations.take(
            Result.failure(new FieldProblem(), FieldPath.of("location")), "assertion_consumer_service");

        assertThat(violations.asList())
            .extracting(Violation::field)
            .containsExactly("assertion_consumer_service.location");
    }

    @Test
    @DisplayName("GIVEN an error naming a field WHEN recorded under a location THEN the name nests inside")
    public void aNamedFieldNestsInsideTheSuppliedLocation() {

        Violations violations = collector();

        violations.record(
            RequiredValueMissing.forField(Owner.class, "displayName"),
            FieldPath.empty().prepend("attributes", 3));

        assertThat(violations.asList())
            .extracting(Violation::field)
            .containsExactly("attributes[3].mapped_displayName");
    }

    @Test
    @DisplayName("GIVEN a location that already names the field WHEN recorded THEN it is not repeated")
    public void anAgreedFieldIsNotRepeated() {

        Violations violations = collector();

        violations.take(Result.failure(RequiredValueMissing.of(Owner.class)), "mapped_owner");

        assertThat(violations.asList())
            .extracting(Violation::field)
            .containsExactly("mapped_owner");
    }

    @Test
    @DisplayName("GIVEN no location at all WHEN recorded THEN the field the error names is used")
    public void anUnlocatedFailureUsesTheNameTheErrorCarries() {

        Violations violations = collector();

        violations.record(RequiredValueMissing.of(Owner.class), FieldPath.empty());

        assertThat(violations.asList()).extracting(Violation::field).containsExactly("mapped_owner");
    }

    @Test
    @DisplayName("GIVEN a field-scoped failure WHEN completed THEN it becomes a violation")
    public void completeWithTranslatesFieldScopedFailures() {

        Violations violations = collector();

        Result<String> completed =
            violations.completeWith(Result.failure(RequiredValueMissing.forField(Owner.class, "nature")));

        assertThat(completed.getError()).isInstanceOf(RequestValidationFailed.class);
        assertThat(((RequestValidationFailed) completed.getError()).getViolations())
            .extracting(Violation::field, Violation::code)
            .containsExactly(tuple("mapped_nature", KernelErrorCodes.REQUIRED_VALUE_MISSING));
    }

    @Test
    @DisplayName("GIVEN a failure about the whole request WHEN completed THEN it passes through untouched")
    public void completeWithPassesRequestScopedFailuresThrough() {

        Violations violations = collector();
        DomainError error = new RequestProblem();

        Result<String> completed = violations.completeWith(Result.failure(error));

        assertThat(completed.getError()).isSameAs(error);
        assertThat(violations.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("GIVEN a request-scoped failure WHEN rejected THEN its path survives the re-typing")
    public void rejectedPreservesThePathOfAPassedThroughFailure() {

        Result<Integer> rejected = collector()
            .rejected(Result.<String>failure(new RequestProblem(), FieldPath.of("metadata_source")));

        assertThat(rejected.getError()).isInstanceOf(RequestProblem.class);
        assertThat(rejected.getPath().toString()).isEqualTo("metadata_source");
    }

    @Test
    @DisplayName("GIVEN a success WHEN completed THEN it is returned unchanged")
    public void completeWithPassesSuccessThrough() {

        assertThat(collector().completeWith(Result.success("ok")).getValue()).isEqualTo("ok");
    }

    @Test
    @DisplayName("GIVEN no violations WHEN a failure is demanded THEN the misuse is rejected")
    public void refusesToReportAFailureWithNoViolations() {

        assertThatThrownBy(() -> collector().asFailure()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("GIVEN collected violations WHEN the list is read THEN it cannot be modified")
    public void theReportedListIsNotModifiable() {

        Violations violations = collector();
        violations.take(Result.failure(new FieldProblem()), "uri");

        assertThatThrownBy(() -> violations.asList().add(new Violation("x", "y", "z")))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
