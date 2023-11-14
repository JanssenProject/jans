package io.jans.fido2.service.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import io.jans.fido2.ctap.AttestationConveyancePreference;
import io.jans.fido2.ctap.AuthenticatorAttachment;
import io.jans.fido2.ctap.TokenBindingSupport;
import io.jans.fido2.exception.Fido2CompromisedDevice;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.processor.attestation.AppleAttestationProcessor;
import io.jans.fido2.service.processor.attestation.TPMProcessor;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.orm.model.fido2.UserVerification;
import io.jans.service.net.NetworkService;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.stream.Stream;

import static io.jans.fido2.service.verifier.CommonVerifiers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonVerifiersTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private CommonVerifiers commonVerifiers;

    @Mock
    private Logger log;

    @Mock
    private NetworkService networkService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private Base64Service base64Service;

    @Mock
    private DataMapperService dataMapperService;

    @Mock
    private Instance<AttestationFormatProcessor> supportedAttestationFormats;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void verifyRpIdHash_retrieveRpIdHashAndCalculatedRpIdHashNotEqual_fido2RuntimeException() {
        AuthData authData = new AuthData();
        authData.setRpIdHash("TEST-rpIdHash".getBytes());
        String domain = "https://test.domain";
        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyRpIdHash(authData, domain));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Hashes don't match");
        verify(log, times(2)).debug(anyString(), anyString());
        verify(log).warn("hash from domain doesn't match hash from assertion HEX");
    }

    @Test
    void verifyRpIdHash_retrieveRpIdHashAndCalculatedRpIdHashAreEqual_valid() {
        AuthData authData = new AuthData();
        authData.setRpIdHash(Hex.decode("dadf92be57a43dde330ee83ddfa4ef40b455466d6477c55c2562a6059f6236fa"));
        String domain = "https://test.domain";
        commonVerifiers.verifyRpIdHash(authData, domain);
        verify(log, times(2)).debug(anyString(), anyString());
        verify(log, never()).warn("hash from domain doesn't match hash from assertion HEX");
    }

    @Test
    void verifyRpDomain_documentDomainNotNull_valid() {
        ObjectNode params = mapper.createObjectNode();
        String documentDomainsValue = "https://test.domain";
        params.put("documentDomain", documentDomainsValue);
        when(networkService.getHost(documentDomainsValue)).thenReturn("test.domain");

        String response = commonVerifiers.verifyRpDomain(params);
        assertNotNull(response);
        assertEquals(response, "test.domain");
        verify(appConfiguration, never()).getIssuer();
    }

    @Test
    void verifyRpDomain_documentDomainIsNull_valid() {
        JsonNode params = mock(JsonNode.class);
        String issuer = "https://test.domain";
        when(params.hasNonNull("documentDomain")).thenReturn(false);
        when(appConfiguration.getIssuer()).thenReturn(issuer);
        when(networkService.getHost(issuer)).thenReturn("test.domain");

        String response = commonVerifiers.verifyRpDomain(params);
        assertNotNull(response);
        assertEquals(response, "test.domain");
        verify(params, never()).get("documentDomain");
    }

    @Test
    void verifyCounter_oldAndNewCounterZero_valid() {
        int oldCounter = 0;
        int newCounter = 0;

        commonVerifiers.verifyCounter(oldCounter, newCounter);
        verify(log).debug("old counter {} new counter {} ", oldCounter, newCounter);
    }

    @Test
    void verifyCounter_oldCounterOneAndNewCounterZero_fido2CompromisedDevice() {
        int oldCounter = 1;
        int newCounter = 0;

        Fido2CompromisedDevice ex = assertThrows(Fido2CompromisedDevice.class, () -> commonVerifiers.verifyCounter(oldCounter, newCounter));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Counter did not increase");
        verify(log).debug("old counter {} new counter {} ", oldCounter, newCounter);
    }

    @Test
    void verifyCounter_ifCounterLessThanZero_fido2RuntimeException() {
        int counter = -1;

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyCounter(counter));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Invalid field : counter");
    }

    @Test
    void verifyCounter_IfCounterGreeterThanZero_valid() {
        int counter = 0;

        commonVerifiers.verifyCounter(counter);
    }

    @Test
    void verifyAttestationOptions_paramsEmpty_fido2RuntimeException() {
        ObjectNode params = mapper.createObjectNode();

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyAttestationOptions(params));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Invalid parameters");
    }

    @Test
    void verifyAttestationOptions_paramsWithValues_valid() {
        ObjectNode params = mapper.createObjectNode();
        params.put("username", "TEST-username");
        params.put("displayName", "TEST-displayName");
        params.put("attestation", "TEST-attestation");

        commonVerifiers.verifyAttestationOptions(params);
    }

    @Test
    void verifyAssertionOptions_paramsEmpty_fido2RuntimeException() {
        ObjectNode params = mapper.createObjectNode();
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyAssertionOptions(params));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyAssertionOptions_paramsWithValues_valid() {
        ObjectNode params = mapper.createObjectNode();
        params.put("username", "TEST-username");

        commonVerifiers.verifyAssertionOptions(params);
    }

    @Test
    void verifyBasicPayload_paramsEmpty_fido2RuntimeException() {
        ObjectNode params = mapper.createObjectNode();
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyBasicPayload(params));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyBasicPayload_paramsWithValues_valid() {
        ObjectNode params = mapper.createObjectNode();
        params.put("response", "TEST-response");
        params.put("type", "TEST-type");
        params.put("id", "TEST-id");

        commonVerifiers.verifyBasicPayload(params);
    }

    @Test
    void verifyBase64UrlString_fieldIsBase64Url_valid() {
        ObjectNode node = mapper.createObjectNode();
        String fieldName = "TEST-fieldName";
        node.put(fieldName, "TEST-fieldNameValue");

        String response = commonVerifiers.verifyBase64UrlString(node, fieldName);
        assertEquals(response, "TEST-fieldNameValue");
        verify(base64Service).urlDecode("TEST-fieldNameValue");
    }

    @Test
    void verifyBase64UrlString_fieldNotIsBase64Url_fido2RuntimeException() {
        ObjectNode node = mapper.createObjectNode();
        String fieldName = "TEST-fieldName";
        node.put(fieldName, "TEST-fieldNameValue");
        when(base64Service.urlDecode("TEST-fieldNameValue")).thenThrow(new Fido2RuntimeException("Invalid \"" + fieldName + "\""));

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyBase64UrlString(node, fieldName));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Invalid \"" + fieldName + "\"");
    }

    @Test
    void verifyBase64String_ifValueIsEmpty_fido2RuntimeException() {
        JsonNode node = mock(JsonNode.class);
        when(node.isBinary()).thenReturn(true);
        when(node.asText()).thenReturn("");

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyBase64String(node));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Invalid data");
    }

    @Test
    void verifyBase64String_ifNodeNotIsBase64_fido2RuntimeException() {
        JsonNode node = mock(JsonNode.class);
        when(node.isBinary()).thenReturn(true);
        when(node.asText()).thenReturn("TEST-value");
        when(base64Service.decode("TEST-value".getBytes())).thenThrow(new IllegalArgumentException());

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyBase64String(node));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Invalid data");
    }

    @Test
    void verifyBase64String_ifNodeIsBase64_valid() {
        JsonNode node = mock(JsonNode.class);
        when(node.isBinary()).thenReturn(true);
        when(node.asText()).thenReturn("TEST-value");

        assertDoesNotThrow(() -> commonVerifiers.verifyBase64String(node));
    }

    @Test
    void verifyThatFieldString_validValues_valid() {
        ObjectNode node = mapper.createObjectNode();
        String fieldName = "TEST-fieldName";
        String fieldValue = "TEST-fieldValue";
        node.put(fieldName, fieldValue);

        String response = commonVerifiers.verifyThatFieldString(node, fieldName);
        assertNotNull(response);
        assertEquals(response, fieldValue);
    }

    @Test
    void verifyThatNonEmptyString_ifValueIsEmpty_fido2RuntimeException() {
        ObjectNode node = mapper.createObjectNode();
        String fieldName = "TEST-fieldName";
        String fieldValue = "";
        node.put(fieldName, fieldValue);
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyThatNonEmptyString(node, fieldName));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyThatNonEmptyString_ifValueNotIsEmpty_value() {
        ObjectNode node = mapper.createObjectNode();
        String fieldName = "TEST-fieldName";
        String fieldValue = "TEST-fieldValue";
        node.put(fieldName, fieldValue);

        String response = commonVerifiers.verifyThatNonEmptyString(node, fieldName);
        assertNotNull(response);
        assertEquals(response, fieldValue);
    }

    @Test
    void verifyThatBinary_ifNodeIsNotBinary_fido2RuntimeException() {
        ObjectNode node = mapper.createObjectNode();
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyThatBinary(node));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyThatBinary_ifNodeIsBinary_value() {
        String value = "TEST-value";
        JsonNode node = new BinaryNode(value.getBytes());

        String response = commonVerifiers.verifyThatBinary(node);
        assertNotNull(response);
        assertEquals(response, "VEVTVC12YWx1ZQ==");
    }

    @Test
    void verifyAuthData_ifDataIsEmpty_fido2RuntimeException() {
        JsonNode node = new BinaryNode(new byte[]{});
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyAuthData(node));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyAuthData_ifDataNotIsEmpty_valid() {
        JsonNode node = new BinaryNode("TEST-value".getBytes());

        String response = commonVerifiers.verifyAuthData(node);
        assertNotNull(response);
        assertEquals(response, "VEVTVC12YWx1ZQ==");
    }

    @Test
    void verifyAuthStatement_validValues_valid() {
        JsonNode node = new TextNode("TEST-value");

        JsonNode response = commonVerifiers.verifyAuthStatement(node);
        assertNotNull(response);
        assertEquals(response, node);
    }

    @Test
    void verifyAlgorithm_ifAlgorithmTypeNotEqualsRegisteredAlgorithmType_fido2RuntimeException() {
        JsonNode alg = new IntNode(1);
        int registeredAlgorithmType = -257;
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyAlgorithm(alg, registeredAlgorithmType));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyAlgorithm_ifAlgorithmTypeIsEqualsRegisteredAlgorithmType_valid() {
        JsonNode alg = new IntNode(-257);
        int registeredAlgorithmType = -257;

        int response = commonVerifiers.verifyAlgorithm(alg, registeredAlgorithmType);
        assertEquals(response, -257);
    }

    @Test
    void verifyFmt_ifFmtNotFound_fido2RuntimeException() {
        String fmt = "tpm";
        JsonNode fmtNode = mapper.createObjectNode().put("fmt", fmt);
        String fieldName = "fmt";
        when(supportedAttestationFormats.stream()).thenReturn(Stream.of(new AppleAttestationProcessor()));
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyFmt(fmtNode, fieldName));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyFmt_ifAlgorithmTypeIsEqualsRegisteredAlgorithmType_valid() {
        String fmt = "tpm";
        JsonNode fmtNode = mapper.createObjectNode().put("fmt", fmt);
        String fieldName = "fmt";
        when(supportedAttestationFormats.stream()).thenReturn(Stream.of(new TPMProcessor()));

        String response = commonVerifiers.verifyFmt(fmtNode, fieldName);
        assertNotNull(response);
        assertEquals(response, fmt);
    }

    @Test
    void verifyAAGUIDZeroed_ifAaguidNotContainsZero_fido2RuntimeException() {
        AuthData authData = new AuthData();
        authData.setAaguid("TEST-aaguid".getBytes());
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyAAGUIDZeroed(authData));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyAAGUIDZeroed_ifAaguidEmpty_valid() {
        AuthData authData = new AuthData();
        authData.setAaguid(new byte[]{});

        assertDoesNotThrow(() -> commonVerifiers.verifyAAGUIDZeroed(authData));
    }

    @Test
    void verifyAAGUIDZeroed_ifAaguidContainsZero_valid() {
        AuthData authData = mock(AuthData.class);
        when(authData.getAaguid()).thenReturn(new byte[]{0, 0});

        assertDoesNotThrow(() -> commonVerifiers.verifyAAGUIDZeroed(authData));
    }

    @Test
    void verifyClientJSONTypeIsGet_validValues_valid() {
        JsonNode clientJsonNode = mapper.createObjectNode().put("webauthn.get", "TEST-webauthn.get");

        commonVerifiers.verifyClientJSONTypeIsGet(clientJsonNode);
    }

    @Test
    void verifyClientJSONTypeIsGet1_ifTypeNotEqualOtherType_fido2RuntimeException() {
        JsonNode clientJsonNode = mapper.createObjectNode().put("type", "TEST-type");
        String type = "TEST-type1";
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSONType(clientJsonNode, type));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyClientJSONTypeIsGet1_ifTypeNotFound_fido2RuntimeException() {
        JsonNode clientJsonNode = mock(JsonNode.class);
        String type = "TEST-type";
        when(clientJsonNode.has("type")).thenReturn(false);

        assertDoesNotThrow(() -> commonVerifiers.verifyClientJSONType(clientJsonNode, type));
        verify(clientJsonNode, never()).get("type");
    }

    @Test
    void verifyClientJSONTypeIsGet1_ifClientJsonNodeHasTypeAndEquals_valid() {
        JsonNode clientJsonNode = mock(JsonNode.class);
        String type = "TEST-type";
        when(clientJsonNode.has("type")).thenReturn(true);
        when(clientJsonNode.get("type")).thenReturn(new TextNode(type));

        commonVerifiers.verifyClientJSONType(clientJsonNode, type);
    }

    @Test
    void verifyClientJSONTypeIsCreate_validValues_valid() {
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("type", "webauthn.create");

        commonVerifiers.verifyClientJSONTypeIsCreate(clientJsonNode);
    }

    @Test
    void verifyClientJSON_ifClientDataJsonIsMissing_fido2RuntimeException() {
        ObjectNode responseNode = mapper.createObjectNode();
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSON(responseNode));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyClientJSON_ifClientDataIsEmpty_fido2RuntimeException() throws IOException {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-clientDataJSON");
        when(base64Service.urlDecode("TEST-clientDataJSON")).thenReturn("TEST-clientDataJsonDecoded".getBytes());
        when(dataMapperService.readTree("TEST-clientDataJsonDecoded")).thenReturn(null);
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSON(responseNode));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyClientJSON_ifReadTreeGivesIOException_fido2RuntimeException() throws IOException {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-clientDataJSON");
        when(base64Service.urlDecode("TEST-clientDataJSON")).thenReturn("TEST-clientDataJsonDecoded".getBytes());
        when(dataMapperService.readTree("TEST-clientDataJsonDecoded")).thenThrow(new IOException());
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSON(responseNode));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyClientJSON_ifClientJsonNodeChallengeIsNull_fido2RuntimeException() throws IOException {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-clientDataJSON");
        when(base64Service.urlDecode("TEST-clientDataJSON")).thenReturn("TEST-clientDataJsonDecoded".getBytes());
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("origin", "TEST-origin");
        clientJsonNode.put("type", "TEST-type");
        when(dataMapperService.readTree("TEST-clientDataJsonDecoded")).thenReturn(clientJsonNode);
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSON(responseNode));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyClientJSON_ifClientJsonNodeOriginIsNull_fido2RuntimeException() throws IOException {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-clientDataJSON");
        when(base64Service.urlDecode("TEST-clientDataJSON")).thenReturn("TEST-clientDataJsonDecoded".getBytes());
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("challenge", "TEST-challenge");
        clientJsonNode.put("type", "TEST-type");
        when(dataMapperService.readTree("TEST-clientDataJsonDecoded")).thenReturn(clientJsonNode);
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSON(responseNode));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyClientJSON_ifClientJsonNodeTypeIsNull_fido2RuntimeException() throws IOException {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-clientDataJSON");
        when(base64Service.urlDecode("TEST-clientDataJSON")).thenReturn("TEST-clientDataJsonDecoded".getBytes());
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("challenge", "TEST-challenge");
        clientJsonNode.put("origin", "TEST-origin");
        when(dataMapperService.readTree("TEST-clientDataJsonDecoded")).thenReturn(clientJsonNode);
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSON(responseNode));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyClientJSON_ifTokenBindingIsNotNull_fido2RuntimeException() throws IOException {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-clientDataJSON");
        when(base64Service.urlDecode("TEST-clientDataJSON")).thenReturn("TEST-clientDataJsonDecoded".getBytes());
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("challenge", "TEST-challenge");
        clientJsonNode.put("origin", "TEST-origin");
        clientJsonNode.put("type", "TEST-type");
        clientJsonNode.put("tokenBinding", mapper.createObjectNode());
        when(dataMapperService.readTree("TEST-clientDataJsonDecoded")).thenReturn(clientJsonNode);
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSON(responseNode));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyClientJSON_ifTokenBindingIsNotNullAndStatusIsNotNull_value() throws IOException {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-clientDataJSON");
        when(base64Service.urlDecode("TEST-clientDataJSON")).thenReturn("TEST-clientDataJsonDecoded".getBytes());
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("challenge", "TEST-challenge");
        clientJsonNode.put("origin", "TEST-origin");
        clientJsonNode.put("type", "TEST-type");
        ObjectNode tokenBinding = mapper.createObjectNode();
        tokenBinding.put("status", "supported");
        clientJsonNode.put("tokenBinding", tokenBinding);
        when(dataMapperService.readTree("TEST-clientDataJsonDecoded")).thenReturn(clientJsonNode);

        JsonNode response = commonVerifiers.verifyClientJSON(responseNode);
        assertNotNull(response);
        assertTrue(response.has("challenge"));
        assertTrue(response.has("origin"));
        assertTrue(response.has("type"));
        assertEquals(response.get("challenge").asText(), "TEST-challenge");
        assertEquals(response.get("origin").asText(), "TEST-origin");
        assertEquals(response.get("type").asText(), "TEST-type");
    }

    @Test
    void verifyClientJSON_ifTokenBindingIsNotNullAndIdIsNotNull_value() throws IOException {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-clientDataJSON");
        when(base64Service.urlDecode("TEST-clientDataJSON")).thenReturn("TEST-clientDataJsonDecoded".getBytes());
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("challenge", "TEST-challenge");
        clientJsonNode.put("origin", "TEST-origin");
        clientJsonNode.put("type", "TEST-type");
        ObjectNode tokenBinding = mapper.createObjectNode();
        tokenBinding.put("status", "supported");
        tokenBinding.put("id", "TEST-id");
        clientJsonNode.put("tokenBinding", tokenBinding);
        when(dataMapperService.readTree("TEST-clientDataJsonDecoded")).thenReturn(clientJsonNode);

        JsonNode response = commonVerifiers.verifyClientJSON(responseNode);
        assertNotNull(response);
        assertTrue(response.has("challenge"));
        assertTrue(response.has("origin"));
        assertTrue(response.has("type"));
        assertEquals(response.get("challenge").asText(), "TEST-challenge");
        assertEquals(response.get("origin").asText(), "TEST-origin");
        assertEquals(response.get("type").asText(), "TEST-type");
    }

    @Test
    void verifyClientJSON_ifOriginIsEmpty_fido2RuntimeException() throws IOException {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-clientDataJSON");
        when(base64Service.urlDecode("TEST-clientDataJSON")).thenReturn("TEST-clientDataJsonDecoded".getBytes());
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("challenge", "TEST-challenge");
        clientJsonNode.put("origin", "");
        clientJsonNode.put("type", "TEST-type");
        when(dataMapperService.readTree("TEST-clientDataJsonDecoded")).thenReturn(clientJsonNode);
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSON(responseNode));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyClientJSON_validValues_value() throws IOException {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-clientDataJSON");
        when(base64Service.urlDecode("TEST-clientDataJSON")).thenReturn("TEST-clientDataJsonDecoded".getBytes());
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("challenge", "TEST-challenge");
        clientJsonNode.put("origin", "TEST-origin");
        clientJsonNode.put("type", "TEST-type");
        when(dataMapperService.readTree("TEST-clientDataJsonDecoded")).thenReturn(clientJsonNode);

        JsonNode response = commonVerifiers.verifyClientJSON(responseNode);
        assertNotNull(response);
        assertTrue(response.has("challenge"));
        assertTrue(response.has("origin"));
        assertTrue(response.has("type"));
        assertEquals(response.get("challenge").asText(), "TEST-challenge");
        assertEquals(response.get("origin").asText(), "TEST-origin");
        assertEquals(response.get("type").asText(), "TEST-type");
    }

    @Test
    void verifyClientRaw_ifClientDataRawIsNull_fido2RuntimeException() {
        ObjectNode responseNode = mapper.createObjectNode();

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyClientRaw(responseNode));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Client data RAW is missing");
    }

    @Test
    void verifyClientRaw_ifClientDataRawIsNotNull_valid() {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataRaw", "TEST-clientDataRaw");

        JsonNode response = commonVerifiers.verifyClientRaw(responseNode);
        assertNotNull(response);
        assertEquals(response.asText(), "TEST-clientDataRaw");
    }

    @Test
    void verifyTPMVersion_ifClientDataRawIsNotNull_valid() {
        JsonNode responseNode = new TextNode("1.0");

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyTPMVersion(responseNode));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Invalid TPM Attestation version");
    }

    @Test
    void verifyTPMVersion_validVersion_valid() {
        JsonNode responseNode = new TextNode("2.0");

        commonVerifiers.verifyTPMVersion(responseNode);
    }

    @Test
    void verifyAttestationConveyanceType_ifAttestationIsNotNull_valid() {
        ObjectNode params = mapper.createObjectNode();
        params.put("attestation", "indirect");

        AttestationConveyancePreference response = commonVerifiers.verifyAttestationConveyanceType(params);
        assertNotNull(response);
        assertEquals(response.name(), "indirect");
    }

    @Test
    void verifyAttestationConveyanceType_ifAttestationConveyancePreferenceIsNull_valid() {
        ObjectNode params = mapper.createObjectNode();

        AttestationConveyancePreference response = commonVerifiers.verifyAttestationConveyanceType(params);
        assertNotNull(response);
        assertEquals(response.name(), "direct");
    }

    @Test
    void verifyTokenBindingSupport_ifStatusIsNull_null() {
        TokenBindingSupport response = commonVerifiers.verifyTokenBindingSupport(null);
        assertNull(response);
    }

    @Test
    void verifyTokenBindingSupport_ifTokenBindingSupportEnumIsNull_fido2RuntimeException() {
        String status = "WRONG-STATUS";
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyTokenBindingSupport(status));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");
    }

    @Test
    void verifyTokenBindingSupport_validValues_valid() {
        String status = "supported";

        TokenBindingSupport response = commonVerifiers.verifyTokenBindingSupport(status);
        assertNotNull(response);
        assertEquals(response.getStatus(), "supported");
    }

    @Test
    void verifyAuthenticatorAttachment_ifStatusIsNull_null() {
        AuthenticatorAttachment response = commonVerifiers.verifyAuthenticatorAttachment(null);
        assertNull(response);
    }

    @Test
    void verifyAuthenticatorAttachment_ifTokenBindingSupportEnumIsNull_fido2RuntimeException() {
        JsonNode authenticatorAttachment = new TextNode("WRONG-AUTHENTICATOR-ATTACHMENT");

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyAuthenticatorAttachment(authenticatorAttachment));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Wrong authenticator attachment parameter " + authenticatorAttachment);
    }

    @Test
    void verifyAuthenticatorAttachment_validValues_valid() {
        JsonNode authenticatorAttachment = new TextNode("platform");

        AuthenticatorAttachment response = commonVerifiers.verifyAuthenticatorAttachment(authenticatorAttachment);
        assertNotNull(response);
        assertEquals(response.getAttachment(), "platform");
    }

    @Test
    void verifyUserVerification_ifStatusIsNull_null() {
        UserVerification response = commonVerifiers.verifyUserVerification(null);
        assertNull(response);
    }

    @Test
    void verifyUserVerification_ifTokenBindingSupportEnumIsNull_fido2RuntimeException() {
        JsonNode userVerification = new TextNode("WRONG-USER-VERIFICATION");

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyUserVerification(userVerification));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Wrong user verification parameter " + userVerification);
    }

    @Test
    void verifyUserVerification_validValues_valid() {
        JsonNode userVerification = new TextNode("required");

        UserVerification response = commonVerifiers.verifyUserVerification(userVerification);
        assertNotNull(response);
        assertEquals(response.name(), "required");
    }

    @Test
    void verifyTimeout_ifParamsContainsTimeout_valid() {
        ObjectNode params = mapper.createObjectNode();
        params.put("timeout", 120);

        int response = commonVerifiers.verifyTimeout(params);
        assertEquals(response, 120);
    }

    @Test
    void verifyTimeout_simpleFlow_valid() {
        ObjectNode params = mapper.createObjectNode();

        int response = commonVerifiers.verifyTimeout(params);
        assertEquals(response, 90);
    }

    @Test
    void verifyThatMetadataIsValid_ifReadTreeCausesAnException_fido2RuntimeException() throws IOException {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("metadataStatement", "TEST-metadataStatement");
        when(dataMapperService.readTree(new TextNode("TEST-metadataStatement").toPrettyString())).thenThrow(new IOException());

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyThatMetadataIsValid(metadata));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Unable to process metadataStatement:");
    }

    @Test
    void verifyThatMetadataIsValid_ifAaguidIsNull_fido2RuntimeException() throws IOException {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("metadataStatement", "TEST-metadataStatement");
        ObjectNode metaDataStatementNode = mapper.createObjectNode();
//        metaDataStatementNode.put("aaguid", "TEST-aaguid");
        metaDataStatementNode.put("attestationTypes", "TEST-attestationTypes");
        metaDataStatementNode.put("description", "TEST-description");
        when(dataMapperService.readTree(new TextNode("TEST-metadataStatement").toPrettyString())).thenReturn(metaDataStatementNode);

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyThatMetadataIsValid(metadata));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Invalid parameters in metadata");
    }

    @Test
    void verifyThatMetadataIsValid_ifAttestationTypesIsNull_fido2RuntimeException() throws IOException {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("metadataStatement", "TEST-metadataStatement");
        ObjectNode metaDataStatementNode = mapper.createObjectNode();
        metaDataStatementNode.put("aaguid", "TEST-aaguid");
//        metaDataStatementNode.put("attestationTypes", "TEST-attestationTypes");
        metaDataStatementNode.put("description", "TEST-description");
        when(dataMapperService.readTree(new TextNode("TEST-metadataStatement").toPrettyString())).thenReturn(metaDataStatementNode);

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyThatMetadataIsValid(metadata));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Invalid parameters in metadata");
    }

    @Test
    void verifyThatMetadataIsValid_ifDescriptionIsNull_fido2RuntimeException() throws IOException {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("metadataStatement", "TEST-metadataStatement");
        ObjectNode metaDataStatementNode = mapper.createObjectNode();
        metaDataStatementNode.put("aaguid", "TEST-aaguid");
        metaDataStatementNode.put("attestationTypes", "TEST-attestationTypes");
//        metaDataStatementNode.put("description", "TEST-description");
        when(dataMapperService.readTree(new TextNode("TEST-metadataStatement").toPrettyString())).thenReturn(metaDataStatementNode);

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyThatMetadataIsValid(metadata));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Invalid parameters in metadata");
    }

    @Test
    void verifyThatMetadataIsValid_validParams_valid() throws IOException {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("metadataStatement", "TEST-metadataStatement");
        ObjectNode metaDataStatementNode = mapper.createObjectNode();
        metaDataStatementNode.put("aaguid", "TEST-aaguid");
        metaDataStatementNode.put("attestationTypes", "TEST-attestationTypes");
        metaDataStatementNode.put("description", "TEST-description");
        when(dataMapperService.readTree(new TextNode("TEST-metadataStatement").toPrettyString())).thenReturn(metaDataStatementNode);

        commonVerifiers.verifyThatMetadataIsValid(metadata);
    }

    @Test
    void hasSuperGluu_ifParamsContainsSuperGluuRequestAndNodeIsBooleanFalse_false() {
        final String SUPER_GLUU_REQUEST = "super_gluu_request";
        JsonNode params = mock(JsonNode.class);
        JsonNode node = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(true);
        when(params.get(SUPER_GLUU_REQUEST)).thenReturn(node);
        when(node.isBoolean()).thenReturn(false);

        boolean response = commonVerifiers.hasSuperGluu(params);
        assertFalse(response);
        verify(node, never()).asBoolean();
    }

    @Test
    void hasSuperGluu_ifParamsContainsSuperGluuRequestAndNodeIsBooleanTrueAndNodeAsBooleanFalse_false() {
        final String SUPER_GLUU_REQUEST = "super_gluu_request";
        JsonNode params = mock(JsonNode.class);
        JsonNode node = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(true);
        when(params.get(SUPER_GLUU_REQUEST)).thenReturn(node);
        when(node.isBoolean()).thenReturn(true);
        when(node.asBoolean()).thenReturn(false);

        boolean response = commonVerifiers.hasSuperGluu(params);
        assertFalse(response);
    }

    @Test
    void hasSuperGluu_ifParamsContainsSuperGluuRequestAndNodeIsAllTrue_false() {
        final String SUPER_GLUU_REQUEST = "super_gluu_request";
        JsonNode params = mock(JsonNode.class);
        JsonNode node = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(true);
        when(params.get(SUPER_GLUU_REQUEST)).thenReturn(node);
        when(node.isBoolean()).thenReturn(true);
        when(node.asBoolean()).thenReturn(true);

        boolean response = commonVerifiers.hasSuperGluu(params);
        assertTrue(response);
    }

    @Test
    void hasSuperGluu_ifParamsNotContainsSuperGluuRequest_false() {
        final String SUPER_GLUU_REQUEST = "super_gluu_request";
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(false);

        boolean response = commonVerifiers.hasSuperGluu(params);
        assertFalse(response);
        verify(params, never()).get(SUPER_GLUU_REQUEST);
    }

    @Test
    void verifyNotUseGluuParameters_ifParamsHasNonNullSuperGluuRequestIsTrue_fido2RpRuntimeException() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(true);
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyNotUseGluuParameters(params));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(params).hasNonNull(SUPER_GLUU_REQUEST);
        verify(params, never()).hasNonNull(SUPER_GLUU_MODE);
        verify(params, never()).hasNonNull(SUPER_GLUU_APP_ID);
        verify(params, never()).hasNonNull(SUPER_GLUU_KEY_HANDLE);
        verify(params, never()).hasNonNull(SUPER_GLUU_REQUEST_CANCEL);
    }

    @Test
    void verifyNotUseGluuParameters_ifParamsHasNonNullSuperGluuModeIsTrue_fido2RpRuntimeException() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(false);
        when(params.hasNonNull(SUPER_GLUU_MODE)).thenReturn(true);
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyNotUseGluuParameters(params));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(params).hasNonNull(SUPER_GLUU_REQUEST);
        verify(params).hasNonNull(SUPER_GLUU_MODE);
        verify(params, never()).hasNonNull(SUPER_GLUU_APP_ID);
        verify(params, never()).hasNonNull(SUPER_GLUU_KEY_HANDLE);
        verify(params, never()).hasNonNull(SUPER_GLUU_REQUEST_CANCEL);
    }

    @Test
    void verifyNotUseGluuParameters_ifParamsHasNonNullSuperGluuAppIdIsTrue_fido2RpRuntimeException() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(false);
        when(params.hasNonNull(SUPER_GLUU_MODE)).thenReturn(false);
        when(params.hasNonNull(SUPER_GLUU_APP_ID)).thenReturn(true);
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyNotUseGluuParameters(params));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(params).hasNonNull(SUPER_GLUU_REQUEST);
        verify(params).hasNonNull(SUPER_GLUU_MODE);
        verify(params).hasNonNull(SUPER_GLUU_APP_ID);
        verify(params, never()).hasNonNull(SUPER_GLUU_KEY_HANDLE);
        verify(params, never()).hasNonNull(SUPER_GLUU_REQUEST_CANCEL);
    }

    @Test
    void verifyNotUseGluuParameters_ifParamsHasNonNullSuperGluuKeyHandleIsTrue_fido2RpRuntimeException() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(false);
        when(params.hasNonNull(SUPER_GLUU_MODE)).thenReturn(false);
        when(params.hasNonNull(SUPER_GLUU_APP_ID)).thenReturn(false);
        when(params.hasNonNull(SUPER_GLUU_KEY_HANDLE)).thenReturn(true);
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyNotUseGluuParameters(params));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(params).hasNonNull(SUPER_GLUU_REQUEST);
        verify(params).hasNonNull(SUPER_GLUU_MODE);
        verify(params).hasNonNull(SUPER_GLUU_APP_ID);
        verify(params).hasNonNull(SUPER_GLUU_KEY_HANDLE);
        verify(params, never()).hasNonNull(SUPER_GLUU_REQUEST_CANCEL);
    }

    @Test
    void verifyNotUseGluuParameters_ifParamsHasNonNullSuperGluuRequestCancelIsTrue_fido2RpRuntimeException() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(false);
        when(params.hasNonNull(SUPER_GLUU_MODE)).thenReturn(false);
        when(params.hasNonNull(SUPER_GLUU_APP_ID)).thenReturn(false);
        when(params.hasNonNull(SUPER_GLUU_KEY_HANDLE)).thenReturn(false);
        when(params.hasNonNull(SUPER_GLUU_REQUEST_CANCEL)).thenReturn(true);
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyNotUseGluuParameters(params));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(params).hasNonNull(SUPER_GLUU_REQUEST);
        verify(params).hasNonNull(SUPER_GLUU_MODE);
        verify(params).hasNonNull(SUPER_GLUU_APP_ID);
        verify(params).hasNonNull(SUPER_GLUU_KEY_HANDLE);
        verify(params).hasNonNull(SUPER_GLUU_REQUEST_CANCEL);
    }

    @Test
    void isSuperGluuOneStepMode_ifHasSuperGluuIsFalse_false() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(false);

        boolean response = commonVerifiers.isSuperGluuOneStepMode(params);
        assertFalse(response);
        verify(params, never()).hasNonNull(SUPER_GLUU_MODE);
        verify(params, never()).get(SUPER_GLUU_MODE);
    }

    @Test
    void isSuperGluuOneStepMode_ifHasNonNullSuperGluuModeIsFalse_false() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(true);
        when(params.get(SUPER_GLUU_REQUEST)).thenReturn(BooleanNode.TRUE);
        when(params.hasNonNull(SUPER_GLUU_MODE)).thenReturn(false);

        boolean response = commonVerifiers.isSuperGluuOneStepMode(params);
        assertFalse(response);
        verify(params).hasNonNull(SUPER_GLUU_MODE);
        verify(params, never()).get(SUPER_GLUU_MODE);
    }

    @Test
    void isSuperGluuOneStepMode_ifOneStepIsFalse_false() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(true);
        when(params.get(SUPER_GLUU_REQUEST)).thenReturn(BooleanNode.TRUE);
        when(params.hasNonNull(SUPER_GLUU_MODE)).thenReturn(true);
        when(params.get(SUPER_GLUU_MODE)).thenReturn(new TextNode("WRONG-STEP"));

        boolean response = commonVerifiers.isSuperGluuOneStepMode(params);
        assertFalse(response);
        verify(params).hasNonNull(SUPER_GLUU_MODE);
        verify(params).get(SUPER_GLUU_MODE);
    }

    @Test
    void isSuperGluuOneStepMode_ifOneStepIsTrue_true() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(true);
        when(params.get(SUPER_GLUU_REQUEST)).thenReturn(BooleanNode.TRUE);
        when(params.hasNonNull(SUPER_GLUU_MODE)).thenReturn(true);
        when(params.get(SUPER_GLUU_MODE)).thenReturn(new TextNode("one_step"));

        boolean response = commonVerifiers.isSuperGluuOneStepMode(params);
        assertTrue(response);
        verify(params).hasNonNull(SUPER_GLUU_MODE);
        verify(params).get(SUPER_GLUU_MODE);
    }

    @Test
    void isSuperGluuCancelRequest_ifHasSuperGluuIsFalse_false() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(false);

        boolean response = commonVerifiers.isSuperGluuCancelRequest(params);
        assertFalse(response);
        verify(params, never()).hasNonNull(SUPER_GLUU_REQUEST_CANCEL);
        verify(params, never()).get(SUPER_GLUU_REQUEST_CANCEL);
    }

    @Test
    void isSuperGluuCancelRequest_ifHasNonNullSuperGluuRequestCancelIsFalse_false() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(true);
        when(params.get(SUPER_GLUU_REQUEST)).thenReturn(BooleanNode.TRUE);
        when(params.hasNonNull(SUPER_GLUU_REQUEST_CANCEL)).thenReturn(false);

        boolean response = commonVerifiers.isSuperGluuCancelRequest(params);
        assertFalse(response);
        verify(params).hasNonNull(SUPER_GLUU_REQUEST_CANCEL);
        verify(params, never()).get(SUPER_GLUU_REQUEST_CANCEL);
    }

    @Test
    void isSuperGluuCancelRequest_ifNodeIsBooleanIsFalse_false() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(true);
        when(params.get(SUPER_GLUU_REQUEST)).thenReturn(BooleanNode.TRUE);
        when(params.hasNonNull(SUPER_GLUU_REQUEST_CANCEL)).thenReturn(true);
        JsonNode node = mock(JsonNode.class);
        when(params.get(SUPER_GLUU_REQUEST_CANCEL)).thenReturn(node);
        when(node.isBoolean()).thenReturn(false);

        boolean response = commonVerifiers.isSuperGluuCancelRequest(params);
        assertFalse(response);
        verify(params).hasNonNull(SUPER_GLUU_REQUEST_CANCEL);
        verify(params).get(SUPER_GLUU_REQUEST_CANCEL);
        verify(node).isBoolean();
        verify(node, never()).asBoolean();
    }

    @Test
    void isSuperGluuCancelRequest_ifNodeAsBooleanIsFalse_false() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(true);
        when(params.get(SUPER_GLUU_REQUEST)).thenReturn(BooleanNode.TRUE);
        when(params.hasNonNull(SUPER_GLUU_REQUEST_CANCEL)).thenReturn(true);
        JsonNode node = mock(JsonNode.class);
        when(params.get(SUPER_GLUU_REQUEST_CANCEL)).thenReturn(node);
        when(node.isBoolean()).thenReturn(true);
        when(node.asBoolean()).thenReturn(false);

        boolean response = commonVerifiers.isSuperGluuCancelRequest(params);
        assertFalse(response);
        verify(params).hasNonNull(SUPER_GLUU_REQUEST_CANCEL);
        verify(params).get(SUPER_GLUU_REQUEST_CANCEL);
        verify(node).isBoolean();
        verify(node).asBoolean();
    }

    @Test
    void isSuperGluuCancelRequest_nodeIsBooleanAndAsBooleanIsTrue_true() {
        JsonNode params = mock(JsonNode.class);
        when(params.hasNonNull(SUPER_GLUU_REQUEST)).thenReturn(true);
        when(params.get(SUPER_GLUU_REQUEST)).thenReturn(BooleanNode.TRUE);
        when(params.hasNonNull(SUPER_GLUU_REQUEST_CANCEL)).thenReturn(true);
        JsonNode node = mock(JsonNode.class);
        when(params.get(SUPER_GLUU_REQUEST_CANCEL)).thenReturn(node);
        when(node.isBoolean()).thenReturn(true);
        when(node.asBoolean()).thenReturn(true);

        boolean response = commonVerifiers.isSuperGluuCancelRequest(params);
        assertTrue(response);
        verify(params).hasNonNull(SUPER_GLUU_REQUEST_CANCEL);
        verify(params).get(SUPER_GLUU_REQUEST_CANCEL);
        verify(node).isBoolean();
        verify(node).asBoolean();
    }
}
