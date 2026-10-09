/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.processor.attestation;

import io.jans.fido2.model.attestation.AttestationErrorResponseType;
import io.jans.fido2.model.error.ErrorResponseFactory;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECField;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class U2FAttestationProcessorCertKeyTest {

    @InjectMocks
    private U2FAttestationProcessor u2FAttestationProcessor;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    private X509Certificate certificateWithEcKey(int fieldSize) {
        ECField field = mock(ECField.class);
        when(field.getFieldSize()).thenReturn(fieldSize);
        EllipticCurve curve = mock(EllipticCurve.class);
        when(curve.getField()).thenReturn(field);
        ECParameterSpec params = mock(ECParameterSpec.class);
        when(params.getCurve()).thenReturn(curve);
        ECPublicKey ecPublicKey = mock(ECPublicKey.class);
        when(ecPublicKey.getParams()).thenReturn(params);
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getPublicKey()).thenReturn(ecPublicKey);
        return certificate;
    }

    @Test
    void verifyU2fAttestationCertKey_ecP256Key_passes() {
        X509Certificate certificate = certificateWithEcKey(256);
        assertDoesNotThrow(() -> u2FAttestationProcessor.verifyU2fAttestationCertKey(certificate));
    }

    @Test
    void verifyU2fAttestationCertKey_nonEcKey_rejected() {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getPublicKey()).thenReturn(mock(PublicKey.class));
        stubBadRequest();

        WebApplicationException res = assertThrows(WebApplicationException.class,
                () -> u2FAttestationProcessor.verifyU2fAttestationCertKey(certificate));
        assertResponse(res);
    }

    @Test
    void verifyU2fAttestationCertKey_ecKeyNotP256_rejected() {
        X509Certificate certificate = certificateWithEcKey(384);
        stubBadRequest();

        WebApplicationException res = assertThrows(WebApplicationException.class,
                () -> u2FAttestationProcessor.verifyU2fAttestationCertKey(certificate));
        assertResponse(res);
    }

    private void stubBadRequest() {
        when(errorResponseFactory.badRequestException(any(AttestationErrorResponseType.class), anyString()))
                .thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));
    }

    private void assertResponse(WebApplicationException res) {
        assertNotNull(res);
        assertEquals(400, res.getResponse().getStatus());
    }
}
