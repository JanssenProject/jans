package io.jans.fido2.service.sg.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.jans.as.model.fido.u2f.message.RawAuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.AuthenticateResponse;
import io.jans.fido2.model.assertion.AssertionOptions;
import io.jans.fido2.model.assertion.AssertionOptionsResponse;
import io.jans.fido2.model.assertion.AssertionResult;
import io.jans.fido2.model.common.AttestationOrAssertionResponse;
import io.jans.fido2.model.common.PublicKeyCredentialDescriptor;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.DigestService;
import io.jans.fido2.service.operation.AssertionService;
import io.jans.fido2.service.persist.UserSessionIdService;
import io.jans.fido2.service.sg.RawAuthenticationService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssertionSuperGluuControllerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private AssertionSuperGluuController assertionSuperGluuController;

    @Mock
    private Logger log;

    @Mock
    private AssertionService assertionService;

    @Mock
    private DataMapperService dataMapperService;

    @Mock
    private Base64Service base64Service;

    @Mock
    private RawAuthenticationService rawAuthenticationService;

    @Mock
    private DigestService digestService;

    @Mock
    private UserSessionIdService userSessionIdService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void startAuthentication_ifAllowCredentialsIsNull_valid() {
        String username = "test_username";
        String keyHandle = "test_key_handle";
        String appId = "test_app_id";
        String sessionId = "session_id";

        AssertionOptionsResponse assertionOptionsResponse =new AssertionOptionsResponse();
        assertionOptionsResponse.setChallenge("test_challenge");
        assertionOptionsResponse.setUserVerification("test_user_verification");

        when(assertionService.options(any())).thenReturn(assertionOptionsResponse);
        when(dataMapperService.createObjectNode()).thenReturn(mapper.createObjectNode(), mapper.createObjectNode());
        when(userSessionIdService.isValidSessionId(sessionId, username)).thenReturn(true);

        JsonNode response = assertionSuperGluuController.startAuthentication(username, keyHandle, appId, sessionId);
        assertNotNull(response);
        assertTrue(response.has("authenticateRequests"));
        JsonNode authenticateRequestsNode = response.get("authenticateRequests");
        assertTrue(authenticateRequestsNode.isEmpty());

        verify(assertionService, only()).options(any());
        verify(dataMapperService, times(1)).createObjectNode();
    }

    @Test
    void startAuthentication_ifAllowCredentialsContainsValue_valid() {
        String username = "test_username";
        String keyHandle = "test_key_handle";
        String appId = "test_app_id";
        String sessionId = "test_session_id";

        AssertionOptionsResponse assertionOptionsResponse =new AssertionOptionsResponse();
        assertionOptionsResponse.setChallenge("test_challenge");
        assertionOptionsResponse.setUserVerification("test_user_verification");

        String transports[] = new String[] { "internal" };
        PublicKeyCredentialDescriptor publicKeyCredentialDescriptor = new PublicKeyCredentialDescriptor(transports, "test_id_1");
        List<PublicKeyCredentialDescriptor> allowCredentials = Lists.newArrayList(publicKeyCredentialDescriptor);

        assertionOptionsResponse.setAllowCredentials(allowCredentials);

        when(assertionService.options(any())).thenReturn(assertionOptionsResponse);
        when(dataMapperService.createObjectNode()).thenReturn(mapper.createObjectNode(), mapper.createObjectNode());
        when(userSessionIdService.isValidSessionId(sessionId, username)).thenReturn(true);

        JsonNode response = assertionSuperGluuController.startAuthentication(username, keyHandle, appId, sessionId);
        assertNotNull(response);
        assertTrue(response.has("authenticateRequests"));
        JsonNode authenticateRequestsArray = response.get("authenticateRequests");
        for (JsonNode itemNode : authenticateRequestsArray) {
            assertTrue(itemNode.has("appId"));
            assertTrue(itemNode.has("userVerification"));
            assertTrue(itemNode.has("challenge"));
            assertTrue(itemNode.has("keyHandle"));
            assertTrue(itemNode.has("version"));
            assertEquals(itemNode.get("appId").asText(), appId);
            assertEquals(itemNode.get("userVerification").asText(), "test_user_verification");
            assertEquals(itemNode.get("challenge").asText(), "test_challenge");
            assertEquals(itemNode.get("keyHandle").asText(), "test_id_1");
            assertEquals(itemNode.get("version").asText(), "U2F_V2");
        }

        verify(assertionService, only()).options(any());
        verify(dataMapperService, times(2)).createObjectNode();
    }

    @Test
    void buildFido2AssertionStartResponse_ifValidIsFalse_webApplicationException() {
        String username = "test_username";
        String keyHandle = "test_key_handle";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        when(userSessionIdService.isValidSessionId(sessionId, username)).thenReturn(false);
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionSuperGluuController.buildFido2AssertionStartResponse(username, keyHandle, appId, sessionId));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(userSessionIdService).isValidSessionId(sessionId, username);
        verifyNoInteractions(dataMapperService, log);
    }

    @Test
    void buildFido2AssertionStartResponse_ifUsernameAndKeyHandleIsEmpty_webApplicationException() {
        String username = "";
        String keyHandle = "";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        when(userSessionIdService.isValidSessionId(sessionId, username)).thenReturn(true);
        WebApplicationException exception = new WebApplicationException(Response.status(400).entity("test exception").build());
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(exception);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionSuperGluuController.buildFido2AssertionStartResponse(username, keyHandle, appId, sessionId));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(userSessionIdService).isValidSessionId(sessionId, username);
        verifyNoInteractions(dataMapperService, log);
    }

    @Test
    void buildFido2AssertionStartResponse_ifOneStep_webApplicationException() {
        String username = "";
        String keyHandle = "test_key_handle";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        when(userSessionIdService.isValidSessionId(sessionId, username)).thenReturn(true);
        //when(dataMapperService.createObjectNode()).thenReturn(mapper.createObjectNode());

        AssertionOptions response = assertionSuperGluuController.buildFido2AssertionStartResponse(username, keyHandle, appId, sessionId);
        assertNotNull(response);
        assertNotNull(response.getSuperGluuRequest());
        assertNotNull(response.getSuperGluuAppId());
        assertNotNull(response.getDocumentDomain());
        assertNotNull(response.getSuperGluuKeyHandle());
        assertNotNull(response.getSuperGluuRequestMode());
        assertNotNull(response.getUsername());
        assertNotNull(response.getSessionId());
        assertEquals((response.getSuperGluuRequest() == null ? "false" : response.getSuperGluuRequest().toString()),"true");
        assertEquals(response.getSuperGluuAppId(), appId);
        assertEquals(response.getDocumentDomain(), appId);
        assertEquals(response.getSuperGluuKeyHandle(), keyHandle);
        assertEquals(response.getSuperGluuRequestMode(), "one_step");
        assertEquals(response.getUsername(), "");
        assertEquals(response.getSessionId(), sessionId);

        verify(userSessionIdService).isValidSessionId(sessionId, username);
//        verify(dataMapperService).createObjectNode();
        verify(log).debug("Prepared U2F_V2 assertions options request: {}", response.toString());
    }

    @Test
    void buildFido2AssertionStartResponse_ifTwoStep_webApplicationException() {
        String username = "test_username";
        String keyHandle = "test_key_handle";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        when(userSessionIdService.isValidSessionId(sessionId, username)).thenReturn(true);

        AssertionOptions response = assertionSuperGluuController.buildFido2AssertionStartResponse(username, keyHandle, appId, sessionId);
        assertNotNull(response);
        assertTrue(response.getSuperGluuRequest());
        assertNotNull(response.getSuperGluuAppId());
        assertNotNull(response.getDocumentDomain());
        assertNotNull(response.getSuperGluuKeyHandle());
        assertNotNull(response.getSuperGluuRequestMode());
        assertNotNull(response.getUsername());
        assertNotNull(response.getSessionId());
        assertEquals((response.getSuperGluuRequest() == null ? "false" : response.getSuperGluuRequest().toString()),"true");
        assertEquals(response.getSuperGluuAppId(), appId);
        assertEquals(response.getDocumentDomain(), appId);
        assertEquals(response.getSuperGluuKeyHandle(), keyHandle);
        assertEquals(response.getSuperGluuRequestMode(), "two_step");
        assertEquals(response.getUsername(), username);
        assertEquals(response.getSessionId(), sessionId);

        verify(userSessionIdService).isValidSessionId(sessionId, username);
        verify(log).debug("Prepared U2F_V2 assertions options request: {}", response.toString());
    }

    @Test
    void finishAuthentication_validValues_valid() throws IOException {
        String username = "test_username";
        String authenticateResponseString = "test_authenticate_response_string";
        String clientData = "eyJ0eXAiOiJuYXZpZ2F0b3IuaWQuZ2V0QXNzZXJ0aW9uIiwiY2hhbGxlbmdlIjoidGVzdF9jaGFsbGVuZ2UiLCJvcmlnaW4iOiJ0ZXN0X29yaWdpbiJ9";
        String signatureData = "test_signature_data";
        String keyHandle = "test_key_handle";
        String deviceData = "test_device_data";
        AuthenticateResponse authenticateResponse = new AuthenticateResponse(clientData, signatureData, keyHandle, deviceData);
        when(dataMapperService.readValue(authenticateResponseString, AuthenticateResponse.class)).thenReturn(authenticateResponse);

        ObjectNode paramsNode = mapper.createObjectNode();
        ObjectNode clientDataNode = mapper.createObjectNode();
        ObjectNode responseNode = mapper.createObjectNode();
        ObjectNode attestationObjectNode = mapper.createObjectNode();
        when(dataMapperService.createObjectNode()).thenReturn(paramsNode, clientDataNode, responseNode, attestationObjectNode);
        when(base64Service.urlEncodeToString(any())).thenReturn("test_client_data_json", "test_signature", "test_authenticator_data", "test_attestation_object");
        when(digestService.hashSha256(anyString())).thenReturn("rp_id_hash".getBytes());
        when(dataMapperService.cborWriteAsBytes(attestationObjectNode)).thenReturn("dGVzdF9hdHRlc3RhdGlvbl9vYmplY3Q".getBytes());
        when(assertionService.verify(any())).thenReturn(mock(AttestationOrAssertionResponse.class));


        RawAuthenticateResponse rawAuthenticateResponse = new RawAuthenticateResponse((byte) 1, 0L, "test_signature".getBytes());
        when(rawAuthenticationService.parseRawAuthenticateResponse(authenticateResponse.getSignatureData())).thenReturn(rawAuthenticateResponse);

        JsonNode result = assertionSuperGluuController.finishAuthentication(username, authenticateResponseString);
        assertNotNull(result);
        assertTrue(result.has("status"));
        assertTrue(result.has("challenge"));
        assertEquals(result.get("status").asText(), "success");
        assertEquals(result.get("challenge").asText(), "test_challenge");

        verify(assertionService, times(1)).verify(any());
    }

    @Test
    void buildFido2AuthenticationVerifyResponse_ifClientDataUnsupportedRegisterTypes_fido2RuntimeException() {
        String username = "test_username";
        String authenticateResponseString = "test_authenticate_response_string";
        String clientData = "eyJ0eXAiOiJ3cm9uZ190eXAiLCJjaGFsbGVuZ2UiOiJ0ZXN0X2NoYWxsZW5nZSIsIm9yaWdpbiI6InRlc3Rfb3JpZ2luIn0";
        String signatureData = "test_signature_data";
        String keyHandle = "test_key_handle";
        String deviceData = "test_device_data";
        AuthenticateResponse authenticateResponse = new AuthenticateResponse(clientData, signatureData, keyHandle, deviceData);
        when(errorResponseFactory.badRequestException(any(), contains("Invalid options attestation request type"))).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionSuperGluuController.buildFido2AuthenticationVerifyResponse(username, authenticateResponseString, authenticateResponse));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verifyNoInteractions(dataMapperService, base64Service, rawAuthenticationService, log);
    }

    @Test
    void buildFido2AuthenticationVerifyResponse_ifThrowIOException_fido2RuntimeException() throws IOException {
        String username = "test_username";
        String authenticateResponseString = "test_authenticate_response_string";
        String clientData = "eyJ0eXAiOiJuYXZpZ2F0b3IuaWQuZ2V0QXNzZXJ0aW9uIiwiY2hhbGxlbmdlIjoidGVzdF9jaGFsbGVuZ2UiLCJvcmlnaW4iOiJ0ZXN0X29yaWdpbiJ9";
        String signatureData = "test_signature_data";
        String keyHandle = "test_key_handle";
        String deviceData = "test_device_data";
        AuthenticateResponse authenticateResponse = new AuthenticateResponse(clientData, signatureData, keyHandle, deviceData);
        ObjectNode paramsNode = mapper.createObjectNode();
        ObjectNode clientDataNode = mapper.createObjectNode();
        ObjectNode responseNode = mapper.createObjectNode();
        ObjectNode attestationObjectNode = mapper.createObjectNode();
        when(dataMapperService.createObjectNode()).thenReturn(paramsNode, clientDataNode, responseNode, attestationObjectNode);
        when(dataMapperService.cborWriteAsBytes(any())).thenThrow(new IOException("test_io_exception"));
        RawAuthenticateResponse rawAuthenticateResponse = new RawAuthenticateResponse((byte) 1, 0L, "test_signature".getBytes());
        when(rawAuthenticationService.parseRawAuthenticateResponse(authenticateResponse.getSignatureData())).thenReturn(rawAuthenticateResponse);
        when(base64Service.urlEncodeToString(any())).thenReturn("test_client_data_json", "test_signature", "test_authenticator_data", "test_attestation_object");
        when(digestService.hashSha256(anyString())).thenReturn("rp_id_hash".getBytes());
        when(errorResponseFactory.invalidRequest(contains("Failed to prepare attestationObject"), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionSuperGluuController.buildFido2AuthenticationVerifyResponse(username, authenticateResponseString, authenticateResponse));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(dataMapperService, times(2)).createObjectNode();
        verify(base64Service, times(3)).urlEncodeToString(any());
        verify(rawAuthenticationService).parseRawAuthenticateResponse(any());
        verifyNoInteractions(log);
    }

    @Test
    void buildFido2AuthenticationVerifyResponse_validValues_valid() throws IOException {
        String username = "test_username";
        String authenticateResponseString = "test_authenticate_response_string";
        String clientData = "eyJ0eXAiOiJuYXZpZ2F0b3IuaWQuZ2V0QXNzZXJ0aW9uIiwiY2hhbGxlbmdlIjoidGVzdF9jaGFsbGVuZ2UiLCJvcmlnaW4iOiJ0ZXN0X29yaWdpbiJ9";
        String signatureData = "test_signature_data";
        String keyHandle = "test_key_handle";
        String deviceData = "test_device_data";
        AuthenticateResponse authenticateResponse = new AuthenticateResponse(clientData, signatureData, keyHandle, deviceData);
        ObjectNode paramsNode = mapper.createObjectNode();
        ObjectNode clientDataNode = mapper.createObjectNode();
        ObjectNode responseNode = mapper.createObjectNode();
        ObjectNode attestationObjectNode = mapper.createObjectNode();
        when(dataMapperService.createObjectNode()).thenReturn(paramsNode, clientDataNode, responseNode, attestationObjectNode);
        when(dataMapperService.cborWriteAsBytes(any())).thenReturn("test_attestation_object".getBytes());
        RawAuthenticateResponse rawAuthenticateResponse = new RawAuthenticateResponse((byte) 1, 0L, "test_signature".getBytes());
        when(rawAuthenticationService.parseRawAuthenticateResponse(authenticateResponse.getSignatureData())).thenReturn(rawAuthenticateResponse);
        when(base64Service.urlEncodeToString(any())).thenReturn("test_client_data_json", "test_signature", "test_authenticator_data", "test_attestation_object");
        when(digestService.hashSha256(anyString())).thenReturn("rp_id_hash".getBytes());

        AssertionResult response = assertionSuperGluuController.buildFido2AuthenticationVerifyResponse(username, authenticateResponseString, authenticateResponse);
        assertNotNull(response);
        assertNotNull(response.getSuperGluuRequest());
        assertNotNull(response.getSuperGluuRequestMode());
        assertNotNull(response.getSuperGluuRequestCancel());
        assertNotNull(response.getId());
        assertNotNull(response.getRawId());
        assertNotNull(response.getType());
        assertEquals((response.getSuperGluuRequest() == null ? "false" : response.getSuperGluuRequest().toString()),"true");
        assertEquals(response.getSuperGluuRequestMode(), "two_step");
        assertEquals((response.getSuperGluuRequestCancel() == null ? "false" : response.getSuperGluuRequestCancel().toString()),"false");
        assertEquals(response.getId(), "test_key_handle");
        assertEquals(response.getRawId(), "test_authenticate_response_string");
        assertEquals(response.getType(), "public-key");

        assertNotNull(response.getResponse());
        io.jans.fido2.model.assertion.Response response1 = response.getResponse();
        assertNotNull(response1.getClientDataJSON());
        assertNotNull(response1.getSignature());
        assertNotNull(response1.getAuthenticatorData());
        assertNotNull(response1.getAttestationObject());
        assertEquals(response1.getClientDataJSON(), "test_client_data_json");
        assertEquals(response1.getSignature(), "test_signature");
        assertEquals(response1.getAuthenticatorData(), "test_authenticator_data");
        assertEquals(response1.getAttestationObject(), "test_attestation_object");

        verify(dataMapperService, times(2)).createObjectNode();
        verify(base64Service, times(4)).urlEncodeToString(any());
        verify(rawAuthenticationService).parseRawAuthenticateResponse(any());
        verify(dataMapperService).cborWriteAsBytes(any());
        verify(log).debug("Prepared U2F_V2 assertion verify request: {}", response.toString());
    }

    @Test
    void parseAuthenticateResponse_ifThrowIOException_fido2RpRuntimeException() throws IOException {
        String authenticateResponseString = "wrong_authenticate_response_string";
        when(dataMapperService.readValue(authenticateResponseString, AuthenticateResponse.class)).thenThrow(new IOException("test_io_exception"));
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionSuperGluuController.parseAuthenticateResponse(authenticateResponseString));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void parseAuthenticateResponse_validValues_valid() throws IOException {
        String authenticateResponseString = "test_authenticate_response_string";
        when(dataMapperService.readValue(authenticateResponseString, AuthenticateResponse.class)).thenReturn(mock(AuthenticateResponse.class));

        AuthenticateResponse response = assertionSuperGluuController.parseAuthenticateResponse(authenticateResponseString);
        assertNotNull(response);
    }
}
