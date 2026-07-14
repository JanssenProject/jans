package io.jans.shibboleth.trust.shared;

import io.jans.shibboleth.trust.activation.error.RequiredValueMissing;
import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Result — unified outcome over DomainError")
public class ResultTests {

    @Test
    @DisplayName("GIVEN a success WHEN inspected THEN it carries the value and no error")
    public void successCarriesValue() {

        Result<String> result = Result.success("ok");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.getValue()).isEqualTo("ok");
        assertThatThrownBy(result::getError).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("GIVEN a failure carrying a trust-context error WHEN inspected THEN getError exposes it as a DomainError")
    public void failureCarriesTrustError() {

        Result<String> result = Result.failure(CannotBeNullOrBlank.forField("displayName"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(CannotBeNullOrBlank.class);
        assertThatThrownBy(result::getValue).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("GIVEN the same Result type WHEN it carries an activation-context error THEN one Result serves both contexts")
    public void oneResultTypeServesBothContexts() {

        Result<String> result = Result.failure(RequiredValueMissing.forField("workerId"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
        assertThat((DomainError) result.getError()).isNotNull();
    }
}
