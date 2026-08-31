package io.jans.shibboleth.trust.dto.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.jans.adapter.error.KernelErrorCodes;
import io.jans.adapter.error.RequestValidationFailed;
import io.jans.adapter.error.Violation;
import io.jans.adapter.error.Violations;
import io.jans.kernel.FieldPath;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;
import io.jans.shibboleth.trust.config.ReleasedAttribute;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.config.error.InvalidUriSyntax;
import io.jans.shibboleth.trust.config.error.OperationForbiddenFromStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The trust context's half of the boundary: which of its errors are about a field, what they are
 * called, and which request field each names. The collecting mechanics are covered in
 * adapter-kernel; these tests pin the trust-specific answers.
 */
@DisplayName("TrustErrorTranslation — what trust errors mean to clients")
public class TrustErrorTranslationTests {

    private static Violations collector() {

        return Violations.create(TrustErrorTranslation.INSTANCE);
    }

    @Test
    @DisplayName("GIVEN a trust syntax error WHEN translated THEN it is field-scoped with its own code")
    public void syntaxErrorsAreFieldScoped() {

        InvalidUriSyntax error = InvalidUriSyntax.forValue("nope");

        assertThat(TrustErrorTranslation.INSTANCE.isFieldScoped(error)).isTrue();
        assertThat(TrustErrorTranslation.INSTANCE.codeFor(error)).isEqualTo("invalid_uri_syntax");
        assertThat(TrustErrorTranslation.INSTANCE.messageFor(error, "uri")).contains("'uri'");
    }

    @Test
    @DisplayName("GIVEN a forbidden operation WHEN translated THEN it describes the request, not a field")
    public void statusConflictsDescribeTheRequest() {

        OperationForbiddenFromStatus error = OperationForbiddenFromStatus.of("activate", TrustStatus.ACTIVE);

        assertThat(TrustErrorTranslation.INSTANCE.isFieldScoped(error)).isFalse();
        assertThat(TrustErrorTranslation.INSTANCE.codeFor(error)).isEqualTo("operation_forbidden_from_status");
    }

    @Test
    @DisplayName("GIVEN an unregistered error WHEN translated THEN it reports the shared unexpected code")
    public void unregisteredErrorsFallBackToTheSharedCode() {

        assertThat(TrustErrorTranslation.INSTANCE.codeFor(RequestValidationFailed.with(
            java.util.List.of(new Violation("uri", "invalid_uri_syntax", "bad")))))
            .isEqualTo(KernelErrorCodes.UNEXPECTED);
    }

    @Test
    @DisplayName("GIVEN a domain field WHEN it fails inside a collection THEN the DTO name nests by index")
    public void domainFieldNamesAreTranslatedAndNested() {

        Violations violations = collector();

        violations.record(
            RequiredValueMissing.forField(ReleasedAttribute.class, "displayName"),
            FieldPath.empty().prepend("attributes", 3));

        assertThat(violations.asList())
            .extracting(Violation::getField)
            .containsExactly("attributes[3].display_name");
    }

    @Test
    @DisplayName("GIVEN an aggregate invariant WHEN completed THEN it becomes a violation on its request field")
    public void aggregateInvariantsBecomeFieldViolations() {

        Result<String> completed = collector().completeWith(
            Result.failure(RequiredValueMissing.forField(TrustRelationship.class, "nature")));

        assertThat(((RequestValidationFailed) completed.getError()).getViolations())
            .extracting(Violation::getField, Violation::getCode)
            .containsExactly(tuple("nature", KernelErrorCodes.REQUIRED_VALUE_MISSING));
    }
}
