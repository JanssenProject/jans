package io.jans.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Result — outcome over DomainError")
public class ResultTests {

    private static final class TestOwner {
    }

    private static final class TestError extends DomainError {

        private TestError(String message) {

            super(message);
        }
    }

    @Test
    @DisplayName("GIVEN a success WHEN inspected THEN it carries the value and getError throws")
    public void successCarriesValue() {

        Result<String> result = Result.success("ok");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.getValue()).isEqualTo("ok");
        assertThatThrownBy(result::getError).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("GIVEN a failure WHEN inspected THEN it carries the error and getValue throws")
    public void failureCarriesError() {

        DomainError error = new TestError("boom");

        Result<String> result = Result.failure(error);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isSameAs(error);
        assertThatThrownBy(result::getValue).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("GIVEN a kernel error WHEN carried by Result THEN it is exposed as a DomainError")
    public void carriesKernelError() {

        Result<String> result = Result.failure(RequiredValueMissing.forField(TestOwner.class, "name"));

        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
        assertThat(result.getError()).isInstanceOf(DomainError.class);
    }

    @Test
    @DisplayName("GIVEN a failure no caller has located WHEN inspected THEN its path is empty")
    public void failureStartsWithoutALocation() {

        Result<String> result = Result.failure(new TestError("boom"));

        assertThat(result.getPath()).isEqualTo(FieldPath.empty());
    }

    @Test
    @DisplayName("GIVEN a failure WHEN composing callers name their segments THEN the path grows outward")
    public void composingCallersAccumulateThePath() {

        Result<String> result = Result.<String>failure(new TestError("boom"))
            .at("validUntil")
            .at("metadataSource");

        assertThat(result.getPath().toString()).isEqualTo("metadataSource.validUntil");
    }

    @Test
    @DisplayName("GIVEN a failure inside a collection WHEN located by index THEN the element is addressable")
    public void locatesCollectionElements() {

        Result<String> result = Result.<String>failure(new TestError("boom"))
            .at("displayName")
            .at("attributes", 2);

        assertThat(result.getPath().toString()).isEqualTo("attributes[2].displayName");
    }

    @Test
    @DisplayName("GIVEN a success WHEN located THEN it is returned unchanged so the call composes inline")
    public void locatingASuccessIsANoOp() {

        Result<String> success = Result.success("ok");

        assertThat(success.at("displayName")).isSameAs(success);
        assertThatThrownBy(success::getPath).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("GIVEN a located failure WHEN propagated to the caller's type THEN error and path both survive")
    public void propagatePreservesErrorAndPath() {

        DomainError error = new TestError("boom");

        Result<Integer> propagated = Result.<String>failure(error).at("displayName").propagate();

        assertThat(propagated.isFailure()).isTrue();
        assertThat(propagated.getError()).isSameAs(error);
        assertThat(propagated.getPath().toString()).isEqualTo("displayName");
    }

    @Test
    @DisplayName("GIVEN a success WHEN propagated as a failure THEN the misuse is rejected")
    public void propagatingASuccessIsRejected() {

        assertThatThrownBy(() -> Result.success("ok").propagate())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("GIVEN a failure built with an explicit path WHEN inspected THEN it carries that path")
    public void failureAcceptsAnExplicitPath() {

        Result<String> result = Result.failure(new TestError("boom"), FieldPath.of("metadataSource", "uri"));

        assertThat(result.getPath().toString()).isEqualTo("metadataSource.uri");
    }
}
