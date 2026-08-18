package io.jans.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Result — outcome over DomainError")
public class ResultTests {

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

        Result<String> result = Result.failure(RequiredValueMissing.forField("name"));

        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
        assertThat(result.getError()).isInstanceOf(DomainError.class);
    }
}
