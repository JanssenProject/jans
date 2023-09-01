package io.jans.fido2.service.processor.attestation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.jans.fido2.google.safetynet.AttestationStatement;
import io.jans.fido2.google.safetynet.OfflineVerify;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import javax.net.ssl.X509TrustManager;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AndroidSafetyNetAttestationProcessorTest {

    @InjectMocks
    private AndroidSafetyNetAttestationProcessor androidSafetyNetAttestationProcessor;

    @Mock
    private Logger log;

    @Mock
    private CommonVerifiers commonVerifiers;

    @Mock
    private AttestationCertificateService attestationCertificateService;

    @Mock
    private Base64Service base64Service;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private OfflineVerify offlineVerify;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    void getAttestationFormat_valid_androidSafetynet() {
        String fmt = androidSafetyNetAttestationProcessor.getAttestationFormat().getFmt();
        assertNotNull(fmt);
        assertEquals(fmt, "android-safetynet");
    }

    @Test
    void process_ifSkipValidateMdsInAttestationEnabledIsTrue_success() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.get("response")).thenReturn(new TextNode("test response"));
        when(authData.getAaguid()).thenReturn("test_aaguid".getBytes());
        when(base64Service.decode(anyString())).thenReturn("test response decoded".getBytes());
        Fido2Configuration fido2Configuration = new Fido2Configuration();
        fido2Configuration.setSkipValidateMdsInAttestationEnabled(true);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(authData.getCredId()).thenReturn("test_cred_id".getBytes());
        when(authData.getCosePublicKey()).thenReturn("test_cose_public_key".getBytes());
        when(base64Service.urlEncodeToString(any(byte[].class))).thenReturn("test_cred_id", "test_uncompressed_ec_point");

        androidSafetyNetAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters);

        verify(commonVerifiers).verifyThatNonEmptyString(any(), eq("ver"));
        verify(log).debug(contains("Android safetynet payload"), any(), any());
        verify(base64Service).decode(anyString());
        verify(appConfiguration).getFido2Configuration();
        verify(log).warn(eq("SkipValidateMdsInAttestation is enabled"));
        verify(base64Service, times(2)).urlEncodeToString(any(byte[].class));
        verifyNoInteractions(attestationCertificateService, offlineVerify, errorResponseFactory);
        verifyNoMoreInteractions(base64Service);
    }

    @Test
    void process_ifStmtIsNull_badRequestException() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.get("response")).thenReturn(new TextNode("test response"));
        when(authData.getAaguid()).thenReturn("test_aaguid".getBytes());
        when(base64Service.decode(anyString())).thenReturn("test response decoded".getBytes());
        Fido2Configuration fido2Configuration = new Fido2Configuration();
        fido2Configuration.setSkipValidateMdsInAttestationEnabled(false);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        X509TrustManager tm = mock(X509TrustManager.class);
        when(attestationCertificateService.populateTrustManager(authData, null)).thenReturn(tm);
        when(offlineVerify.parseAndVerify(anyString(), any())).thenReturn(null);
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> androidSafetyNetAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(commonVerifiers).verifyThatNonEmptyString(any(), eq("ver"));
        verify(base64Service, times(2)).decode(anyString());
        verify(log).debug(contains("Android safetynet payload"), any(), any());
        verify(attestationCertificateService).populateTrustManager(authData, null);
        verify(offlineVerify).parseAndVerify(any(), any());
        verify(errorResponseFactory).badRequestException(any(), eq("Invalid safety net attestation, stmt is null"));
        verifyNoMoreInteractions(log, errorResponseFactory, base64Service);
    }

    @Test
    void process_ifHashedBufferAndNonceAreNotEquals_badRequestException() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.get("response")).thenReturn(new TextNode("test response"));
        when(authData.getAaguid()).thenReturn("test_aaguid".getBytes());
        when(base64Service.decode(anyString())).thenReturn("test response decoded".getBytes());
        Fido2Configuration fido2Configuration = new Fido2Configuration();
        fido2Configuration.setSkipValidateMdsInAttestationEnabled(false);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        X509TrustManager tm = mock(X509TrustManager.class);
        when(attestationCertificateService.populateTrustManager(authData, null)).thenReturn(tm);
        AttestationStatement stmt = mock(AttestationStatement.class);
        when(offlineVerify.parseAndVerify(anyString(), any())).thenReturn(stmt);
        when(authData.getAuthDataDecoded()).thenReturn("authDataDecoded".getBytes());
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> androidSafetyNetAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(commonVerifiers).verifyThatNonEmptyString(any(), eq("ver"));
        verify(base64Service, times(2)).decode(anyString());
        verify(log).debug(contains("Android safetynet payload"), any(), any());
        verify(attestationCertificateService).populateTrustManager(authData, null);
        verify(offlineVerify).parseAndVerify(any(), any());
        verify(errorResponseFactory).badRequestException(any(), eq("Invalid safety net attestation, hashed and nonce are not equals"));
        verifyNoMoreInteractions(log, errorResponseFactory, base64Service);
    }

    @Test
    void process_ifCtsProfileMatchIsFalse_badRequestException() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.get("response")).thenReturn(new TextNode("test response"));
        when(authData.getAaguid()).thenReturn("test_aaguid".getBytes());
        when(base64Service.decode(anyString())).thenReturn("test response decoded".getBytes());
        Fido2Configuration fido2Configuration = new Fido2Configuration();
        fido2Configuration.setSkipValidateMdsInAttestationEnabled(false);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        X509TrustManager tm = mock(X509TrustManager.class);
        when(attestationCertificateService.populateTrustManager(authData, null)).thenReturn(tm);
        AttestationStatement stmt = mock(AttestationStatement.class);
        when(offlineVerify.parseAndVerify(anyString(), any())).thenReturn(stmt);
        when(authData.getAuthDataDecoded()).thenReturn("authDataDecoded".getBytes());
        when(stmt.getNonce()).thenReturn(DigestUtils.getSha256Digest().digest("authDataDecodedtest_clientDataHash".getBytes()));
        when(stmt.isCtsProfileMatch()).thenReturn(false);
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> androidSafetyNetAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(commonVerifiers).verifyThatNonEmptyString(any(), eq("ver"));
        verify(base64Service, times(2)).decode(anyString());
        verify(log).debug(contains("Android safetynet payload"), any(), any());
        verify(attestationCertificateService).populateTrustManager(authData, null);
        verify(offlineVerify).parseAndVerify(any(), any());
        verify(errorResponseFactory).badRequestException(any(), eq("Invalid safety net attestation, cts profile match is false"));
        verifyNoMoreInteractions(log, errorResponseFactory, base64Service);
    }

    @Test
    void process_ifTimestampIsAfterNow_badRequestException() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.get("response")).thenReturn(new TextNode("test response"));
        when(authData.getAaguid()).thenReturn("test_aaguid".getBytes());
        when(base64Service.decode(anyString())).thenReturn("test response decoded".getBytes());
        Fido2Configuration fido2Configuration = new Fido2Configuration();
        fido2Configuration.setSkipValidateMdsInAttestationEnabled(false);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        X509TrustManager tm = mock(X509TrustManager.class);
        when(attestationCertificateService.populateTrustManager(authData, null)).thenReturn(tm);
        AttestationStatement stmt = mock(AttestationStatement.class);
        when(offlineVerify.parseAndVerify(anyString(), any())).thenReturn(stmt);
        when(authData.getAuthDataDecoded()).thenReturn("authDataDecoded".getBytes());
        when(stmt.getNonce()).thenReturn(DigestUtils.getSha256Digest().digest("authDataDecodedtest_clientDataHash".getBytes()));
        when(stmt.isCtsProfileMatch()).thenReturn(true);
        when(stmt.getTimestampMs()).thenReturn(ZonedDateTime.now().plusHours(1).toInstant().toEpochMilli());
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> androidSafetyNetAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(commonVerifiers).verifyThatNonEmptyString(any(), eq("ver"));
        verify(base64Service, times(2)).decode(anyString());
        verify(log).debug(contains("Android safetynet payload"), any(), any());
        verify(attestationCertificateService).populateTrustManager(authData, null);
        verify(offlineVerify).parseAndVerify(any(), any());
        verify(errorResponseFactory).badRequestException(any(), eq("Invalid safety net attestation, timestamp is after now"));
        verifyNoMoreInteractions(log, errorResponseFactory, base64Service);
    }

    @Test
    void process_ifTimestampIsBeforeNowMinus1Minutes_badRequestException() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.get("response")).thenReturn(new TextNode("test response"));
        when(authData.getAaguid()).thenReturn("test_aaguid".getBytes());
        when(base64Service.decode(anyString())).thenReturn("test response decoded".getBytes());
        Fido2Configuration fido2Configuration = new Fido2Configuration();
        fido2Configuration.setSkipValidateMdsInAttestationEnabled(false);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        X509TrustManager tm = mock(X509TrustManager.class);
        when(attestationCertificateService.populateTrustManager(authData, null)).thenReturn(tm);
        AttestationStatement stmt = mock(AttestationStatement.class);
        when(offlineVerify.parseAndVerify(anyString(), any())).thenReturn(stmt);
        when(authData.getAuthDataDecoded()).thenReturn("authDataDecoded".getBytes());
        when(stmt.getNonce()).thenReturn(DigestUtils.getSha256Digest().digest("authDataDecodedtest_clientDataHash".getBytes()));
        when(stmt.isCtsProfileMatch()).thenReturn(true);
        when(stmt.getTimestampMs()).thenReturn(ZonedDateTime.now().minusHours(1).toInstant().toEpochMilli());
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> androidSafetyNetAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(commonVerifiers).verifyThatNonEmptyString(any(), eq("ver"));
        verify(base64Service, times(2)).decode(anyString());
        verify(log).debug(contains("Android safetynet payload"), any(), any());
        verify(attestationCertificateService).populateTrustManager(authData, null);
        verify(offlineVerify).parseAndVerify(any(), any());
        verify(errorResponseFactory).badRequestException(any(), eq("Invalid safety net attestation, timestamp is before now minus 1 minutes"));
        verifyNoMoreInteractions(log, errorResponseFactory, base64Service);
    }

    @Test
    void process_validData_success() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.get("response")).thenReturn(new TextNode("test response"));
        when(authData.getAaguid()).thenReturn("test_aaguid".getBytes());
        when(base64Service.decode(anyString())).thenReturn("test response decoded".getBytes());
        Fido2Configuration fido2Configuration = new Fido2Configuration();
        fido2Configuration.setSkipValidateMdsInAttestationEnabled(false);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        X509TrustManager tm = mock(X509TrustManager.class);
        when(attestationCertificateService.populateTrustManager(authData, null)).thenReturn(tm);
        AttestationStatement stmt = mock(AttestationStatement.class);
        when(offlineVerify.parseAndVerify(anyString(), any())).thenReturn(stmt);
        when(authData.getAuthDataDecoded()).thenReturn("authDataDecoded".getBytes());
        when(stmt.getNonce()).thenReturn(DigestUtils.getSha256Digest().digest("authDataDecodedtest_clientDataHash".getBytes()));
        when(stmt.isCtsProfileMatch()).thenReturn(true);
        when(stmt.getTimestampMs()).thenReturn(ZonedDateTime.now().toInstant().toEpochMilli());
        when(authData.getCredId()).thenReturn("test_cred_id".getBytes());
        when(authData.getCosePublicKey()).thenReturn("test_cose_public_key".getBytes());
        when(base64Service.urlEncodeToString(any(byte[].class))).thenReturn("test_cred_id", "test_uncompressed_ec_point");

        androidSafetyNetAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters);

        verify(commonVerifiers).verifyThatNonEmptyString(any(), eq("ver"));
        verify(base64Service, times(2)).decode(anyString());
        verify(log).debug(contains("Android safetynet payload"), any(), any());
        verify(attestationCertificateService).populateTrustManager(authData, null);
        verify(offlineVerify).parseAndVerify(any(), any());
        verify(base64Service, times(2)).urlEncodeToString(any(byte[].class));
        verifyNoMoreInteractions(log);
        verifyNoInteractions(errorResponseFactory);
    }
}
