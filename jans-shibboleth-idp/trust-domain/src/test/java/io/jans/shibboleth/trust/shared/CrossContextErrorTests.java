package io.jans.shibboleth.trust.shared;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.kernel.DomainError;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

import io.jans.shibboleth.trust.activation.error.ActivationError;
import io.jans.shibboleth.trust.activation.error.StaleReport;
import io.jans.shibboleth.trust.config.error.InvalidUriSyntax;
import io.jans.shibboleth.trust.config.error.TrustError;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the two trust bounded-context error families interoperate through the shared-kernel
 * types: both are {@link DomainError}s, so a single {@link Result} can carry either.
 */
@DisplayName("Cross-context errors — trust and activation errors flow through the shared kernel")
public class CrossContextErrorTests {

    @Test
    @DisplayName("GIVEN a TrustError and an ActivationError WHEN inspected THEN both are DomainErrors")
    public void bothFamiliesAreDomainErrors() {

        TrustError trust = InvalidUriSyntax.forValue("not a uri");
        ActivationError activation = StaleReport.instance();

        assertThat(trust).isInstanceOf(DomainError.class);
        assertThat(activation).isInstanceOf(DomainError.class);
    }

    @Test
    @DisplayName("GIVEN errors from both contexts WHEN carried by Result THEN one Result type serves both")
    public void oneResultServesBothContexts() {

        Result<String> trustFailure = Result.failure(InvalidUriSyntax.forValue("not a uri"));
        Result<String> activationFailure = Result.failure(StaleReport.instance());

        assertThat(trustFailure.getError()).isInstanceOf(InvalidUriSyntax.class);
        assertThat(activationFailure.getError()).isInstanceOf(StaleReport.class);
    }

    @Test
    @DisplayName("GIVEN a kernel error WHEN inspected THEN it is a DomainError belonging to neither trust context")
    public void kernelErrorBelongsToNeitherContext() {

        RequiredValueMissing error = RequiredValueMissing.forField("displayName");

        assertThat(error).isInstanceOf(DomainError.class);
        assertThat(error).isNotInstanceOf(TrustError.class);
        assertThat(error).isNotInstanceOf(ActivationError.class);
    }
}
