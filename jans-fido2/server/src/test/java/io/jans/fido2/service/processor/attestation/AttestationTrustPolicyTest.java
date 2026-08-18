/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.processor.attestation;

import io.jans.fido2.model.attestation.AttestationErrorResponseType;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.AttestationMode;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttestationTrustPolicyTest {

    @InjectMocks
    private AttestationTrustPolicy attestationTrustPolicy;

    @Mock
    private Logger log;
    @Mock
    private AppConfiguration appConfiguration;
    @Mock
    private Fido2Configuration fido2Configuration;
    @Mock
    private ErrorResponseFactory errorResponseFactory;

    private void withMode(String mode) {
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(mode);
    }

    private AuthData authData() {
        AuthData authData = new AuthData();
        authData.setAaguid(new byte[] {1, 2, 3, 4});
        return authData;
    }

    @Test
    void isEnforced_enforcedMode_true() {
        withMode(AttestationMode.ENFORCED.getValue());
        assertTrue(attestationTrustPolicy.isEnforced());
    }

    @Test
    void isEnforced_monitorMode_false() {
        withMode(AttestationMode.MONITOR.getValue());
        assertFalse(attestationTrustPolicy.isEnforced());
    }

    @Test
    void onTrustFailure_enforcedMode_throws() {
        withMode(AttestationMode.ENFORCED.getValue());
        when(errorResponseFactory.badRequestException(any(AttestationErrorResponseType.class), anyString()))
                .thenThrow(new WebApplicationException(Response.status(400).entity("rejected").build()));
        CredAndCounterData out = new CredAndCounterData();

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationTrustPolicy
                .onTrustFailure(AttestationErrorResponseType.PACKED_ERROR, "trust failure", authData(), out));
        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void onTrustFailure_monitorMode_toleratesAndMarksUntrusted() {
        withMode(AttestationMode.MONITOR.getValue());
        CredAndCounterData out = new CredAndCounterData();
        assertTrue(out.isAttestationTrusted()); // default

        assertDoesNotThrow(() -> attestationTrustPolicy
                .onTrustFailure(AttestationErrorResponseType.PACKED_ERROR, "trust failure", authData(), out));
        assertFalse(out.isAttestationTrusted());
    }
}
