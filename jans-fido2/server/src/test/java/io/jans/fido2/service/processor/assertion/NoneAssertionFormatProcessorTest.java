package io.jans.fido2.service.processor.assertion;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.fido2.exception.Fido2CompromisedDevice;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.service.AuthenticatorDataParser;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.verifier.AuthenticatorDataVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.UserVerificationVerifier;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.orm.model.fido2.UserVerification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoneAssertionFormatProcessorTest {

    @InjectMocks
    private NoneAssertionFormatProcessor noneAssertionFormatProcessor;

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

    @Test
    void getAttestationFormat_valid_none() {
        String fmt = noneAssertionFormatProcessor.getAttestationFormat().getFmt();
        assertNotNull(fmt);
        assertEquals(fmt, "none");
    }

    @Test
    void process_validData_success() throws IOException {
        String base64AuthenticatorData = "base64AuthenticatorData_test";
        String signature = "signature_test";
        String clientDataJson = "clientDataJson_test";
        Fido2RegistrationData registration = mock(Fido2RegistrationData.class);
        Fido2AuthenticationData authenticationEntity = mock(Fido2AuthenticationData.class);

        when(authenticatorDataParser.parseAssertionData(any())).thenReturn(mock(AuthData.class));
        when(base64Service.urlDecode(any(String.class))).thenReturn("decode_test".getBytes());
        when(dataMapperService.cborReadTree(any())).thenReturn(mock(JsonNode.class));
        PublicKey publicKey = mock(PublicKey.class);
        when(coseService.createUncompressedPointFromCOSEPublicKey(any())).thenReturn(publicKey);
        when(publicKey.getEncoded()).thenReturn("test".getBytes());
        when(authenticationEntity.getUserVerificationOption()).thenReturn(UserVerification.preferred);
        when(registration.getDomain()).thenReturn("domain_test");

        noneAssertionFormatProcessor.process(base64AuthenticatorData, signature, clientDataJson, registration, authenticationEntity);

        verify(log).debug(eq("Registration: {}"), any(Fido2RegistrationData.class));
        verify(log).debug(eq("User verification option: {}"), any(UserVerification.class));
        verify(commonVerifiers).verifyRpIdHash(any(AuthData.class), any(String.class));
        verify(log).debug(eq("Uncompressed ECpoint node: {}"), any(JsonNode.class));
        verify(log).debug(eq("EC Public key hex: {}"), any(String.class));
        verify(log).debug(eq("Registration algorithm: {}, default use: -7"), any(Integer.class));
        verify(userVerificationVerifier).verifyUserVerificationOption(any(UserVerification.class), any(AuthData.class));
        verify(authenticatorDataVerifier).verifyAssertionSignature(any(AuthData.class), any(byte[].class), any(String.class), any(PublicKey.class), any(Integer.class));

        verify(log, never()).error(eq("Error compromised device: {}"), any(String.class));
        verify(log, never()).error(eq("Error to check none assertion: {}"), any(String.class));
        verifyNoMoreInteractions(log);
    }

    @Test
    void process_ifVerifyCounterIsThrowException_fido2CompromisedDevice() throws Fido2CompromisedDevice {
        String base64AuthenticatorData = "base64AuthenticatorData_test";
        String signature = "signature_test";
        String clientDataJson = "clientDataJson_test";
        Fido2RegistrationData registration = mock(Fido2RegistrationData.class);
        Fido2AuthenticationData authenticationEntity = mock(Fido2AuthenticationData.class);

        when(authenticationEntity.getUserVerificationOption()).thenReturn(UserVerification.preferred);
        when(registration.getDomain()).thenReturn("domain_test");
        when(registration.getCounter()).thenReturn(100);

        when(authenticatorDataParser.parseAssertionData(any())).thenReturn(mock(AuthData.class));
        when(base64Service.urlDecode(any(String.class))).thenReturn("decode_test".getBytes());
        doThrow(new Fido2CompromisedDevice("fido2CompromisedDevice_testError")).when(commonVerifiers).verifyCounter(any(Integer.class), any(Integer.class));

        Fido2CompromisedDevice ex = assertThrows(Fido2CompromisedDevice.class, () -> noneAssertionFormatProcessor.process(base64AuthenticatorData, signature, clientDataJson, registration, authenticationEntity));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "fido2CompromisedDevice_testError");

        verify(log).debug(eq("Registration: {}"), any(Fido2RegistrationData.class));
        verify(log).debug(eq("User verification option: {}"), any(UserVerification.class));
        verify(commonVerifiers).verifyRpIdHash(any(AuthData.class), any(String.class));
        verify(authenticatorDataParser).parseCounter(any());
        verify(log).error(eq("Error compromised device: {}"), any(String.class));

        verify(log, never()).debug(eq("Registration algorithm: {}, default use: -7"), any(Integer.class));
        verify(log, never()).error(eq("Error to check none assertion: {}"), any(String.class));
        verifyNoInteractions(authenticatorDataVerifier);
        verifyNoMoreInteractions(log);
    }

    @Test
    void process_ifCborReadTreeThrowException_fido2RuntimeException() throws Fido2CompromisedDevice, IOException {
        String base64AuthenticatorData = "base64AuthenticatorData_test";
        String signature = "signature_test";
        String clientDataJson = "clientDataJson_test";
        Fido2RegistrationData registration = mock(Fido2RegistrationData.class);
        Fido2AuthenticationData authenticationEntity = mock(Fido2AuthenticationData.class);

        when(authenticationEntity.getUserVerificationOption()).thenReturn(UserVerification.preferred);
        when(registration.getDomain()).thenReturn("domain_test");
        when(registration.getCounter()).thenReturn(100);
        when(registration.getUncompressedECPoint()).thenReturn("uncompressedECPoint_test");

        when(authenticatorDataParser.parseAssertionData(any())).thenReturn(mock(AuthData.class));
        when(base64Service.urlDecode(any(String.class))).thenReturn("decode_test".getBytes());
        when(dataMapperService.cborReadTree(any(byte[].class))).thenThrow(new IOException("IOException_test"));

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> noneAssertionFormatProcessor.process(base64AuthenticatorData, signature, clientDataJson, registration, authenticationEntity));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "IOException_test");

        verify(log).debug(eq("Registration: {}"), any(Fido2RegistrationData.class));
        verify(log).debug(eq("User verification option: {}"), any(UserVerification.class));
        verify(commonVerifiers).verifyRpIdHash(any(AuthData.class), any(String.class));
        verify(authenticatorDataParser).parseCounter(any());
        verify(log).error(eq("Error to check none assertion: {}"), any(String.class));

        verify(log, never()).error(eq("Error compromised device: {}"), any(String.class));
        verifyNoInteractions(coseService, authenticatorDataVerifier);
        verifyNoMoreInteractions(log);
    }
}
