package io.jans.fido2.service.processor.assertion;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.service.AuthenticatorDataParser;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.util.DigestUtilService;
import io.jans.fido2.service.util.HexUtilService;
import io.jans.fido2.service.verifier.AuthenticatorDataVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.UserVerificationVerifier;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class U2FAssertionFormatProcessorTest {

    @InjectMocks
    private U2FAssertionFormatProcessor u2FAssertionFormatProcessor;

    @Mock
    private Logger log;

    @Mock
    private CoseService coseService;

    @Mock
    private CommonVerifiers commonVerifiers;

    @Mock
    private AuthenticatorDataVerifier authenticatorDataVerifier;

    @Mock
    private UserVerificationVerifier userVerificationVerifier;

    @Mock
    private AuthenticatorDataParser authenticatorDataParser;

    @Mock
    private DataMapperService dataMapperService;

    @Mock
    private Base64Service base64Service;

    @Mock
    private DigestUtilService digestUtilService;

    @Mock
    private HexUtilService hexUtilService;

    @Test
    void getAttestationFormat_valid_u2f() {
        String fmt = u2FAssertionFormatProcessor.getAttestationFormat().getFmt();
        assertNotNull(fmt);
        assertEquals(fmt, "fido-u2f");
    }

    @Test
    void process_happyPath_success() throws IOException {
        String base64AuthenticatorData = "base64AuthenticatorData_test";
        String signature = "signature_test";
        String clientDataJson = "clientDataJson_test";
        Fido2RegistrationData registration = mock(Fido2RegistrationData.class);
        Fido2AuthenticationData authenticationEntity = mock(Fido2AuthenticationData.class);

        when(registration.getSignatureAlgorithm()).thenReturn(-1);
        when(registration.getCounter()).thenReturn(1);

        AuthData authData = mock(AuthData.class);
        when(authenticatorDataParser.parseAssertionData(base64AuthenticatorData)).thenReturn(authData);
        when(dataMapperService.cborReadTree(any())).thenReturn(mock(JsonNode.class));
        when(coseService.createUncompressedPointFromCOSEPublicKey(any())).thenReturn(mock(PublicKey.class));
        when(hexUtilService.encodeHexString(any())).thenReturn("publicKeyEncoded_test");

        u2FAssertionFormatProcessor.process(base64AuthenticatorData, signature, clientDataJson, registration, authenticationEntity);

        verify(base64Service).urlDecode(anyString());
        verify(userVerificationVerifier).verifyUserPresent(any(AuthData.class));
        verify(digestUtilService).sha256Digest(any());
        verify(commonVerifiers).verifyCounter(eq(1), eq(0));
        verify(log).debug(eq("Uncompressed ECpoint node {}"), any(JsonNode.class));
        verify(log).debug(eq("Public key hex {}"), eq("publicKeyEncoded_test"));
        verify(authenticatorDataVerifier).verifyAssertionSignature(any(), any(), any(), any(), eq(-1));
    }

    @Test
    void process_ifCborReadTreeThrownError_fido2RuntimeException() throws IOException {
        String base64AuthenticatorData = "base64AuthenticatorData_test";
        String signature = "signature_test";
        String clientDataJson = "clientDataJson_test";
        Fido2RegistrationData registration = mock(Fido2RegistrationData.class);
        Fido2AuthenticationData authenticationEntity = mock(Fido2AuthenticationData.class);

        when(registration.getCounter()).thenReturn(1);

        when(authenticatorDataParser.parseAssertionData(base64AuthenticatorData)).thenReturn(mock(AuthData.class));
        when(dataMapperService.cborReadTree(any())).thenReturn(mock(JsonNode.class));
        when(dataMapperService.cborReadTree(any())).thenThrow(new IOException("IOException_test"));

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> u2FAssertionFormatProcessor.process(base64AuthenticatorData, signature, clientDataJson, registration, authenticationEntity));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Failed to check U2F assertion");

        verify(base64Service).urlDecode(anyString());
        verify(userVerificationVerifier).verifyUserPresent(any(AuthData.class));
        verify(digestUtilService).sha256Digest(any());
        verify(commonVerifiers).verifyCounter(eq(1), eq(0));

        verifyNoInteractions(coseService, hexUtilService, authenticatorDataVerifier);
    }
}
