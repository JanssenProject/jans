package io.jans.shibboleth.trust.shared;

import io.jans.shibboleth.trust.activation.error.ActivationError;
import io.jans.shibboleth.trust.activation.error.StaleReport;
import io.jans.shibboleth.trust.config.error.InvalidUriSyntax;
import io.jans.shibboleth.trust.config.error.TrustError;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DomainError — shared root error type")
public class DomainErrorTests {

    @Test
    @DisplayName("GIVEN a TrustError WHEN its type is inspected THEN it is a DomainError")
    public void trustErrorIsDomainError() {

        TrustError error = InvalidUriSyntax.forValue("not a uri");
        assertThat(error).isInstanceOf(DomainError.class);
    }

    @Test
    @DisplayName("GIVEN an ActivationError WHEN its type is inspected THEN it is a DomainError")
    public void activationErrorIsDomainError() {

        ActivationError error = StaleReport.instance();
        assertThat(error).isInstanceOf(DomainError.class);
    }

    @Test
    @DisplayName("GIVEN a shared kernel error WHEN its type is inspected THEN it is a DomainError belonging to neither context")
    public void sharedErrorIsDomainError() {

        RequiredValueMissing error = RequiredValueMissing.forField("displayName");

        assertThat(error).isInstanceOf(DomainError.class);
        assertThat(error).isNotInstanceOf(TrustError.class);
        assertThat(error).isNotInstanceOf(ActivationError.class);
    }

    @Test
    @DisplayName("GIVEN errors from both contexts WHEN referenced as DomainError THEN the message contract is exposed")
    public void exposesMessageContractAcrossContexts() {

        DomainError trust = InvalidUriSyntax.forValue("not a uri");
        DomainError activation = StaleReport.instance();

        assertThat(trust.getMessage()).isNotBlank();
        assertThat(trust.toString()).isEqualTo(trust.getMessage());
        assertThat(activation.getMessage()).isNotBlank();
        assertThat(activation.toString()).isEqualTo(activation.getMessage());
    }
}
