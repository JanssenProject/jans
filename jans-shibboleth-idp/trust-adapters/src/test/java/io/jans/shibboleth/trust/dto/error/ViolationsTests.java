package io.jans.shibboleth.trust.dto.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import io.jans.kernel.FieldPath;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;
import io.jans.shibboleth.trust.config.error.InvalidUriSyntax;
import io.jans.shibboleth.trust.config.error.OperationForbiddenFromStatus;
import io.jans.shibboleth.trust.config.TrustStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Violations — collecting a request's field-level failures")
public class ViolationsTests {

    @Test
    @DisplayName("GIVEN a successful value WHEN taken THEN it is returned and nothing is recorded")
    public void takingASuccessRecordsNothing() {

        Violations violations = Violations.create();

        String value = violations.take(Result.success("ok"), "display_name");

        assertThat(value).isEqualTo("ok");
        assertThat(violations.isEmpty()).isTrue();
        assertThat(violations.any()).isFalse();
    }

    @Test
    @DisplayName("GIVEN a failed value WHEN taken THEN null is returned and the field is recorded")
    public void takingAFailureRecordsTheField() {

        Violations violations = Violations.create();

        String value = violations.take(
            Result.<String>failure(RequiredValueMissing.of(TrustStatus.class)), "display_name");

        assertThat(value).isNull();
        assertThat(violations.any()).isTrue();
        assertThat(violations.asList())
            .extracting(Violation::getField, Violation::getCode, Violation::getMessage)
            .containsExactly(tuple("display_name", "required_value_missing", "'display_name' is required"));
    }

    @Test
    @DisplayName("GIVEN several failures WHEN collected THEN all are reported in one failure")
    public void everyFailureIsReportedTogether() {

        Violations violations = Violations.create();

        violations.take(Result.failure(RequiredValueMissing.of(TrustStatus.class)), "display_name");
        violations.take(Result.failure(InvalidUriSyntax.forValue("nope")), "uri");

        Result<String> failure = violations.asFailure();

        assertThat(failure.getError()).isInstanceOf(RequestValidationFailed.class);
        assertThat(((RequestValidationFailed) failure.getError()).getViolations())
            .extracting(Violation::getField)
            .containsExactly("display_name", "uri");
        assertThat(failure.getError().getMessage()).contains("2 invalid fields");
    }

    @Test
    @DisplayName("GIVEN a nested location WHEN taken under an outer field THEN the path nests")
    public void nestedLocationsNestUnderTheOuterField() {

        Violations violations = Violations.create();

        violations.take(
            Result.failure(InvalidUriSyntax.forValue("nope"), FieldPath.of("location")),
            "assertion_consumer_service");

        assertThat(violations.asList())
            .extracting(Violation::getField)
            .containsExactly("assertion_consumer_service.location");
    }

    @Test
    @DisplayName("GIVEN an error naming a domain field WHEN recorded under a location THEN the DTO name nests inside")
    public void domainFieldNamesAreTranslatedAndNested() {

        Violations violations = Violations.create();

        violations.record(
            RequiredValueMissing.forField(
                io.jans.shibboleth.trust.config.ReleasedAttribute.class, "displayName"),
            FieldPath.empty().prepend("attributes", 3));

        assertThat(violations.asList())
            .extracting(Violation::getField)
            .containsExactly("attributes[3].display_name");
    }

    @Test
    @DisplayName("GIVEN a field-scoped domain failure WHEN completed THEN it becomes a violation")
    public void completeWithTranslatesFieldScopedFailures() {

        Violations violations = Violations.create();

        Result<String> completed = violations.completeWith(
            Result.failure(RequiredValueMissing.forField(
                io.jans.shibboleth.trust.config.TrustRelationship.class, "nature")));

        assertThat(completed.getError()).isInstanceOf(RequestValidationFailed.class);
        assertThat(((RequestValidationFailed) completed.getError()).getViolations())
            .extracting(Violation::getField, Violation::getCode)
            .containsExactly(tuple("nature", "required_value_missing"));
    }

    @Test
    @DisplayName("GIVEN a failure about the whole request WHEN completed THEN it passes through untouched")
    public void completeWithPassesRequestScopedFailuresThrough() {

        Violations violations = Violations.create();

        OperationForbiddenFromStatus error =
            OperationForbiddenFromStatus.of("activate", TrustStatus.ACTIVE);

        Result<String> completed = violations.completeWith(Result.failure(error));

        assertThat(completed.getError()).isSameAs(error);
        assertThat(violations.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("GIVEN a success WHEN completed THEN it is returned unchanged")
    public void completeWithPassesSuccessThrough() {

        Violations violations = Violations.create();

        assertThat(violations.completeWith(Result.success("ok")).getValue()).isEqualTo("ok");
    }

    @Test
    @DisplayName("GIVEN no violations WHEN a failure is demanded THEN the misuse is rejected")
    public void refusesToReportAFailureWithNoViolations() {

        assertThatThrownBy(() -> Violations.create().asFailure())
            .isInstanceOf(IllegalArgumentException.class);
    }
}
