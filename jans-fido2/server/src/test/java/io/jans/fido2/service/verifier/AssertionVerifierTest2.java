package io.jans.fido2.service.verifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;



import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.assertion.Response;
import io.jans.fido2.service.AuthenticatorDataParser;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.util.DigestUtilService;
import io.jans.fido2.service.util.HexUtilService;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.inject.Inject;

@ExtendWith(MockitoExtension.class)
class AssertionVerifierTest2 {

    

	
	@InjectMocks 
	private AssertionVerifier assertionVerifier ;
	 
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

	@Mock
	private Logger log;
	
    @Test
    void verifyAuthenticatorAssertionResponse_authenticatorDataIsNull_fido2RuntimeException() {
        Response response = new Response();
        response.setClientDataJSON("TEST-clientDataJSON");
        response.setSignature("TEST-signature");
        Fido2RegistrationData registration = new Fido2RegistrationData();
        Fido2AuthenticationData authenticationEntity = new Fido2AuthenticationData();

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> assertionVerifier.verifyAuthenticatorAssertionResponse(response, registration, authenticationEntity));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Authenticator data is invalid");
        
    }

    @Test
    void verifyAuthenticatorAssertionResponse_clientDataJSONIsNull_fido2RuntimeException() {
        Response response = new Response();
        response.setAuthenticatorData("TEST-authenticatorData");
        response.setSignature("TEST-signature");

        Fido2RegistrationData registration = new Fido2RegistrationData();
        Fido2AuthenticationData authenticationEntity = new Fido2AuthenticationData();

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> assertionVerifier.verifyAuthenticatorAssertionResponse(response, registration, authenticationEntity));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Authenticator data is invalid");
        
    }

    @Test
    void verifyAuthenticatorAssertionResponse_signatureIsNull_fido2RuntimeException() {
        Response response = new Response();
        response.setAuthenticatorData("TEST-authenticatorData");
        response.setSignature("TEST-signature");
        Fido2RegistrationData registration = new Fido2RegistrationData();
        Fido2AuthenticationData authenticationEntity = new Fido2AuthenticationData();

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> assertionVerifier.verifyAuthenticatorAssertionResponse(response, registration, authenticationEntity));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Authenticator data is invalid");
        
    }

  
}
