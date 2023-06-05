package io.jans.fido2.service.sg.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.as.model.fido.u2f.message.RawAuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.AuthenticateResponse;
import io.jans.fido2.exception.Fido2RpRuntimeException;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.DigestService;
import io.jans.fido2.service.operation.AssertionService;
import io.jans.fido2.service.persist.UserSessionIdService;
import io.jans.fido2.service.sg.RawAuthenticationService;
import jakarta.ws.rs.WebApplicationException;
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

    @Test
    void startAuthentication_ifAllowCredentialsIsNull_valid() {
        String username = "test_username";
        String keyHandle = "test_key_handle";
        String appId = "test_app_id";
        String sessionId = "session_id";
        ObjectNode resultNode = mapper.createObjectNode();
        resultNode.put("challenge", "test_challenge");
        resultNode.put("userVerification", "test_user_verification");
        when(assertionService.options(any())).thenReturn(resultNode);
        when(dataMapperService.createObjectNode()).thenReturn(mapper.createObjectNode(), mapper.createObjectNode());
        when(userSessionIdService.isValidSessionId(sessionId, username)).thenReturn(true);

        JsonNode response = assertionSuperGluuController.startAuthentication(username, keyHandle, appId, sessionId);
        assertNotNull(response);
        assertTrue(response.has("authenticateRequests"));
        JsonNode authenticateRequestsNode = response.get("authenticateRequests");
        assertTrue(authenticateRequestsNode.isEmpty());

        verify(assertionService, only()).options(any());
        verify(dataMapperService, times(2)).createObjectNode();
    }

    @Test
    void startAuthentication_ifAllowCredentialsContainsValue_valid() {
        String username = "test_username";
        String keyHandle = "test_key_handle";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        ObjectNode resultNode = mapper.createObjectNode();
        resultNode.put("challenge", "test_challenge");
        resultNode.put("userVerification", "test_user_verification");
        ArrayNode credentialsArraysNode = mapper.createArrayNode();
        ObjectNode credentialsNode = mapper.createObjectNode();
        credentialsNode.put("id", "test_id_1");
        credentialsArraysNode.add(credentialsNode);
        resultNode.set("allowCredentials", credentialsArraysNode);
        when(assertionService.options(any())).thenReturn(resultNode);
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
    void buildFido2AssertionStartResponse_ifValidIsFalse_webApplicationException() throws JsonProcessingException {
        String username = "test_username";
        String keyHandle = "test_key_handle";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        when(userSessionIdService.isValidSessionId(sessionId, username)).thenReturn(false);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionSuperGluuController.buildFido2AssertionStartResponse(username, keyHandle, appId, sessionId));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertNotNull(ex.getResponse().getEntity());
        JsonNode entityNode = mapper.readTree(ex.getResponse().getEntity().toString());
        assertNotNull(entityNode);
        assertTrue(entityNode.has("reason"));
        assertTrue(entityNode.has("error_description"));
        assertTrue(entityNode.has("error"));
        assertEquals(entityNode.get("reason").asText(), "session_id 'test_session_id' is invalid");
        assertEquals(entityNode.get("error_description").asText(), "The session_id is null, blank or invalid, this param is required.");
        assertEquals(entityNode.get("error").asText(), "invalid_id_session");

        verify(userSessionIdService).isValidSessionId(sessionId, username);
        verifyNoInteractions(dataMapperService, log);
    }

    @Test
    void buildFido2AssertionStartResponse_ifUsernameAndKeyHandleIsEmpty_webApplicationException() throws JsonProcessingException {
        String username = "";
        String keyHandle = "";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        when(userSessionIdService.isValidSessionId(sessionId, username)).thenReturn(true);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> assertionSuperGluuController.buildFido2AssertionStartResponse(username, keyHandle, appId, sessionId));
        assertNotNull(ex);
        assertEquals(ex.getResponse().getStatus(), 400);
        assertNotNull(ex.getResponse().getEntity());
        JsonNode entityNode = mapper.readTree(ex.getResponse().getEntity().toString());
        assertNotNull(entityNode);
        assertTrue(entityNode.has("reason"));
        assertTrue(entityNode.has("error_description"));
        assertTrue(entityNode.has("error"));
        assertEquals(entityNode.get("reason").asText(), "invalid: username or keyHandle");
        assertEquals(entityNode.get("error_description").asText(), "The request should contains either username or keyHandle");
        assertEquals(entityNode.get("error").asText(), "invalid_username_or_keyhandle");

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
        when(dataMapperService.createObjectNode()).thenReturn(mapper.createObjectNode());

        ObjectNode response = assertionSuperGluuController.buildFido2AssertionStartResponse(username, keyHandle, appId, sessionId);
        assertNotNull(response);
        assertTrue(response.has("super_gluu_request"));
        assertTrue(response.has("super_gluu_app_id"));
        assertTrue(response.has("documentDomain"));
        assertTrue(response.has("super_gluu_key_handle"));
        assertTrue(response.has("super_gluu_request_mode"));
        assertTrue(response.has("username"));
        assertTrue(response.has("session_id"));
        assertEquals(response.get("super_gluu_request").asText(), "true");
        assertEquals(response.get("super_gluu_app_id").asText(), appId);
        assertEquals(response.get("documentDomain").asText(), appId);
        assertEquals(response.get("super_gluu_key_handle").asText(), keyHandle);
        assertEquals(response.get("super_gluu_request_mode").asText(), "one_step");
        assertEquals(response.get("username").asText(), "");
        assertEquals(response.get("session_id").asText(), sessionId);

        verify(userSessionIdService).isValidSessionId(sessionId, username);
        verify(dataMapperService).createObjectNode();
        verify(log).debug("Prepared U2F_V2 assertions options request: {}", response);
    }

    @Test
    void buildFido2AssertionStartResponse_ifTwoStep_webApplicationException() {
        String username = "test_username";
        String keyHandle = "test_key_handle";
        String appId = "test_app_id";
        String sessionId = "test_session_id";
        when(userSessionIdService.isValidSessionId(sessionId, username)).thenReturn(true);
        when(dataMapperService.createObjectNode()).thenReturn(mapper.createObjectNode());

        ObjectNode response = assertionSuperGluuController.buildFido2AssertionStartResponse(username, keyHandle, appId, sessionId);
        assertNotNull(response);
        assertTrue(response.has("super_gluu_request"));
        assertTrue(response.has("super_gluu_app_id"));
        assertTrue(response.has("documentDomain"));
        assertTrue(response.has("super_gluu_key_handle"));
        assertTrue(response.has("super_gluu_request_mode"));
        assertTrue(response.has("username"));
        assertTrue(response.has("session_id"));
        assertEquals(response.get("super_gluu_request").asText(), "true");
        assertEquals(response.get("super_gluu_app_id").asText(), appId);
        assertEquals(response.get("documentDomain").asText(), appId);
        assertEquals(response.get("super_gluu_key_handle").asText(), keyHandle);
        assertEquals(response.get("super_gluu_request_mode").asText(), "two_step");
        assertEquals(response.get("username").asText(), username);
        assertEquals(response.get("session_id").asText(), sessionId);

        verify(userSessionIdService).isValidSessionId(sessionId, username);
        verify(dataMapperService).createObjectNode();
        verify(log).debug("Prepared U2F_V2 assertions options request: {}", response);
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
        when(assertionService.verify(paramsNode)).thenReturn(mapper.createObjectNode());

        RawAuthenticateResponse rawAuthenticateResponse = new RawAuthenticateResponse((byte) 1, 0L, "test_signature".getBytes());
        when(rawAuthenticationService.parseRawAuthenticateResponse(authenticateResponse.getSignatureData())).thenReturn(rawAuthenticateResponse);

        JsonNode response = assertionSuperGluuController.finishAuthentication(username, authenticateResponseString);
        assertNotNull(response);
        assertTrue(response.has("status"));
        assertTrue(response.has("challenge"));
        assertEquals(response.get("status").asText(), "success");
        assertEquals(response.get("challenge").asText(), "test_challenge");

        verify(assertionService).verify(paramsNode);
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

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> assertionSuperGluuController.buildFido2AuthenticationVerifyResponse(username, authenticateResponseString, authenticateResponse));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Invalid options attestation request type");

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

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> assertionSuperGluuController.buildFido2AuthenticationVerifyResponse(username, authenticateResponseString, authenticateResponse));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Failed to prepare attestationObject");

        verify(dataMapperService, times(4)).createObjectNode();
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

        ObjectNode response = assertionSuperGluuController.buildFido2AuthenticationVerifyResponse(username, authenticateResponseString, authenticateResponse);
        assertNotNull(response);
        assertTrue(response.has("super_gluu_request"));
        assertTrue(response.has("super_gluu_request_mode"));
        assertTrue(response.has("super_gluu_request_cancel"));
        assertTrue(response.has("id"));
        assertTrue(response.has("rawId"));
        assertTrue(response.has("type"));
        assertEquals(response.get("super_gluu_request").asText(), "true");
        assertEquals(response.get("super_gluu_request_mode").asText(), "two_step");
        assertEquals(response.get("super_gluu_request_cancel").asText(), "false");
        assertEquals(response.get("id").asText(), "test_key_handle");
        assertEquals(response.get("rawId").asText(), "test_authenticate_response_string");
        assertEquals(response.get("type").asText(), "public-key");

        assertTrue(response.has("response"));
        JsonNode response1Node = response.get("response");
        assertTrue(response1Node.has("clientDataJSON"));
        assertTrue(response1Node.has("signature"));
        assertTrue(response1Node.has("authenticatorData"));
        assertTrue(response1Node.has("attestationObject"));
        assertEquals(response1Node.get("clientDataJSON").asText(), "test_client_data_json");
        assertEquals(response1Node.get("signature").asText(), "test_signature");
        assertEquals(response1Node.get("authenticatorData").asText(), "test_authenticator_data");
        assertEquals(response1Node.get("attestationObject").asText(), "test_attestation_object");

        verify(dataMapperService, times(4)).createObjectNode();
        verify(base64Service, times(4)).urlEncodeToString(any());
        verify(rawAuthenticationService).parseRawAuthenticateResponse(any());
        verify(dataMapperService).cborWriteAsBytes(any());
        verify(log).debug("Prepared U2F_V2 assertion verify request: {}", response);
    }

    @Test
    void parseAuthenticateResponse_ifThrowIOException_fido2RpRuntimeException() throws IOException {
        String authenticateResponseString = "wrong_authenticate_response_string";
        when(dataMapperService.readValue(authenticateResponseString, AuthenticateResponse.class)).thenThrow(new IOException("test_io_exception"));

        Fido2RpRuntimeException ex = assertThrows(Fido2RpRuntimeException.class, () -> assertionSuperGluuController.parseAuthenticateResponse(authenticateResponseString));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Failed to parse options assertion request");
    }

    @Test
    void parseAuthenticateResponse_validValues_valid() throws IOException {
        String authenticateResponseString = "test_authenticate_response_string";
        when(dataMapperService.readValue(authenticateResponseString, AuthenticateResponse.class)).thenReturn(mock(AuthenticateResponse.class));

        AuthenticateResponse response = assertionSuperGluuController.parseAuthenticateResponse(authenticateResponseString);
        assertNotNull(response);
    }
}
