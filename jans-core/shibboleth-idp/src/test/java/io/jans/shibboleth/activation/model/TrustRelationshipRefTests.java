package io.jans.shibboleth.activation.model;

import io.jans.shibboleth.activation.error.RequiredValueMissing;
import io.jans.shibboleth.activation.util.ActivationResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Group 4 — TrustRelationshipRef")
public class TrustRelationshipRefTests {

    @Test
    @DisplayName("GIVEN a UUID WHEN a TrustRelationshipRef is built THEN it exposes that value")
    public void shouldExposeValue_whenBuiltFromUuid() {

        UUID value = UUID.randomUUID();

        TrustRelationshipRef ref = TrustRelationshipRef.of(value).getValue();

        assertThat(ref.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("GIVEN a null value WHEN a TrustRelationshipRef is built THEN it fails and no reference is produced")
    public void shouldFail_whenBuiltFromNullValue() {

        ActivationResult<TrustRelationshipRef> result = TrustRelationshipRef.of(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }
}
