package io.jans.fido2.service.verifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


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
import io.jans.fido2.model.assertion.Response;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2RegistrationData;

@ExtendWith(MockitoExtension.class)
class AssertionVerifierTest {

	@InjectMocks 
	private AssertionVerifier assertionVerifier = Mockito.mock(AssertionVerifier.class) ;
	 
	@Mock
	private Logger log;
	
   

    @Test
    void verifyAuthenticatorAssertionResponse_validValues_valid() {
        String authenticatorDataValue = "TEST-authenticatorData";
        String clientDataJSONValue = "TEST-clientDataJSON";
        String signatureValue = "TEST-signature";
        Response response = new Response();
        response.setAuthenticatorData(authenticatorDataValue);
        response.setSignature(signatureValue);
        response.setClientDataJSON(clientDataJSONValue);

        Fido2RegistrationData registration = new Fido2RegistrationData();
        Fido2AuthenticationData authenticationEntity = new Fido2AuthenticationData();
        assertionVerifier.verifyAuthenticatorAssertionResponse(response, registration, authenticationEntity);
        
    }
}
