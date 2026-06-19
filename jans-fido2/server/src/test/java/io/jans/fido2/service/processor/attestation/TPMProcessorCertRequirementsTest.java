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

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TPMProcessorCertRequirementsTest {

    private static final String OID_AIK_CERTIFICATE = "2.23.133.8.3";

    @InjectMocks
    private TPMProcessor tpmProcessor;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    private X509Certificate compliantAikCertificate() throws Exception {
        X509Certificate aikCertificate = mock(X509Certificate.class);
        when(aikCertificate.getVersion()).thenReturn(3);
        when(aikCertificate.getSubjectX500Principal()).thenReturn(new X500Principal(""));
        when(aikCertificate.getBasicConstraints()).thenReturn(-1);
        when(aikCertificate.getExtendedKeyUsage()).thenReturn(Collections.singletonList(OID_AIK_CERTIFICATE));
        doReturn(Collections.singletonList(Collections.singletonList("tpm.example")))
                .when(aikCertificate).getSubjectAlternativeNames();
        return aikCertificate;
    }

    @Test
    void verifyTpmAttestationCertRequirements_compliantCertificate_passes() throws Exception {
        X509Certificate aikCertificate = compliantAikCertificate();
        assertDoesNotThrow(() -> tpmProcessor.verifyTpmAttestationCertRequirements(aikCertificate));
    }

    @Test
    void verifyTpmAttestationCertRequirements_versionNotThree_rejected() {
        X509Certificate aikCertificate = mock(X509Certificate.class);
        when(aikCertificate.getVersion()).thenReturn(1);
        stubBadRequest();

        WebApplicationException res = assertThrows(WebApplicationException.class,
                () -> tpmProcessor.verifyTpmAttestationCertRequirements(aikCertificate));
        assertResponse(res);
    }

    @Test
    void verifyTpmAttestationCertRequirements_subjectNotEmpty_rejected() {
        X509Certificate aikCertificate = mock(X509Certificate.class);
        when(aikCertificate.getVersion()).thenReturn(3);
        when(aikCertificate.getSubjectX500Principal()).thenReturn(new X500Principal("CN=should-be-empty"));
        stubBadRequest();

        WebApplicationException res = assertThrows(WebApplicationException.class,
                () -> tpmProcessor.verifyTpmAttestationCertRequirements(aikCertificate));
        assertResponse(res);
    }

    @Test
    void verifyTpmAttestationCertRequirements_caCertificate_rejected() {
        X509Certificate aikCertificate = mock(X509Certificate.class);
        when(aikCertificate.getVersion()).thenReturn(3);
        when(aikCertificate.getSubjectX500Principal()).thenReturn(new X500Principal(""));
        when(aikCertificate.getBasicConstraints()).thenReturn(0);
        stubBadRequest();

        WebApplicationException res = assertThrows(WebApplicationException.class,
                () -> tpmProcessor.verifyTpmAttestationCertRequirements(aikCertificate));
        assertResponse(res);
    }

    @Test
    void verifyTpmAttestationCertRequirements_missingAikExtendedKeyUsage_rejected() throws Exception {
        X509Certificate aikCertificate = mock(X509Certificate.class);
        when(aikCertificate.getVersion()).thenReturn(3);
        when(aikCertificate.getSubjectX500Principal()).thenReturn(new X500Principal(""));
        when(aikCertificate.getBasicConstraints()).thenReturn(-1);
        when(aikCertificate.getExtendedKeyUsage()).thenReturn(Collections.singletonList("1.3.6.1.5.5.7.3.1"));
        stubBadRequest();

        WebApplicationException res = assertThrows(WebApplicationException.class,
                () -> tpmProcessor.verifyTpmAttestationCertRequirements(aikCertificate));
        assertResponse(res);
    }

    @Test
    void verifyTpmAttestationCertRequirements_missingSubjectAlternativeName_rejected() throws Exception {
        X509Certificate aikCertificate = mock(X509Certificate.class);
        when(aikCertificate.getVersion()).thenReturn(3);
        when(aikCertificate.getSubjectX500Principal()).thenReturn(new X500Principal(""));
        when(aikCertificate.getBasicConstraints()).thenReturn(-1);
        when(aikCertificate.getExtendedKeyUsage()).thenReturn(Collections.singletonList(OID_AIK_CERTIFICATE));
        doReturn(null).when(aikCertificate).getSubjectAlternativeNames();
        stubBadRequest();

        WebApplicationException res = assertThrows(WebApplicationException.class,
                () -> tpmProcessor.verifyTpmAttestationCertRequirements(aikCertificate));
        assertResponse(res);
    }

    private void stubBadRequest() {
        when(errorResponseFactory.badRequestException(any(AttestationErrorResponseType.class), anyString()))
                .thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));
    }

    private void assertResponse(WebApplicationException res) {
        org.junit.jupiter.api.Assertions.assertNotNull(res);
        org.junit.jupiter.api.Assertions.assertEquals(400, res.getResponse().getStatus());
    }
}
