package io.jans.fido2.service.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.Certificate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticatorDataVerifierTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private AuthenticatorDataVerifier authenticatorDataVerifier;

    @Mock
    private Logger log;

    @Mock
    private Base64Service base64Service;

    @Mock
    private DataMapperService dataMapperService;

    @Mock
    private SignatureVerifier signatureVerifier;

    @Test
    void verifyPackedAttestationSignature_validParam_valid() {
        AuthData authData = new AuthData();
        authData.setRpIdHash("TEST-rpIdHash".getBytes());
        authData.setFlags("TEST-flags".getBytes());
        authData.setCounters("TEST-counter".getBytes());
        byte[] clientDataHash = "TEST-clientDataHash".getBytes();
        String signature = "TEST-signature";
        Certificate certificate = mock(Certificate.class);
        int signatureAlgorithm = -257;
        when(base64Service.decode(signature.getBytes())).thenReturn("VEVTVC1zaWduYXR1cmU=".getBytes());

        authenticatorDataVerifier.verifyPackedAttestationSignature(authData, clientDataHash, signature, certificate, signatureAlgorithm);
        verify(log, times(2)).debug(anyString(), anyString());
        verify(log).debug(anyString(), anyInt());
        verify(signatureVerifier).verifySignature(any(), any(), any(Certificate.class), anyInt());
    }

    @Test
    void verifyPackedAttestationSignature1_validParam_valid() {
        byte[] authData = "TEST-authData".getBytes();
        byte[] clientDataHash = "TEST-clientDataHash".getBytes();
        String signature = "TEST-signature";
        PublicKey key = mock(PublicKey.class);
        int signatureAlgorithm = -257;
        when(base64Service.decode(signature.getBytes())).thenReturn("VEVTVC1zaWduYXR1cmU=".getBytes());

        authenticatorDataVerifier.verifyPackedAttestationSignature(authData, clientDataHash, signature, key, signatureAlgorithm);
        verify(log, times(2)).debug(anyString(), anyString());
        verify(log).debug(anyString(), anyInt());
        verify(signatureVerifier).verifySignature(any(), any(), any(PublicKey.class), anyInt());
    }

    @Test
    void verifyPackedAttestationSignature2_validParam_valid() {
        byte[] authData = "TEST-authData".getBytes();
        byte[] clientDataHash = "TEST-clientDataHash".getBytes();
        String signature = "TEST-signature";
        Certificate certificate = mock(Certificate.class);
        int signatureAlgorithm = -257;
        when(base64Service.decode(signature.getBytes())).thenReturn("VEVTVC1zaWduYXR1cmU=".getBytes());

        authenticatorDataVerifier.verifyPackedAttestationSignature(authData, clientDataHash, signature, certificate, signatureAlgorithm);
        verify(certificate).getPublicKey();
    }

    @Test
    void verifyPackedSurrogateAttestationSignature_validParam_valid() {
        byte[] authData = "TEST-authData".getBytes();
        byte[] clientDataHash = "TEST-clientDataHash".getBytes();
        String signature = "TEST-signature";
        PublicKey publicKey = mock(PublicKey.class);
        int signatureAlgorithm = -257;
        when(base64Service.decode(signature.getBytes())).thenReturn("VEVTVC1zaWduYXR1cmU=".getBytes());

        authenticatorDataVerifier.verifyPackedSurrogateAttestationSignature(authData, clientDataHash, signature, publicKey, signatureAlgorithm);
        verify(log, times(2)).debug(anyString(), anyString());
        verify(log).debug(anyString(), anyInt());
        verify(signatureVerifier).verifySignature(any(), any(), any(PublicKey.class), anyInt());
    }

    @Test
    void verifyAssertionSignature_authDataExtensionWithValue_valid() {
        AuthData authData = new AuthData();
        authData.setRpIdHash("TEST-rpIdHash".getBytes());
        authData.setFlags("TEST-flags".getBytes());
        authData.setCounters("TEST-counter".getBytes());
        authData.setExtensions("TEST-extensions".getBytes());
        byte[] clientDataHash = "TEST-clientDataHash".getBytes();
        String signature = "TEST-signature";
        PublicKey publicKey = mock(PublicKey.class);
        int signatureAlgorithm = -257;
        when(base64Service.urlDecode(signature.getBytes())).thenReturn("VEVTVC1zaWduYXR1cmU=".getBytes());

        authenticatorDataVerifier.verifyAssertionSignature(authData, clientDataHash, signature, publicKey, signatureAlgorithm);
        verify(log, times(8)).debug(anyString(), anyString());
        verify(log).debug(anyString(), anyInt());
        verify(signatureVerifier).verifySignature(any(), any(), any(PublicKey.class), anyInt());
    }

    @Test
    void verifyAssertionSignature_authDataExtensionIsNull_valid() {
        AuthData authData = new AuthData();
        authData.setRpIdHash("TEST-rpIdHash".getBytes());
        authData.setFlags("TEST-flags".getBytes());
        authData.setCounters("TEST-counter".getBytes());
        byte[] clientDataHash = "TEST-clientDataHash".getBytes();
        String signature = "TEST-signature";
        PublicKey publicKey = mock(PublicKey.class);
        int signatureAlgorithm = -257;
        when(base64Service.urlDecode(signature.getBytes())).thenReturn("VEVTVC1zaWduYXR1cmU=".getBytes());

        authenticatorDataVerifier.verifyAssertionSignature(authData, clientDataHash, signature, publicKey, signatureAlgorithm);
        verify(log, times(8)).debug(anyString(), anyString());
        verify(log).debug(anyString(), anyInt());
        verify(signatureVerifier).verifySignature(any(), any(), any(PublicKey.class), anyInt());
    }

    @Test
    void verifyU2FAttestationSignature_validParam_valid() throws IOException {
        AuthData authData = new AuthData();
        authData.setRpIdHash("TEST-rpIdHash".getBytes());
        authData.setCredId("TEST-credId".getBytes());
        authData.setCosePublicKey("TEST-cosePublicKey".getBytes());
        byte[] clientDataHash = "TEST-clientDataHash".getBytes();
        String signature = "TEST-signature";
        Certificate certificate = mock(Certificate.class);
        int signatureAlgorithm = -257;
        ObjectNode cborPublicKey = mapper.createObjectNode();
        cborPublicKey.put("-2", "test-2-value");
        cborPublicKey.put("-3", "test-3-value");
        when(base64Service.decode(signature.getBytes())).thenReturn("VEVTVC1zaWduYXR1cmU=".getBytes());
        when(dataMapperService.cborReadTree(authData.getCosePublicKey())).thenReturn(cborPublicKey);
        when(base64Service.decode(cborPublicKey.get("-2").asText())).thenReturn("dGVzdC0yLXZhbHVl".getBytes());
        when(base64Service.decode(cborPublicKey.get("-3").asText())).thenReturn("dGVzdC0zLXZhbHVl".getBytes());

        authenticatorDataVerifier.verifyU2FAttestationSignature(authData, clientDataHash, signature, certificate, signatureAlgorithm);
        verify(log, times(3)).debug(anyString(), anyString());
        verify(signatureVerifier).verifySignature(any(), any(), any(Certificate.class), anyInt());
    }

    @Test
    void verifyU2FAttestationSignature1_validParam_valid() throws IOException {
        AuthData authData = new AuthData();
        authData.setCredId("TEST-credId".getBytes());
        authData.setCosePublicKey("TEST-cosePublicKey".getBytes());
        byte[] rpIdHash = "TEST-rpIdHash".getBytes();
        byte[] clientDataHash = "TEST-clientDataHash".getBytes();
        String signature = "TEST-signature";
        Certificate certificate = mock(Certificate.class);
        int signatureAlgorithm = -257;
        ObjectNode cborPublicKey = mapper.createObjectNode();
        cborPublicKey.put("-2", "test-2-value");
        cborPublicKey.put("-3", "test-3-value");
        when(base64Service.decode(signature.getBytes())).thenReturn("VEVTVC1zaWduYXR1cmU=".getBytes());
        when(dataMapperService.cborReadTree(authData.getCosePublicKey())).thenReturn(cborPublicKey);
        when(base64Service.decode(cborPublicKey.get("-2").asText())).thenReturn("dGVzdC0yLXZhbHVl".getBytes());
        when(base64Service.decode(cborPublicKey.get("-3").asText())).thenReturn("dGVzdC0zLXZhbHVl".getBytes());

        authenticatorDataVerifier.verifyU2FAttestationSignature(authData, rpIdHash, clientDataHash, signature, certificate, signatureAlgorithm);
        verify(log, times(3)).debug(anyString(), anyString());
        verify(signatureVerifier).verifySignature(any(), any(), any(Certificate.class), anyInt());
    }

    @Test
    void verifyAttestationSignature_validParam_valid() {
        AuthData authData = new AuthData();
        authData.setAttestationBuffer("TEST-attestationBuffer".getBytes());
        byte[] clientDataHash = "TEST-clientDataHash".getBytes();
        String signature = "TEST-signature";
        Certificate certificate = mock(Certificate.class);
        int signatureAlgorithm = -257;
        when(base64Service.decode(signature.getBytes())).thenReturn("VEVTVC1zaWduYXR1cmU=".getBytes());

        authenticatorDataVerifier.verifyAttestationSignature(authData, clientDataHash, signature, certificate, signatureAlgorithm);
        verify(log, times(2)).debug(anyString(), anyString());
        verify(signatureVerifier).verifySignature(any(), any(), any(Certificate.class), anyInt());
    }
}