package io.jans.shibboleth.trust.shared;

import io.jans.shibboleth.trust.activation.error.StaleReport;
import io.jans.shibboleth.trust.config.error.InvalidUriSyntax;

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

        Result<String> result = Result.failure(InvalidUriSyntax.forValue("not a uri"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(InvalidUriSyntax.class);
        assertThatThrownBy(result::getValue).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("GIVEN the same Result type WHEN it carries an activation-context error THEN one Result serves both contexts")
    public void oneResultTypeServesBothContexts() {

        Result<String> result = Result.failure(StaleReport.instance());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(StaleReport.class);
        assertThat(result.getError()).isInstanceOf(DomainError.class);
    }
}
