package io.jans.fido2.service.processor.assertion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;


import io.jans.fido2.exception.Fido2CompromisedDevice;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.service.AuthenticatorDataParser;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.util.DigestUtilService;
import io.jans.fido2.service.util.HexUtilService;
import io.jans.fido2.service.verifier.AssertionVerifier;
import io.jans.fido2.service.verifier.AuthenticatorDataVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.UserVerificationVerifier;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import io.jans.orm.model.fido2.UserVerification;

@ExtendWith(MockitoExtension.class)
class AssertionFormatProcessorTest {

	@InjectMocks
	private AssertionVerifier appleAssertionFormatProcessor ;

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
    void process_ifVerifyCounterThrowError_fido2CompromisedDevice() {
        String base64AuthenticatorData = "base64AuthenticatorData_test";
        String signature = "signature_test";
        String clientDataJson = "clientDataJson_test";
        Fido2RegistrationData registration = mock(Fido2RegistrationData.class);
        Fido2AuthenticationData authenticationEntity = mock(Fido2AuthenticationData.class);

        when(authenticatorDataParser.parseAssertionData(any(String.class))).thenReturn(mock(AuthData.class));
        when(authenticationEntity.getUserVerificationOption()).thenReturn(UserVerification.discouraged);
        doThrow(new Fido2CompromisedDevice("Fido2 Exception")).when(commonVerifiers).verifyCounter(anyInt(), anyInt());

        Fido2CompromisedDevice ex = assertThrows(Fido2CompromisedDevice.class, () -> appleAssertionFormatProcessor.process(base64AuthenticatorData, signature, clientDataJson, registration, authenticationEntity));
        assertNotNull(ex);
        assertEquals("Fido2 Exception", ex.getMessage());

        verify(authenticatorDataVerifier).verifyAssertionSignature(any(), any(), any(), any(), anyInt());
        verifyNoInteractions(hexUtilService);
    }

    @Test
    void process_ifCborReadTreeThrowError_fido2RuntimeException() throws IOException {
        String base64AuthenticatorData = "base64AuthenticatorData_test";
        String signature = "signature_test";
        String clientDataJson = "clientDataJson_test";
        Fido2RegistrationData registration = mock(Fido2RegistrationData.class);
        Fido2AuthenticationData authenticationEntity = mock(Fido2AuthenticationData.class);

        when(authenticatorDataParser.parseAssertionData(any(String.class))).thenReturn(mock(AuthData.class));
        when(authenticationEntity.getUserVerificationOption()).thenReturn(UserVerification.discouraged);
        when(dataMapperService.cborReadTree(any())).thenThrow(new IOException("IOException test"));

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> appleAssertionFormatProcessor.process(base64AuthenticatorData, signature, clientDataJson, registration, authenticationEntity));
        assertNotNull(ex);
        assertEquals("Failed to check assertion", ex.getMessage());

        verifyNoInteractions(coseService, hexUtilService, authenticatorDataVerifier);
    }
    
    
}
