package io.jans.fido2.service.processor.attestation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import static io.jans.util.TestUtil.instanceMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackedAttestationProcessorTest {

    @InjectMocks
    private PackedAttestationProcessor packedAttestationProcessor;

    @Mock
    private Logger log;

    @Mock
    private CommonVerifiers commonVerifiers;

    @Mock
    private AuthenticatorDataVerifier authenticatorDataVerifier;

    @Mock
    private CertificateVerifier certificateVerifier;

    @Mock
    private CoseService coseService;

    @Mock
    private Base64Service base64Service;

    @Mock
    private AttestationCertificateService attestationCertificateService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private Fido2Configuration fido2Configuration;

    @Mock
    private CertificateService certificateService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void getAttestationFormat_valid_packed() {
        String fmt = packedAttestationProcessor.getAttestationFormat().getFmt();
        assertNotNull(fmt);
        assertEquals(fmt, "packed");
    }

    @Test
    void process_ifAttStmtHasX5cAndTrustManagerIsNull_badRequestException() throws DecoderException {
        ObjectNode attStmt = instanceMapper().createObjectNode();
        ArrayNode x5cArray = instanceMapper().createArrayNode();
        x5cArray.add("certPath1");
        attStmt.set("x5c", x5cArray);
        attStmt.put("alg", -7);
        attStmt.put("sig", "test-signature");
        AuthData authData = new AuthData();
        authData.setKeyType(-7);
        String hexAaguid = "6161677569642d74657374";
        authData.setAaguid(Hex.decodeHex(hexAaguid.toCharArray()));
        Fido2RegistrationData registration = new Fido2RegistrationData();
        byte[] clientDataHash = "test-clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = new CredAndCounterData();

        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(
                new WebApplicationException(Response.status(400).entity("test exception").build())
        );
        when(certificateService.getCertificates(anyList())).thenReturn(List.of());
        WebApplicationException res = assertThrows(WebApplicationException.class, () ->
                packedAttestationProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters)
        );

        // Verify the exception and response details
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(commonVerifiers).verifyAlgorithm(any(JsonNode.class), any(Integer.class));
        verify(commonVerifiers).verifyBase64String(any(JsonNode.class));
        verify(attestationCertificateService).populateTrustManager(any(AuthData.class), anyList());
        verifyNoInteractions(certificateVerifier, coseService, authenticatorDataVerifier, base64Service);
    }

    @Test
    void process_ifAttStmtHasX5cAndAcceptedIssuersLengthIsZero_fido2RuntimeException() throws DecoderException {
        ObjectNode attStmt = instanceMapper().createObjectNode();
        ArrayNode x5cArray = instanceMapper().createArrayNode();
        x5cArray.add("certPath1");
        attStmt.set("x5c", x5cArray);
        attStmt.put("alg", -7);
        attStmt.put("sig", "test-signature");
        AuthData authData = new AuthData();
        authData.setKeyType(-7);
        String hexAaguid = "6161677569642d74657374";
        authData.setAaguid(Hex.decodeHex(hexAaguid.toCharArray()));
        Fido2RegistrationData registration = new Fido2RegistrationData();
        byte[] clientDataHash = "test-clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = new CredAndCounterData();
        List<X509Certificate> certificates = Collections.singletonList(mock(X509Certificate.class));
        when(certificateService.getCertificates(anyList())).thenReturn(certificates);
        X509TrustManager tm = mock(X509TrustManager.class);
        when(attestationCertificateService.populateTrustManager(authData, certificates)).thenReturn(tm);
        when(tm.getAcceptedIssuers()).thenReturn(new X509Certificate[]{});
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> packedAttestationProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(commonVerifiers).verifyAlgorithm(any(JsonNode.class), any(Integer.class));
        verify(commonVerifiers).verifyBase64String(any(JsonNode.class));
        verify(attestationCertificateService).populateTrustManager(any(AuthData.class), anyList());
        verifyNoInteractions(certificateVerifier, coseService, authenticatorDataVerifier, base64Service);
    }

    @Test
    void process_ifAttStmtHasX5cAndTrustManagerAndIsSelfSignedTrue_badRequestException() throws DecoderException {
        ObjectNode attStmt = instanceMapper().createObjectNode();
        ArrayNode x5cArray = instanceMapper().createArrayNode();
        x5cArray.add("certPath1");
        attStmt.set("x5c", x5cArray);
        int alg = -7;
        attStmt.put("alg", alg);
        attStmt.put("sig", "test-signature");
        AuthData authData = new AuthData();
        authData.setKeyType(alg);
        String hexAaguid = "6161677569642d74657374";
        authData.setAaguid(Hex.decodeHex(hexAaguid.toCharArray()));
        authData.setAuthDataDecoded("test-AuthDataDecoded".getBytes());
        Fido2RegistrationData registration = new Fido2RegistrationData();
        byte[] clientDataHash = "test-clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = new CredAndCounterData();
        String signature = "test-signature";
        when(commonVerifiers.verifyAlgorithm(any(), anyInt())).thenReturn(alg);
        when(commonVerifiers.verifyBase64String(any())).thenReturn(signature);
        List<X509Certificate> certificates = Collections.singletonList(mock(X509Certificate.class));
        when(certificateService.getCertificates(anyList())).thenReturn(certificates);
        X509TrustManager tm = mock(X509TrustManager.class);
        when(attestationCertificateService.populateTrustManager(authData, certificates)).thenReturn(tm);
        X509Certificate verifiedCert = mock(X509Certificate.class);
        when(certificateVerifier.verifyAttestationCertificates(anyList(), anyList())).thenReturn(verifiedCert);
        when(tm.getAcceptedIssuers()).thenReturn(new X509Certificate[]{mock(X509Certificate.class)});
        when(certificateVerifier.isSelfSigned(any())).thenReturn(true);
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> packedAttestationProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(commonVerifiers).verifyAlgorithm(any(JsonNode.class), any(Integer.class));
        verify(commonVerifiers).verifyBase64String(any(JsonNode.class));
        verify(attestationCertificateService).populateTrustManager(any(AuthData.class), anyList());
        verify(authenticatorDataVerifier).verifyPackedAttestationSignature(authData.getAuthDataDecoded(), clientDataHash, signature, verifiedCert, alg);
        verifyNoMoreInteractions(authenticatorDataVerifier);
        verifyNoInteractions(coseService, base64Service);
    }

    @Test
    void process_ifAttStmtHasX5cAndTrustManagerAndIsSelfSignedTrue_success() throws DecoderException {
        ObjectNode attStmt = instanceMapper().createObjectNode();
        ArrayNode x5cArray = instanceMapper().createArrayNode();
        x5cArray.add("certPath1");
        attStmt.set("x5c", x5cArray);
        int alg = -7;
        attStmt.put("alg", alg);
        attStmt.put("sig", "test-signature");
        AuthData authData = new AuthData();
        authData.setKeyType(alg);
        String hexAaguid = "6161677569642d74657374";
        authData.setAaguid(Hex.decodeHex(hexAaguid.toCharArray()));
        authData.setAuthDataDecoded("test-AuthDataDecoded".getBytes());
        Fido2RegistrationData registration = new Fido2RegistrationData();
        byte[] clientDataHash = "test-clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = new CredAndCounterData();
        String signature = "test-signature";
        when(commonVerifiers.verifyAlgorithm(any(), anyInt())).thenReturn(alg);
        when(commonVerifiers.verifyBase64String(any())).thenReturn(signature);
        List<X509Certificate> certificates = Collections.singletonList(mock(X509Certificate.class));
        when(certificateService.getCertificates(anyList())).thenReturn(certificates);
        X509TrustManager tm = mock(X509TrustManager.class);
        when(attestationCertificateService.populateTrustManager(authData, certificates)).thenReturn(tm);
        X509Certificate verifiedCert = mock(X509Certificate.class);
        when(certificateVerifier.verifyAttestationCertificates(anyList(), anyList())).thenReturn(verifiedCert);
        when(tm.getAcceptedIssuers()).thenReturn(new X509Certificate[]{mock(X509Certificate.class)});
        when(certificateVerifier.isSelfSigned(any())).thenReturn(false);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());

        packedAttestationProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters);
        verify(commonVerifiers).verifyAlgorithm(any(JsonNode.class), any(Integer.class));
        verify(commonVerifiers).verifyBase64String(any(JsonNode.class));
        verify(attestationCertificateService).populateTrustManager(any(AuthData.class), anyList());
        verify(certificateVerifier).verifyAttestationCertificates(anyList(), anyList());
        verify(authenticatorDataVerifier).verifyPackedAttestationSignature(authData.getAuthDataDecoded(), clientDataHash, signature, verifiedCert, alg);
        verify(certificateVerifier).isSelfSigned(any(X509Certificate.class));
        verify(base64Service, times(2)).urlEncodeToString(any());
        verifyNoInteractions(log, coseService);
    }


    @Test
    void process_ifAttStmtHasEcdaaKey_badRequestException() {
        ObjectNode attStmt = instanceMapper().createObjectNode();
        String ecdaaKeyId = "test-ecdaaKeyId";
        attStmt.put("ecdaaKeyId", ecdaaKeyId);
        int alg = -7;
        attStmt.put("alg", alg);
        attStmt.put("sig", "test-signature");
        AuthData authData = new AuthData();
        authData.setKeyType(alg);
        authData.setAuthDataDecoded("test-AuthDataDecoded".getBytes());
        Fido2RegistrationData registration = new Fido2RegistrationData();
        byte[] clientDataHash = "test-clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = new CredAndCounterData();
        when(commonVerifiers.verifyAlgorithm(any(), anyInt())).thenReturn(alg);
        when(commonVerifiers.verifyBase64String(any())).thenReturn("test-signature");
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> packedAttestationProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verify(fido2Configuration).getAttestationMode();
        verify(commonVerifiers).verifyAlgorithm(any(JsonNode.class), any(Integer.class));
        verify(commonVerifiers).verifyBase64String(any(JsonNode.class));

        verifyNoInteractions(attestationCertificateService, certificateVerifier, certificateService, coseService, authenticatorDataVerifier, base64Service);
    }

    @Test
    void process_ifAttStmtIsNotX5cOrEcdaaKey_valid() {
        ObjectNode attStmt = instanceMapper().createObjectNode();
        int alg = -7;
        attStmt.put("alg", alg);
        attStmt.put("sig", "test-signature");
        AuthData authData = new AuthData();
        authData.setKeyType(alg);
        authData.setAuthDataDecoded("test-AuthDataDecoded".getBytes());
        Fido2RegistrationData registration = new Fido2RegistrationData();
        byte[] clientDataHash = "test-clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = new CredAndCounterData();
        String signature = "test-signature";
        when(commonVerifiers.verifyAlgorithm(any(), anyInt())).thenReturn(alg);
        when(commonVerifiers.verifyBase64String(any())).thenReturn(signature);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());

        packedAttestationProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters);
        verify(commonVerifiers).verifyAlgorithm(any(JsonNode.class), any(Integer.class));
        verify(commonVerifiers).verifyBase64String(any(JsonNode.class));
        verify(base64Service, times(2)).urlEncodeToString(any());
        verify(appConfiguration).getFido2Configuration(); // Explicit verification
        verifyNoInteractions(certificateService, log, certificateVerifier);
    }
}
