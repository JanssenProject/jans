package io.jans.fido2.service.processor.attestation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.util.AppleUtilService;
import io.jans.fido2.service.util.CommonUtilService;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppleAttestationProcessorTest {

    @InjectMocks
    private AppleAttestationProcessor appleAttestationProcessor;

    @Mock
    private Logger log;

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
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private CommonUtilService commonUtilService;

    @Mock
    private AppleUtilService appleUtilService;

    @Mock
    private CommonVerifiers commonVerifiers;

    @Test
    void getAttestationFormat_valid_apple() {
        String fmt = appleAttestationProcessor.getAttestationFormat().getFmt();
        assertNotNull(fmt);
        assertEquals(fmt, "apple");
    }

    @Test
    void process_ifCertificatesIsEmpty_badRequestException() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.asText()).thenReturn("test_att_stmt");
        when(attStmt.hasNonNull("x5c")).thenReturn(true);
        JsonNode x5cNode = mock(JsonNode.class);
        when(attStmt.get("x5c")).thenReturn(x5cNode);
        when(x5cNode.elements()).thenReturn(Collections.emptyIterator());
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> appleAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(log).info(eq("AttStmt: test_att_stmt"));
        verifyNoInteractions(certificateService, appConfiguration, attestationCertificateService, certificateVerifier, coseService, base64Service);
        verifyNoMoreInteractions(log, errorResponseFactory);
    }

    @Test
    void process_ifGetRootCertificatesBySubjectDN_badRequestException() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.asText()).thenReturn("test_att_stmt");
        when(attStmt.hasNonNull("x5c")).thenReturn(true);
        JsonNode x5cNode = mock(JsonNode.class);
        when(attStmt.get("x5c")).thenReturn(x5cNode);
        when(x5cNode.elements()).thenReturn(Collections.singletonList((JsonNode) new TextNode("x5c item")).iterator());
        X509Certificate credCert = mock(X509Certificate.class);
        when(certificateService.getCertificate(anyString())).thenReturn(credCert);
        when(attestationCertificateService.getRootCertificatesBySubjectDN(anyString())).thenThrow(new Fido2RuntimeException("test exception"));
        when(credCert.getIssuerDN()).thenReturn(new BasicUserPrincipal("test issuer dn"));
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> appleAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(log).info(eq("AttStmt: test_att_stmt"));
        verify(certificateService).getCertificate(eq("x5c item"));
        verify(attestationCertificateService).getRootCertificatesBySubjectDN(anyString());
        verify(log).warn(eq("Failed to find attestation validation signature public certificate with DN: '{}'"), eq("test issuer dn"));
        verify(errorResponseFactory).badRequestException(any(), eq("Failed to find attestation validation signature public certificate with DN: test issuer dn"));
        verifyNoMoreInteractions(log, errorResponseFactory);
        verifyNoInteractions(certificateVerifier, coseService, base64Service);
    }

    @Test
    void process_ifByArrayOutputStreamThrownError_badRequestException() throws IOException {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.asText()).thenReturn("test_att_stmt");
        when(attStmt.hasNonNull("x5c")).thenReturn(true);
        JsonNode x5cNode = mock(JsonNode.class);
        when(attStmt.get("x5c")).thenReturn(x5cNode);
        when(x5cNode.elements()).thenReturn(Collections.singletonList((JsonNode) new TextNode("x5c item")).iterator());
        X509Certificate credCert = mock(X509Certificate.class);
        when(certificateService.getCertificate(anyString())).thenReturn(credCert);
        List<X509Certificate> rootCertificates = Collections.singletonList(mock(X509Certificate.class));
        when(attestationCertificateService.getRootCertificatesBySubjectDN(anyString())).thenReturn(rootCertificates);
        when(certificateVerifier.verifyAttestationCertificates(anyList(), anyList())).thenReturn(mock(X509Certificate.class));
        when(authData.getAuthDataDecoded()).thenReturn("test decoded".getBytes());
        when(commonUtilService.writeOutputStreamByteList(anyList())).thenThrow(new IOException("test ioexception"));
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> appleAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(log).info(eq("AttStmt: test_att_stmt"));
        verify(certificateService).getCertificate(eq("x5c item"));
        verify(attestationCertificateService).getRootCertificatesBySubjectDN(anyString());
        verify(log).debug(eq("APPLE_WEBAUTHN_ROOT_CA root certificate: 1"));
        verify(certificateVerifier).verifyAttestationCertificates(anyList(), anyList());
        verify(log).info(eq("Step 1 completed"));
        verify(commonUtilService).writeOutputStreamByteList(anyList());
        verify(errorResponseFactory).badRequestException(any(), eq("Concatenate |authenticatorData| and |clientDataHash| to form |nonceToHash| : test ioexception"));
        verifyNoMoreInteractions(log, errorResponseFactory);
        verifyNoInteractions(coseService, base64Service);
    }

    @Test
    void process_ifNonceAndAttestationChallengeAreNotEquals_badRequestException() throws IOException {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.asText()).thenReturn("test_att_stmt");
        when(attStmt.hasNonNull("x5c")).thenReturn(true);
        JsonNode x5cNode = mock(JsonNode.class);
        when(attStmt.get("x5c")).thenReturn(x5cNode);
        when(x5cNode.elements()).thenReturn(Collections.singletonList((JsonNode) new TextNode("x5c item")).iterator());
        X509Certificate credCert = mock(X509Certificate.class);
        when(certificateService.getCertificate(anyString())).thenReturn(credCert);
        List<X509Certificate> rootCertificates = Collections.singletonList(mock(X509Certificate.class));
        when(attestationCertificateService.getRootCertificatesBySubjectDN(anyString())).thenReturn(rootCertificates);
        when(certificateVerifier.verifyAttestationCertificates(anyList(), anyList())).thenReturn(mock(X509Certificate.class));
        when(authData.getAuthDataDecoded()).thenReturn("test decoded".getBytes());
        ByteArrayOutputStream baos = mock(ByteArrayOutputStream.class);
        when(commonUtilService.writeOutputStreamByteList(anyList())).thenReturn(baos);
        when(baos.toByteArray()).thenReturn("test baos".getBytes());
        when(appleUtilService.getExtension(any())).thenReturn("test_challenge".getBytes());
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> appleAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(log).info(eq("AttStmt: test_att_stmt"));
        verify(certificateService).getCertificate(eq("x5c item"));
        verify(attestationCertificateService).getRootCertificatesBySubjectDN(anyString());
        verify(log).debug(eq("APPLE_WEBAUTHN_ROOT_CA root certificate: 1"));
        verify(certificateVerifier).verifyAttestationCertificates(anyList(), anyList());
        verify(log).info(eq("Step 1 completed"));
        verify(commonUtilService).writeOutputStreamByteList(anyList());
        verify(log).info(eq("Step 2 completed"));
        verify(log).info(eq("Step 3 completed"));
        verify(appleUtilService).getExtension(any());
        verify(errorResponseFactory).badRequestException(any(), eq("Certificate 1.2.840.113635.100.8.2 extension does not match nonce"));
        verifyNoMoreInteractions(log, errorResponseFactory);
        verifyNoInteractions(coseService, base64Service);
    }

    @Test
    void process_ifPublicKeyAuthDataAndPublicCredCertAreNotEquals_badRequestException() throws IOException {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.asText()).thenReturn("test_att_stmt");
        when(attStmt.hasNonNull("x5c")).thenReturn(true);
        JsonNode x5cNode = mock(JsonNode.class);
        when(attStmt.get("x5c")).thenReturn(x5cNode);
        when(x5cNode.elements()).thenReturn(Collections.singletonList((JsonNode) new TextNode("x5c item")).iterator());
        X509Certificate credCert = mock(X509Certificate.class);
        when(certificateService.getCertificate(anyString())).thenReturn(credCert);
        List<X509Certificate> rootCertificates = Collections.singletonList(mock(X509Certificate.class));
        when(attestationCertificateService.getRootCertificatesBySubjectDN(anyString())).thenReturn(rootCertificates);
        when(certificateVerifier.verifyAttestationCertificates(anyList(), anyList())).thenReturn(mock(X509Certificate.class));
        when(authData.getAuthDataDecoded()).thenReturn("test decoded".getBytes());
        ByteArrayOutputStream baos = mock(ByteArrayOutputStream.class);
        when(commonUtilService.writeOutputStreamByteList(anyList())).thenReturn(baos);
        when(baos.toByteArray()).thenReturn("test baos".getBytes());
        when(appleUtilService.getExtension(any())).thenReturn(DigestUtils.getSha256Digest().digest("test baos".getBytes()));
        when(coseService.getPublicKeyFromUncompressedECPoint(any())).thenReturn(mock(PublicKey.class));
        when(credCert.getPublicKey()).thenReturn(mock(PublicKey.class));
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> appleAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(log).info(eq("AttStmt: test_att_stmt"));
        verify(certificateService).getCertificate(eq("x5c item"));
        verify(attestationCertificateService).getRootCertificatesBySubjectDN(anyString());
        verify(log).debug(eq("APPLE_WEBAUTHN_ROOT_CA root certificate: 1"));
        verify(certificateVerifier).verifyAttestationCertificates(anyList(), anyList());
        verify(log).info(eq("Step 1 completed"));
        verify(commonUtilService).writeOutputStreamByteList(anyList());
        verify(log).info(eq("Step 2 completed"));
        verify(log).info(eq("Step 3 completed"));
        verify(log).info(eq("Step 4 completed"));
        verify(appleUtilService).getExtension(any());
        verify(coseService).getPublicKeyFromUncompressedECPoint(any());
        verify(errorResponseFactory).badRequestException(any(), eq("The public key in the first certificate in x5c doesn't matches the credentialPublicKey in the attestedCredentialData in authenticatorData."));
        verifyNoMoreInteractions(log, errorResponseFactory);
        verifyNoInteractions(base64Service);
    }

    @Test
    void process_validData_success() throws IOException {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.asText()).thenReturn("test_att_stmt");
        when(attStmt.hasNonNull("x5c")).thenReturn(true);
        JsonNode x5cNode = mock(JsonNode.class);
        when(attStmt.get("x5c")).thenReturn(x5cNode);
        when(x5cNode.elements()).thenReturn(Collections.singletonList((JsonNode) new TextNode("x5c item")).iterator());
        X509Certificate credCert = mock(X509Certificate.class);
        when(certificateService.getCertificate(anyString())).thenReturn(credCert);
        List<X509Certificate> rootCertificates = Collections.singletonList(mock(X509Certificate.class));
        when(attestationCertificateService.getRootCertificatesBySubjectDN(anyString())).thenReturn(rootCertificates);
        when(certificateVerifier.verifyAttestationCertificates(anyList(), anyList())).thenReturn(mock(X509Certificate.class));
        when(authData.getAuthDataDecoded()).thenReturn("test decoded".getBytes());
        ByteArrayOutputStream baos = mock(ByteArrayOutputStream.class);
        when(commonUtilService.writeOutputStreamByteList(anyList())).thenReturn(baos);
        when(baos.toByteArray()).thenReturn("test baos".getBytes());
        when(appleUtilService.getExtension(any())).thenReturn(DigestUtils.getSha256Digest().digest("test baos".getBytes()));
        PublicKey publicKey = mock(PublicKey.class);
        when(coseService.getPublicKeyFromUncompressedECPoint(any())).thenReturn(publicKey);
        when(credCert.getPublicKey()).thenReturn(publicKey);
        when(authData.getCredId()).thenReturn("test_cred_id".getBytes());
        when(authData.getCosePublicKey()).thenReturn("test_cose_public_key".getBytes());
        when(base64Service.urlEncodeToString(any(byte[].class))).thenReturn("test_cred_id", "test_uncompressed_ec_point");

        when(attStmt.get("alg")).thenReturn(mock(JsonNode.class));
        when(authData.getKeyType()).thenReturn(1);
        when(commonVerifiers.verifyAlgorithm(any(JsonNode.class), anyInt())).thenReturn(1);

        appleAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters);

        verify(log).info(eq("AttStmt: test_att_stmt"));
        verify(certificateService).getCertificate(eq("x5c item"));
        verify(attestationCertificateService).getRootCertificatesBySubjectDN(anyString());
        verify(log).debug(eq("APPLE_WEBAUTHN_ROOT_CA root certificate: 1"));
        verify(certificateVerifier).verifyAttestationCertificates(anyList(), anyList());
        verify(log).info(eq("Step 1 completed"));
        verify(commonUtilService).writeOutputStreamByteList(anyList());
        verify(log).info(eq("Step 2 completed"));
        verify(log).info(eq("Step 3 completed"));
        verify(log).info(eq("Step 4 completed"));
        verify(log).info(eq("Step 5 completed"));
        verify(appleUtilService).getExtension(any());
        verify(coseService).getPublicKeyFromUncompressedECPoint(any());
        verify(base64Service, times(2)).urlEncodeToString(any());
        verify(commonVerifiers).verifyAlgorithm(any(JsonNode.class), eq(1));
        verifyNoMoreInteractions(log);
        verifyNoInteractions(errorResponseFactory);
    }
}
