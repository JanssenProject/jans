package io.jans.fido2.service.processor.attestation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.jans.fido2.google.safetynet.AttestationStatement;
import io.jans.fido2.google.safetynet.OfflineVerify;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
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

    @Test
    void process_ifParseAndVerifyThrownError_badRequestException() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.get("response")).thenReturn(new TextNode("response"));
        when(authData.getAaguid()).thenReturn("aaguid".getBytes());
        when(base64Service.decode("response")).thenReturn("test_decode".getBytes());
        when(offlineVerify.parseAndVerify(anyString(), any())).thenThrow(new RuntimeException("test exception"));
        WebApplicationException e = new WebApplicationException(Response.status(400).entity("test exception").build());
        when(errorResponseFactory.badRequestException(any(), any())).thenThrow(e);

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> androidSafetyNetAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(commonVerifiers).verifyThatNonEmptyString(any(), eq("ver"));
        verify(log).debug(contains("Android safetynet payload"), any(), any());
        verify(log).error(contains("Error on parse and verify"), any(), any());
        verify(errorResponseFactory).badRequestException(any(), contains("Invalid safety net attestation "));
        verifyNoMoreInteractions(errorResponseFactory, base64Service);
    }

    @Test
    void process_ifStmtIsNull_badRequestException() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test_clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.get("response")).thenReturn(new TextNode("response"));
        when(authData.getAaguid()).thenReturn("aaguid".getBytes());
        when(base64Service.decode("response")).thenReturn("test_decode".getBytes());
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
        verify(log).debug(contains("Android safetynet payload"), any(), any());
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

        when(attStmt.get("response")).thenReturn(new TextNode("response"));
        when(authData.getAaguid()).thenReturn("aaguid".getBytes());
        when(authData.getAuthDataDecoded()).thenReturn("authDataDecoded".getBytes());
        when(base64Service.decode("response")).thenReturn("test_decode".getBytes());
        X509TrustManager tm = mock(X509TrustManager.class);
        when(attestationCertificateService.populateTrustManager(authData, null)).thenReturn(tm);
        AttestationStatement stmt = mock(AttestationStatement.class);
        when(offlineVerify.parseAndVerify(anyString(), any())).thenReturn(stmt);
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> androidSafetyNetAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(commonVerifiers).verifyThatNonEmptyString(any(), eq("ver"));
        verify(log).debug(contains("Android safetynet payload"), any(), any());
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

        when(attStmt.get("response")).thenReturn(new TextNode("response"));
        when(authData.getAaguid()).thenReturn("aaguid".getBytes());
        when(authData.getAuthDataDecoded()).thenReturn("authDataDecoded".getBytes());
        when(base64Service.decode("response")).thenReturn("test_decode".getBytes());
        X509TrustManager tm = mock(X509TrustManager.class);
        when(attestationCertificateService.populateTrustManager(authData, null)).thenReturn(tm);
        AttestationStatement stmt = mock(AttestationStatement.class);
        when(offlineVerify.parseAndVerify(anyString(), any())).thenReturn(stmt);
        when(stmt.getNonce()).thenReturn(DigestUtils.getSha256Digest().digest("authDataDecodedtest_clientDataHash".getBytes()));
        when(stmt.isCtsProfileMatch()).thenReturn(false);
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> androidSafetyNetAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(commonVerifiers).verifyThatNonEmptyString(any(), eq("ver"));
        verify(log).debug(contains("Android safetynet payload"), any(), any());
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

        when(attStmt.get("response")).thenReturn(new TextNode("response"));
        when(authData.getAaguid()).thenReturn("aaguid".getBytes());
        when(authData.getAuthDataDecoded()).thenReturn("authDataDecoded".getBytes());
        when(base64Service.decode("response")).thenReturn("test_decode".getBytes());
        X509TrustManager tm = mock(X509TrustManager.class);
        when(attestationCertificateService.populateTrustManager(authData, null)).thenReturn(tm);
        AttestationStatement stmt = mock(AttestationStatement.class);
        when(offlineVerify.parseAndVerify(anyString(), any())).thenReturn(stmt);
        when(stmt.getNonce()).thenReturn(DigestUtils.getSha256Digest().digest("authDataDecodedtest_clientDataHash".getBytes()));
        when(stmt.isCtsProfileMatch()).thenReturn(true);
        when(stmt.getTimestampMs()).thenReturn(ZonedDateTime.now().plusHours(1).toInstant().toEpochMilli());
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> androidSafetyNetAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(log).debug(contains("Android safetynet payload"), any(), any());
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

        when(attStmt.get("response")).thenReturn(new TextNode("response"));
        when(authData.getAaguid()).thenReturn("aaguid".getBytes());
        when(authData.getAuthDataDecoded()).thenReturn("authDataDecoded".getBytes());
        when(base64Service.decode("response")).thenReturn("test_decode".getBytes());
        X509TrustManager tm = mock(X509TrustManager.class);
        when(attestationCertificateService.populateTrustManager(authData, null)).thenReturn(tm);
        AttestationStatement stmt = mock(AttestationStatement.class);
        when(offlineVerify.parseAndVerify(anyString(), any())).thenReturn(stmt);
        when(stmt.getNonce()).thenReturn(DigestUtils.getSha256Digest().digest("authDataDecodedtest_clientDataHash".getBytes()));
        when(stmt.isCtsProfileMatch()).thenReturn(true);
        when(stmt.getTimestampMs()).thenReturn(ZonedDateTime.now().minusHours(1).toInstant().toEpochMilli());
        when(errorResponseFactory.badRequestException(any(), anyString())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> androidSafetyNetAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(log).debug(contains("Android safetynet payload"), any(), any());
        verify(errorResponseFactory).badRequestException(any(), eq("Invalid safety net attestation, timestamp is before now minus 1 minutes"));
        verifyNoMoreInteractions(log, errorResponseFactory, base64Service);
    }
}