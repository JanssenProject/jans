package io.jans.fido2.service.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.AuthenticatorDataParser;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.processor.attestation.AttestationProcessorFactory;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttestationVerifierTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private AttestationVerifier attestationVerifier;

    @Mock
    private Logger log;

    @Mock
    private CommonVerifiers commonVerifiers;

    @Mock
    private AuthenticatorDataParser authenticatorDataParser;

    @Mock
    private Base64Service base64Service;

    @Mock
    private DataMapperService dataMapperService;

    @Mock
    private AttestationProcessorFactory attestationProcessorFactory;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void verifyAuthenticatorAttestationResponse_attestationObjectFieldIsNull_fido2RuntimeException() {
        ObjectNode authenticatorResponse = mapper.createObjectNode();
        authenticatorResponse.put("clientDataJSON", "TEST-clientDataJSON");
        Fido2RegistrationData credential = new Fido2RegistrationData();
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationVerifier.verifyAuthenticatorAttestationResponse(authenticatorResponse, credential));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
        verifyNoInteractions(log, base64Service, dataMapperService, commonVerifiers, authenticatorDataParser, attestationProcessorFactory);
    }

    @Test
    void verifyAuthenticatorAttestationResponse_clientDataJSONFieldIsNull_fido2RuntimeException() {
        ObjectNode authenticatorResponse = mapper.createObjectNode();
        authenticatorResponse.put("attestationObject", "TEST-attestationObject");
        Fido2RegistrationData credential = new Fido2RegistrationData();
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationVerifier.verifyAuthenticatorAttestationResponse(authenticatorResponse, credential));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
        verifyNoInteractions(log, base64Service, dataMapperService, commonVerifiers, authenticatorDataParser, attestationProcessorFactory);
    }

    @Test
    void verifyAuthenticatorAttestationResponse_attestationObjectNotBase64_fido2RuntimeException() {
        ObjectNode authenticatorResponse = mapper.createObjectNode();
        authenticatorResponse.put("attestationObject", "TEST-attestationObject");
        authenticatorResponse.put("clientDataJSON", "TEST-clientDataJSON");
        Fido2RegistrationData credential = new Fido2RegistrationData();
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationVerifier.verifyAuthenticatorAttestationResponse(authenticatorResponse, credential));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
        verifyNoInteractions(log, dataMapperService, commonVerifiers, authenticatorDataParser, attestationProcessorFactory);
    }

    @Test
    void verifyAuthenticatorAttestationResponse_attestationObjectCborNull_fido2RuntimeException() {
        ObjectNode authenticatorResponse = mapper.createObjectNode();
        String attestationObjectString = "TEST-attestationObject";
        authenticatorResponse.put("attestationObject", attestationObjectString);
        authenticatorResponse.put("clientDataJSON", "TEST-clientDataJSON");
        Fido2RegistrationData credential = new Fido2RegistrationData();
        when(base64Service.urlDecode(attestationObjectString)).thenReturn(attestationObjectString.getBytes());
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationVerifier.verifyAuthenticatorAttestationResponse(authenticatorResponse, credential));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
        verifyNoInteractions(log, commonVerifiers, authenticatorDataParser, attestationProcessorFactory);
    }

    @Test
    void verifyAuthenticatorAttestationResponse_attestationObjectCborIOException_fido2RuntimeException() throws IOException {
        ObjectNode authenticatorResponse = mapper.createObjectNode();
        String attestationObjectString = "TEST-attestationObject";
        byte[] attestationObjectBytes = attestationObjectString.getBytes();
        authenticatorResponse.put("attestationObject", "TEST-attestationObject");
        authenticatorResponse.put("clientDataJSON", "TEST-clientDataJSON");
        Fido2RegistrationData credential = new Fido2RegistrationData();
        when(base64Service.urlDecode(attestationObjectString)).thenReturn(attestationObjectBytes);
        when(dataMapperService.cborReadTree(attestationObjectBytes)).thenThrow(mock(IOException.class));
        when(errorResponseFactory.invalidRequest(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationVerifier.verifyAuthenticatorAttestationResponse(authenticatorResponse, credential));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
        verifyNoInteractions(log, commonVerifiers, authenticatorDataParser, attestationProcessorFactory);
    }

    @Test
    void verifyAuthenticatorAttestationResponse_responseAndCredential_valid() throws IOException {
        ObjectNode authenticatorResponse = mapper.createObjectNode();
        String attestationObjectString = "TEST-attestationObject";
        String clientDataJSONString = "TEST-clientDataJSON";
        authenticatorResponse.put("attestationObject", attestationObjectString);
        authenticatorResponse.put("clientDataJSON", clientDataJSONString);
        ObjectNode authenticatorDataNode = mapper.createObjectNode();
        authenticatorDataNode.put("attStmt", "TEST-attStmt");
        authenticatorDataNode.put("authData", "TEST-authData");
        Fido2RegistrationData credential = new Fido2RegistrationData();
        AuthData authData = new AuthData();
        authData.setCounters("TEST-counter".getBytes());
        String authDataText = "TEST-authDataText";
        String fmt = "TEST-fmt";
        AttestationFormatProcessor attestationProcessor = mock(AttestationFormatProcessor.class);
        when(base64Service.urlDecode(attestationObjectString)).thenReturn(attestationObjectString.getBytes());
        when(base64Service.urlDecode(clientDataJSONString)).thenReturn(clientDataJSONString.getBytes());
        when(dataMapperService.cborReadTree(any())).thenReturn(authenticatorDataNode);
        when(commonVerifiers.verifyFmt(authenticatorDataNode, "fmt")).thenReturn(fmt);
        when(commonVerifiers.verifyAuthData(any())).thenReturn(authDataText);
        when(authenticatorDataParser.parseAttestationData(authDataText)).thenReturn(authData);
        when(attestationProcessorFactory.getCommandProcessor(fmt)).thenReturn(attestationProcessor);

        CredAndCounterData credIdAndCounters = attestationVerifier.verifyAuthenticatorAttestationResponse(authenticatorResponse, credential);
        assertNotNull(credIdAndCounters);
        assertEquals(credIdAndCounters.getCounters(), 0);
        verify(log).debug("Authenticator data {} {}", fmt, authenticatorDataNode);
        verify(base64Service, times(2)).urlDecode(anyString());
    }
}
