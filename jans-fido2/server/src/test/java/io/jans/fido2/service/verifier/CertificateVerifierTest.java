package io.jans.fido2.service.verifier;

import io.jans.fido2.exception.Fido2MissingAttestationCertException;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.security.*;
import java.security.cert.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateVerifierTest {

    @InjectMocks
    private CertificateVerifier certificateVerifier;

    @Mock
    private Logger log;

    @Mock
    private Base64Service base64Service;

    @Mock
    private CertificateService certificateService;

    @Test
    void checkForTrustedCertsInAttestation_ifDuplicateSignatureNotIsEmpty_fido2RuntimeException() {
        X509Certificate cert1 = mock(X509Certificate.class);
        X509Certificate cert2 = mock(X509Certificate.class);
        List<X509Certificate> attestationCerts = Collections.singletonList(cert1);
        List<X509Certificate> trustChainCertificates = Arrays.asList(cert1, cert2);
        byte[] cert1Bytes = "TEST-cert1".getBytes();
        byte[] cert2Bytes = "TEST-cert2".getBytes();
        when(cert1.getSignature()).thenReturn(cert1Bytes);
        when(cert2.getSignature()).thenReturn(cert2Bytes);
        when(base64Service.encodeToString(cert1Bytes)).thenReturn("TEST-cert1-encoded");
        when(base64Service.encodeToString(cert2Bytes)).thenReturn("TEST-cert2-encoded");

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> certificateVerifier.checkForTrustedCertsInAttestation(attestationCerts, trustChainCertificates));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Root certificate in the attestation");
        verify(base64Service, times(3)).encodeToString(any());
    }

    @Test
    void checkForTrustedCertsInAttestation_ifDuplicateSignatureIsEmpty_valid() {
        X509Certificate cert1 = mock(X509Certificate.class);
        X509Certificate cert2 = mock(X509Certificate.class);
        X509Certificate cert3 = mock(X509Certificate.class);
        List<X509Certificate> attestationCerts = Collections.singletonList(cert1);
        List<X509Certificate> trustChainCertificates = Arrays.asList(cert2, cert3);
        byte[] cert1Bytes = "TEST-cert1".getBytes();
        byte[] cert2Bytes = "TEST-cert2".getBytes();
        byte[] cert3Bytes = "TEST-cert3".getBytes();
        when(cert1.getSignature()).thenReturn(cert1Bytes);
        when(cert2.getSignature()).thenReturn(cert2Bytes);
        when(cert3.getSignature()).thenReturn(cert3Bytes);
        when(base64Service.encodeToString(cert1Bytes)).thenReturn("TEST-cert1-encoded");
        when(base64Service.encodeToString(cert2Bytes)).thenReturn("TEST-cert2-encoded");
        when(base64Service.encodeToString(cert3Bytes)).thenReturn("TEST-cert3-encoded");

        certificateVerifier.checkForTrustedCertsInAttestation(attestationCerts, trustChainCertificates);
        verify(base64Service, times(3)).encodeToString(any());
    }

    @Test
    void verifyAttestationCertificates_ifTrustAnchorsIsEmpty_fido2MissingAttestationCertException() {
        X509Certificate cert1 = mock(X509Certificate.class);
        List<X509Certificate> certs = Collections.singletonList(cert1);
        List<X509Certificate> trustChainCertificates = Collections.emptyList();
        byte[] cert1Bytes = "TEST-cert1".getBytes();
        when(cert1.getSignature()).thenReturn(cert1Bytes);
        when(base64Service.encodeToString(cert1Bytes)).thenReturn("TEST-cert1-encoded");

        Fido2MissingAttestationCertException ex = assertThrows(Fido2MissingAttestationCertException.class, () -> certificateVerifier.verifyAttestationCertificates(certs, trustChainCertificates));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Trust anchors certs list is empty!");
        verifyNoInteractions(log);
    }

    @Test
    void verifyAttestationCertificates_ifCertIsNotNull_valid() throws CertificateException {
        X509Certificate cert1 = mock(X509Certificate.class);
        X509Certificate cert2 = mock(X509Certificate.class);
        X509Certificate cert3 = mock(X509Certificate.class);
        List<X509Certificate> certs = Collections.singletonList(cert1);
        List<X509Certificate> trustChainCertificates = Arrays.asList(cert2, cert3);
        byte[] cert1Bytes = "TEST-cert1".getBytes();
        byte[] cert2Bytes = "TEST-cert2".getBytes();
        byte[] cert3Bytes = "TEST-cert3".getBytes();
        when(cert1.getSignature()).thenReturn(cert1Bytes);
        when(cert2.getSignature()).thenReturn(cert2Bytes);
        when(cert3.getSignature()).thenReturn(cert3Bytes);
        when(base64Service.encodeToString(cert1Bytes)).thenReturn("TEST-cert1-encoded");
        when(base64Service.encodeToString(cert2Bytes)).thenReturn("TEST-cert2-encoded");
        when(base64Service.encodeToString(cert3Bytes)).thenReturn("TEST-cert3-encoded");
        CertPathValidator cpv = mock(CertPathValidator.class);
        when(certificateService.instanceCertPathValidatorPKIX()).thenReturn(cpv);
        when(cpv.getRevocationChecker()).thenReturn(mock(PKIXRevocationChecker.class));
        when(certificateService.instanceCertificateFactoryX509()).thenReturn(CertificateFactory.getInstance("X.509"));

        X509Certificate certificate = certificateVerifier.verifyAttestationCertificates(certs, trustChainCertificates);
        assertNotNull(certificate);
        verifyNoInteractions(log);
        verifyNoMoreInteractions(certificateService);
    }

    @Test
    void verifyAttestationCertificates_ifCertIsNull_null() throws CertificateException {
        X509Certificate cert2 = mock(X509Certificate.class);
        X509Certificate cert3 = mock(X509Certificate.class);
        List<X509Certificate> certs = Collections.emptyList();
        List<X509Certificate> trustChainCertificates = Arrays.asList(cert2, cert3);
        byte[] cert2Bytes = "TEST-cert2".getBytes();
        byte[] cert3Bytes = "TEST-cert3".getBytes();
        when(cert2.getSignature()).thenReturn(cert2Bytes);
        when(cert3.getSignature()).thenReturn(cert3Bytes);
        when(base64Service.encodeToString(cert2Bytes)).thenReturn("TEST-cert2-encoded");
        when(base64Service.encodeToString(cert3Bytes)).thenReturn("TEST-cert3-encoded");
        CertPathValidator cpv = mock(CertPathValidator.class);
        when(certificateService.instanceCertPathValidatorPKIX()).thenReturn(cpv);
        when(cpv.getRevocationChecker()).thenReturn(mock(PKIXRevocationChecker.class));
        when(certificateService.instanceCertificateFactoryX509()).thenReturn(CertificateFactory.getInstance("X.509"));

        X509Certificate certificate = certificateVerifier.verifyAttestationCertificates(certs, trustChainCertificates);
        assertNull(certificate);
        verifyNoInteractions(log);
        verify(certificateService, times(2)).instanceCertPathValidatorPKIX();
    }

    @Test
    void verifyAttestationCertificates_ifCertFactoryGenerateCertPathContainsException_certificateException() throws CertificateException {
        X509Certificate cert1 = mock(X509Certificate.class);
        X509Certificate cert2 = mock(X509Certificate.class);
        X509Certificate cert3 = mock(X509Certificate.class);
        List<X509Certificate> certs = Collections.singletonList(cert1);
        List<X509Certificate> trustChainCertificates = Arrays.asList(cert2, cert3);
        byte[] cert1Bytes = "TEST-cert1".getBytes();
        byte[] cert2Bytes = "TEST-cert2".getBytes();
        byte[] cert3Bytes = "TEST-cert3".getBytes();
        when(cert1.getSignature()).thenReturn(cert1Bytes);
        when(cert2.getSignature()).thenReturn(cert2Bytes);
        when(cert3.getSignature()).thenReturn(cert3Bytes);
        when(base64Service.encodeToString(cert1Bytes)).thenReturn("TEST-cert1-encoded");
        when(base64Service.encodeToString(cert2Bytes)).thenReturn("TEST-cert2-encoded");
        when(base64Service.encodeToString(cert3Bytes)).thenReturn("TEST-cert3-encoded");
        CertPathValidator cpv = mock(CertPathValidator.class);
        when(certificateService.instanceCertPathValidatorPKIX()).thenReturn(cpv);
        when(cpv.getRevocationChecker()).thenReturn(mock(PKIXRevocationChecker.class));
        CertificateFactory certPath = mock(CertificateFactory.class);
        when(certificateService.instanceCertificateFactoryX509()).thenReturn(certPath);
        when(certPath.generateCertPath(certs)).thenThrow(new CertificateException("Test CertificateException"));

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> certificateVerifier.verifyAttestationCertificates(certs, trustChainCertificates));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Problem with certificate");
        verify(log).warn(contains("Cert verification problem"), any(), any());
        verifyNoMoreInteractions(certificateService);
    }

    @Test
    void isSelfSigned_ifIsSelfSignedTrue_true() {
        X509Certificate cert = mock(X509Certificate.class);
        Principal principal = mock(Principal.class);
        when(cert.getIssuerDN()).thenReturn(principal);
        when(cert.getSubjectDN()).thenReturn(principal);

        boolean result = certificateVerifier.isSelfSigned(cert);
        assertTrue(result);
    }

    @Test
    void isSelfSigned_ifIsSelfSignedFalse_false() {
        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getIssuerDN()).thenReturn(mock(Principal.class));
        when(cert.getSubjectDN()).thenReturn(mock(Principal.class));

        boolean result = certificateVerifier.isSelfSigned(cert);
        assertFalse(result);
    }

    @Test
    void isSelfSigned1_ifIssuerDNAndSubjectDNAreEqual_true() throws CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
        X509Certificate cert = mock(X509Certificate.class);
        PublicKey key = mock(PublicKey.class);
        Principal principal = mock(Principal.class);
        when(cert.getIssuerDN()).thenReturn(principal);
        when(cert.getSubjectDN()).thenReturn(principal);

        boolean result = certificateVerifier.isSelfSigned(cert, key);
        assertTrue(result);
        verify(cert).verify(key);
        verifyNoInteractions(log);
    }

    @Test
    void isSelfSigned1_ifVerifyKeyThrownException_signatureException() throws CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
        X509Certificate cert = mock(X509Certificate.class);
        PublicKey key = mock(PublicKey.class);
        SignatureException ex = mock(SignatureException.class);
        doThrow(ex).when(cert).verify(key);

        boolean result = certificateVerifier.isSelfSigned(cert, key);
        assertFalse(result);
        verify(log).warn("Probably not self signed cert. Cert verification problem {}", ex.getMessage());
        verify(cert, never()).getIssuerDN();
        verify(cert, never()).getSubjectDN();
    }
}
