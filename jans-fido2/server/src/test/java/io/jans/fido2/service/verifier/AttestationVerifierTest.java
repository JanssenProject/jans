package io.jans.fido2.service.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AttestationMode;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.model.conf.AppConfiguration;
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

    @Mock
    private AppConfiguration appConfiguration;

    //Test
    void verifyAuthenticatorAttestationResponse_attestationObjectFieldIsNull_fido2RuntimeException() {
        io.jans.fido2.model.attestation.Response authenticatorResponse = new io.jans.fido2.model.attestation.Response();
        authenticatorResponse.setClientDataJSON("TEST-clientDataJSON");
        Fido2RegistrationData credential = new Fido2RegistrationData();
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationVerifier.verifyAuthenticatorAttestationResponse(authenticatorResponse, credential));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
        verifyNoInteractions(log, base64Service, dataMapperService, commonVerifiers, authenticatorDataParser, attestationProcessorFactory);
    }

    //Test
    void verifyAuthenticatorAttestationResponse_clientDataJSONFieldIsNull_fido2RuntimeException() {
        io.jans.fido2.model.attestation.Response authenticatorResponse = new io.jans.fido2.model.attestation.Response();
        authenticatorResponse.setClientDataJSON("TEST-clientDataJSON");
        authenticatorResponse.setAttestationObject("TEST-attestationObject");

        Fido2RegistrationData credential = new Fido2RegistrationData();
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationVerifier.verifyAuthenticatorAttestationResponse(authenticatorResponse, credential));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
        //verifyNoInteractions(log, base64Service, dataMapperService, commonVerifiers, authenticatorDataParser, attestationProcessorFactory);
    }

    //Test
    void verifyAuthenticatorAttestationResponse_attestationObjectNotBase64_fido2RuntimeException() {
        io.jans.fido2.model.attestation.Response authenticatorResponse = new io.jans.fido2.model.attestation.Response();
        authenticatorResponse.setClientDataJSON("TEST-clientDataJSON");
        authenticatorResponse.setAttestationObject("TEST-attestationObject");

        Fido2RegistrationData credential = new Fido2RegistrationData();
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> attestationVerifier.verifyAuthenticatorAttestationResponse(authenticatorResponse, credential));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
        verifyNoInteractions(log, dataMapperService, commonVerifiers, authenticatorDataParser, attestationProcessorFactory);
    }

    //Test
    void verifyAuthenticatorAttestationResponse_attestationObjectCborNull_fido2RuntimeException() {
        String attestationObjectString = "TEST-attestationObject";
        io.jans.fido2.model.attestation.Response authenticatorResponse = new io.jans.fido2.model.attestation.Response();
        authenticatorResponse.setClientDataJSON("TEST-clientDataJSON");
        authenticatorResponse.setAttestationObject(attestationObjectString);

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

    //Test
    void verifyAuthenticatorAttestationResponse_attestationObjectCborIOException_fido2RuntimeException() throws IOException {
        String attestationObjectString = "TEST-attestationObject";
        io.jans.fido2.model.attestation.Response authenticatorResponse = new io.jans.fido2.model.attestation.Response();
        authenticatorResponse.setClientDataJSON("TEST-clientDataJSON");
        authenticatorResponse.setAttestationObject(attestationObjectString);

        byte[] attestationObjectBytes = attestationObjectString.getBytes();
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

    //Test
    void verifyAuthenticatorAttestationResponse_responseAndCredential_valid() throws IOException {
        String attestationObjectString = "TEST-attestationObject";
        String clientDataJSONString = "TEST-clientDataJSON";
        io.jans.fido2.model.attestation.Response authenticatorResponse = new io.jans.fido2.model.attestation.Response();
        authenticatorResponse.setClientDataJSON(clientDataJSONString);
        authenticatorResponse.setAttestationObject(attestationObjectString);

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

        Fido2Configuration fido2Configuration = new Fido2Configuration(null, null, null,
                null, false, false,
                0, 0, null,
                null, null, null, false,
                AttestationMode.MONITOR.getValue(), null, false);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);


        CredAndCounterData credIdAndCounters = attestationVerifier.verifyAuthenticatorAttestationResponse(authenticatorResponse, credential);
        assertNotNull(credIdAndCounters);
        assertEquals(credIdAndCounters.getCounters(), 0);
        verify(log).debug("Authenticator data {} {}", fmt, authenticatorDataNode);
        verify(base64Service, times(2)).urlDecode(anyString());
    }

    //Test
    void verifyAuthenticatorAttestationResponse_whenAttestationModeIsDisabled() throws IOException {
        String attestationObjectString = "TEST-attestationObject";
        String clientDataJSONString = "TEST-clientDataJSON";
        io.jans.fido2.model.attestation.Response authenticatorResponse = new io.jans.fido2.model.attestation.Response();
        authenticatorResponse.setClientDataJSON(clientDataJSONString);
        authenticatorResponse.setAttestationObject(attestationObjectString);

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

        Fido2Configuration fido2Configuration = new Fido2Configuration(null, null, null,
                null, false, false,
                0, 0, null,
                null, null, null, false,
                AttestationMode.DISABLED.getValue(), null, false);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);

        CredAndCounterData result = attestationVerifier.verifyAuthenticatorAttestationResponse(authenticatorResponse, credential);

        assertNotNull(result);
       // verify(log).warn(eq("SkipValidateMdsInAttestation is enabled"));
    }

    //Test
    void verifyAuthenticatorAttestationResponse_whenAttestationModeIsEnforcedAndFmtNone_shouldThrowException() throws IOException {
        String attestationObjectString = "TEST-attestationObject";
        String clientDataJSONString = "TEST-clientDataJSON";
        io.jans.fido2.model.attestation.Response authenticatorResponse = new io.jans.fido2.model.attestation.Response();
        authenticatorResponse.setClientDataJSON(clientDataJSONString);
        authenticatorResponse.setAttestationObject(attestationObjectString);

        ObjectNode authenticatorDataNode = mapper.createObjectNode();
        authenticatorDataNode.put("attStmt", "TEST-attStmt");
        authenticatorDataNode.put("authData", "TEST-authData");
        Fido2RegistrationData credential = new Fido2RegistrationData();
        AuthData authData = new AuthData();
        authData.setCounters("TEST-counter".getBytes());
        String authDataText = "TEST-authDataText";
        String fmt = "none";
        AttestationFormatProcessor attestationProcessor = mock(AttestationFormatProcessor.class);
        when(base64Service.urlDecode(attestationObjectString)).thenReturn(attestationObjectString.getBytes());
        when(base64Service.urlDecode(clientDataJSONString)).thenReturn(clientDataJSONString.getBytes());
        when(dataMapperService.cborReadTree(any())).thenReturn(authenticatorDataNode);
        when(commonVerifiers.verifyFmt(authenticatorDataNode, "fmt")).thenReturn(fmt);
        when(commonVerifiers.verifyAuthData(any())).thenReturn(authDataText);
        when(authenticatorDataParser.parseAttestationData(authDataText)).thenReturn(authData);
        when(attestationProcessorFactory.getCommandProcessor(fmt)).thenReturn(attestationProcessor);

        Fido2Configuration fido2Configuration = new Fido2Configuration(null, null, null,
                null, false, false,
                0, 0, null,
                null, null, null, false,
                AttestationMode.ENFORCED.getValue(), null, false);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);

        /*Fido2RuntimeException exception = assertThrows(Fido2RuntimeException.class, () ->
                attestationVerifier.verifyAuthenticatorAttestationResponse(authenticatorResponse, credential)
        );
        assertEquals("Unauthorized to perform this action", exception.getMessage());*/
    }

    //Test
    void verifyAuthenticatorAttestationResponse_whenAttestationModeIsEnforcedAndFormatIsNotNone_shouldProcess() throws IOException {
        String attestationObjectString = "TEST-attestationObject";
        String clientDataJSONString = "TEST-clientDataJSON";
        io.jans.fido2.model.attestation.Response authenticatorResponse = new io.jans.fido2.model.attestation.Response();
        authenticatorResponse.setClientDataJSON(clientDataJSONString);
        authenticatorResponse.setAttestationObject(attestationObjectString);

        ObjectNode authenticatorDataNode = mapper.createObjectNode();
        authenticatorDataNode.put("attStmt", "TEST-attStmt");
        authenticatorDataNode.put("authData", "TEST-authData");
        Fido2RegistrationData credential = new Fido2RegistrationData();
        AuthData authData = new AuthData();
        authData.setCounters("TEST-counter".getBytes());
        String authDataText = "TEST-authDataText";
        String fmt = "apple";
        AttestationFormatProcessor attestationProcessor = mock(AttestationFormatProcessor.class);
        when(base64Service.urlDecode(attestationObjectString)).thenReturn(attestationObjectString.getBytes());
        when(base64Service.urlDecode(clientDataJSONString)).thenReturn(clientDataJSONString.getBytes());
        when(dataMapperService.cborReadTree(any())).thenReturn(authenticatorDataNode);
        when(commonVerifiers.verifyFmt(authenticatorDataNode, "fmt")).thenReturn(fmt);
        when(commonVerifiers.verifyAuthData(any())).thenReturn(authDataText);
        when(authenticatorDataParser.parseAttestationData(authDataText)).thenReturn(authData);
        when(attestationProcessorFactory.getCommandProcessor(fmt)).thenReturn(attestationProcessor);

        Fido2Configuration fido2Configuration = new Fido2Configuration(null, null, null,
                null, false, false,
                0, 0, null,
                null, null, null, false,
                AttestationMode.ENFORCED.getValue(), null, false);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        CredAndCounterData result = attestationVerifier.verifyAuthenticatorAttestationResponse(authenticatorResponse, credential);

        assertNotNull(result);
        assertEquals(result.getCounters(), 0);
        verify(base64Service, times(2)).urlDecode(anyString());
    }
}
