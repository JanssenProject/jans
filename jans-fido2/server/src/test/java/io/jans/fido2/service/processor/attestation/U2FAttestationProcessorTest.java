package io.jans.fido2.service.processor.attestation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.jans.fido2.exception.Fido2MissingAttestationCertException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.AttestationMode;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.verifier.AuthenticatorDataVerifier;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.UserVerificationVerifier;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.nio.file.attribute.UserPrincipal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class U2FAttestationProcessorTest {

    @InjectMocks
    private U2FAttestationProcessor u2FAttestationProcessor;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private Fido2Configuration fido2Configuration;

    @Mock
    private CommonVerifiers commonVerifiers;

    @Mock
    private AuthenticatorDataVerifier authenticatorDataVerifier;

    @Mock
    private UserVerificationVerifier userVerificationVerifier;

    @Mock
    private AttestationCertificateService attestationCertificateService;

    @Mock
    private CertificateVerifier certificateVerifier;

    @Mock
    private CoseService coseService;

    @Mock
    private Base64Service base64Service;

    @Mock
    private CertificateService certificateService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;
    
    

    @Test
    void getAttestationFormat_valid_fidoU2f() {
        String fmt = u2FAttestationProcessor.getAttestationFormat().getFmt();
        assertNotNull(fmt);
        assertEquals(fmt, "fido-u2f");
    }

    @Test
    void process_ifAttStmtHasX5cAndVerifyAttestationThrowErrorAndCertificatesIsEmpty_fido2MissingAttestationCertException() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData registration = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = new byte[]{};
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);
        JsonNode x5cNode = mock(JsonNode.class);
        when(registration.getOrigin()).thenReturn("test-domain");
        when(attStmt.hasNonNull("x5c")).thenReturn(true);
        when(attStmt.get("x5c")).thenReturn(x5cNode);
        when(x5cNode.elements()).thenReturn(Collections.emptyIterator());
        when(attStmt.get("sig")).thenReturn(mock(JsonNode.class));
        when(commonVerifiers.verifyBase64String(any())).thenReturn("test-signature");
        when(certificateVerifier.verifyAttestationCertificates(any(), any())).thenThrow(new Fido2MissingAttestationCertException("test missing"));
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());
        WebApplicationException res = assertThrows(WebApplicationException.class, () -> u2FAttestationProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(commonVerifiers).verifyAAGUIDZeroed(authData);
        verify(userVerificationVerifier).verifyUserPresent(authData);
        verify(userVerificationVerifier).verifyUserPresent(authData);
        verify(commonVerifiers).verifyRpIdHash(authData, "test-domain");
        verify(certificateService).getCertificates(anyList());
        verify(attestationCertificateService).getAttestationRootCertificates((JsonNode) eq(null), anyList());
        verify(certificateVerifier).verifyAttestationCertificates(anyList(), anyList());
        verify(authenticatorDataVerifier, never()).verifyU2FAttestationSignature(any(AuthData.class), any(byte[].class), any(String.class), any(X509Certificate.class), any(Integer.class));
        verify(log, never()).warn(contains("Failed to find attestation validation signature public certificate with DN"), anyString());
        verifyNoInteractions(log, authenticatorDataVerifier, coseService, base64Service);
    }

    @Test
    void process_ifAttStmprocess_ifAttStmtHasX5cAndVerifyAttestationThrowErrorAndCertificatesIsNotEmpty_badRequestExceptiotHasX5cAndSkipValidateMdsInAttestationIsFalseAndVerifyAttestationThrowErrorAndCertificatesIsNotEmpty_badRequestException() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData registration = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = new byte[]{};
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);
        JsonNode x5cNode = mock(JsonNode.class);
        when(registration.getOrigin()).thenReturn("test-domain");
        when(attStmt.hasNonNull("x5c")).thenReturn(true);
        when(attStmt.get("x5c")).thenReturn(x5cNode);
        when(x5cNode.elements()).thenReturn(Collections.singletonList((JsonNode) new TextNode("cert1")).iterator());
        when(attStmt.get("sig")).thenReturn(mock(JsonNode.class));
        when(commonVerifiers.verifyBase64String(any())).thenReturn("test-signature");
        when(certificateVerifier.verifyAttestationCertificates(any(), any())).thenThrow(new Fido2MissingAttestationCertException("test missing"));
        X509Certificate publicCert1 = mock(X509Certificate.class);
        when(certificateService.getCertificates(anyList())).thenReturn(Collections.singletonList(publicCert1));
        when(publicCert1.getIssuerDN()).thenReturn((UserPrincipal) () -> "test-issuer");
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());
        WebApplicationException res = assertThrows(WebApplicationException.class, () -> u2FAttestationProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(commonVerifiers).verifyAAGUIDZeroed(authData);
        verify(userVerificationVerifier).verifyUserPresent(authData);
        verify(userVerificationVerifier).verifyUserPresent(authData);
        verify(commonVerifiers).verifyRpIdHash(authData, "test-domain");
        verify(certificateService).getCertificates(anyList());
        verify(attestationCertificateService).getAttestationRootCertificates((JsonNode) eq(null), anyList());
        verify(certificateVerifier).verifyAttestationCertificates(anyList(), anyList());
        verify(authenticatorDataVerifier, never()).verifyU2FAttestationSignature(any(AuthData.class), any(byte[].class), any(String.class), any(X509Certificate.class), any(Integer.class));
        verify(log).warn("Failed to find attestation validation signature public certificate with DN: '{}'", "test-issuer");
        verifyNoInteractions(authenticatorDataVerifier, coseService, base64Service);
    }

    @Test
    void process_ifAttStmtHasX5cAndCertificatesIsNotEmptyAndVerifyAttestationIsValid_success() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData registration = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = new byte[]{};
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);
        JsonNode x5cNode = mock(JsonNode.class);
        when(registration.getOrigin
        		()).thenReturn("test-domain");
        when(attStmt.hasNonNull("x5c")).thenReturn(true);
        when(attStmt.get("x5c")).thenReturn(x5cNode);
        when(x5cNode.elements()).thenReturn(Collections.singletonList((JsonNode) new TextNode("cert1")).iterator());
        when(attStmt.get("sig")).thenReturn(mock(JsonNode.class));
        X509Certificate verifiedCert = mock(X509Certificate.class);
        when(commonVerifiers.verifyBase64String(any())).thenReturn("test-signature");
        when(certificateVerifier.verifyAttestationCertificates(any(), any())).thenReturn(verifiedCert);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());
        u2FAttestationProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters);
        verify(commonVerifiers).verifyAAGUIDZeroed(authData);
        verify(userVerificationVerifier).verifyUserPresent(authData);
        verify(userVerificationVerifier).verifyUserPresent(authData);
        verify(commonVerifiers).verifyRpIdHash(authData, "test-domain");
        verify(certificateService).getCertificates(anyList());
        verify(attestationCertificateService).getAttestationRootCertificates((JsonNode) eq(null), anyList());
        verify(certificateVerifier).verifyAttestationCertificates(anyList(), anyList());
        verify(authenticatorDataVerifier).verifyU2FAttestationSignature(any(AuthData.class), any(byte[].class), any(String.class), any(X509Certificate.class), any(Integer.class));
        verify(base64Service, times(2)).urlEncodeToString(any());
        verifyNoInteractions(log, coseService);
    }



    @Test
    void process_ifAttStmtHasEcdaaKeyId_badRequestException() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData registration = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = new byte[]{};
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);
        when(registration.getOrigin()).thenReturn("test-domain");
        when(attStmt.get("sig")).thenReturn(mock(JsonNode.class));
        when(attStmt.hasNonNull("x5c")).thenReturn(false);
        when(attStmt.hasNonNull("ecdaaKeyId")).thenReturn(true);
        when(attStmt.get("ecdaaKeyId")).thenReturn(new TextNode("test-ecdaaKeyId"));
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> u2FAttestationProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(fido2Configuration).getAttestationMode();
        verify(commonVerifiers).verifyBase64String(any());
        verify(commonVerifiers).verifyAAGUIDZeroed(authData);
        verify(userVerificationVerifier).verifyUserPresent(authData);
        verify(commonVerifiers).verifyRpIdHash(authData, "test-domain");
        verify(log).warn("Fido-U2F unsupported EcdaaKeyId: {}", "test-ecdaaKeyId");
        verifyNoMoreInteractions(log);
        verifyNoInteractions(certificateService, certificateVerifier, attestationCertificateService, authenticatorDataVerifier, coseService, base64Service);
    }

    @Test
    void process_ifAttStmtNotIsX5cOrEcdaaKeyId_success() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData registration = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = new byte[]{};
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);
        when(registration.getOrigin()).thenReturn("test-domain");
        when(authData.getAuthDataDecoded()).thenReturn("test-decoded".getBytes());
        when(attStmt.get("sig")).thenReturn(mock(JsonNode.class));
        when(commonVerifiers.verifyBase64String(any())).thenReturn("test-signature");
        when(attStmt.hasNonNull("x5c")).thenReturn(false);
        when(attStmt.hasNonNull("ecdaaKeyId")).thenReturn(false);
        PublicKey publicKey = mock(PublicKey.class);
        when(coseService.getPublicKeyFromUncompressedECPoint(any())).thenReturn(publicKey);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());

        u2FAttestationProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters);
        verify(appConfiguration).getFido2Configuration();
        verify(fido2Configuration).getAttestationMode();
        verify(commonVerifiers).verifyBase64String(any());
        verify(commonVerifiers).verifyAAGUIDZeroed(authData);
        verify(userVerificationVerifier).verifyUserPresent(authData);
        verify(commonVerifiers).verifyRpIdHash(authData, "test-domain");
        verify(coseService).getPublicKeyFromUncompressedECPoint(any());
        verify(authenticatorDataVerifier).verifyPackedSurrogateAttestationSignature(authData.getAuthDataDecoded(), clientDataHash, "test-signature", publicKey, -7);
        verifyNoInteractions(log, certificateService, certificateVerifier);
    }
}
