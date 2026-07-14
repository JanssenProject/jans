package io.jans.shibboleth.trust.shared;

import io.jans.shibboleth.trust.activation.error.ActivationError;
import io.jans.shibboleth.trust.activation.error.RequiredValueMissing;
import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.config.error.TrustError;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DomainError — shared root error type")
public class DomainErrorTests {

    @Test
    @DisplayName("GIVEN a TrustError WHEN its type is inspected THEN it is a DomainError")
    public void trustErrorIsDomainError() {

        TrustError error = CannotBeNullOrBlank.forField("displayName");
        assertThat(error).isInstanceOf(DomainError.class);
    }

    @Test
    @DisplayName("GIVEN an ActivationError WHEN its type is inspected THEN it is a DomainError")
    public void activationErrorIsDomainError() {

        ActivationError error = RequiredValueMissing.forField("workerId");
        assertThat(error).isInstanceOf(DomainError.class);
    }

    @Test
    @DisplayName("GIVEN errors from both contexts WHEN referenced as DomainError THEN the message contract is exposed")
    public void exposesMessageContractAcrossContexts() {

        DomainError trust = CannotBeNullOrBlank.forField("displayName");
        DomainError activation = RequiredValueMissing.forField("workerId");

        assertThat(trust.getMessage()).isNotBlank();
        assertThat(trust.toString()).isEqualTo(trust.getMessage());
        assertThat(activation.getMessage()).isNotBlank();
        assertThat(activation.toString()).isEqualTo(activation.getMessage());
    }
}
