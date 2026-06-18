package io.jans.fido2.service.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import io.jans.fido2.ctap.AttestationConveyancePreference;
import io.jans.fido2.ctap.TokenBindingSupport;
import io.jans.fido2.exception.Fido2CompromisedDevice;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.assertion.AssertionOptions;
import io.jans.fido2.model.assertion.AssertionResult;
import io.jans.fido2.model.attestation.AttestationOptions;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.RequestedParty;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.processor.attestation.AppleAttestationProcessor;
import io.jans.fido2.service.processor.attestation.TPMProcessor;
import io.jans.fido2.service.processors.AttestationFormatProcessor;
import io.jans.service.net.NetworkService;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.BadRequestException;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

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
        assertEquals("Hashes don't match", ex.getMessage());
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
    void verifyRpDomain_originNotNull_valid() {
        String originsValue = "https://test.domain";
        List<RequestedParty> requestedParties = Collections.emptyList();
        when(networkService.getHost(originsValue)).thenReturn("test.domain");
        String response = commonVerifiers.verifyRpDomain(originsValue, null, requestedParties);

        assertNotNull(response);
        assertEquals("test.domain", response);
        verify(appConfiguration, never()).getIssuer();
    }

    @Test
    void verifyRpDomain_originIsNull_valid() {
        String issuer = "https://test.domain";
        List<RequestedParty> requestedParties = Collections.emptyList();
        when(networkService.getHost(issuer)).thenReturn("test.domain");
        String response = commonVerifiers.verifyRpDomain(null, issuer, requestedParties);

        assertNotNull(response);
        assertEquals("test.domain", response);
    }


    @Test
    void verifyRpDomain_originMatchesValidOrigin_valid() {
        String origin = "https://test.bank.com";
        String rpId = "bank.com";
        List<RequestedParty> requestedParties = new ArrayList<>();
        RequestedParty rp = new RequestedParty();
        rp.setOrigins(Arrays.asList("test.bank.com", "emp.bank.com", "india.bank.com"));
        requestedParties.add(rp);

        when(networkService.getHost(origin)).thenReturn("test.bank.com");

        String response = commonVerifiers.verifyRpDomain(origin, rpId, requestedParties);

        assertNotNull(response);
        assertEquals("test.bank.com", response);
    }

    @Test
    void verifyRpDomain_originDoesNotMatchValidOrigins_invalid() {
        String origin = "https://test.bank1.com";
        String rpId = "bank.com";
        List<RequestedParty> requestedParties = new ArrayList<>();
        RequestedParty rp = new RequestedParty();
        rp.setOrigins(Arrays.asList("test.bank.com", "emp.bank.com", "india.bank.com"));
        requestedParties.add(rp);

        when(networkService.getHost(origin)).thenReturn("test.bank1.com");

        when(errorResponseFactory.badRequestException(any(), anyString()))
                .thenThrow(new BadRequestException("The origin " + origin + " is not listed in the allowed origins."));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> commonVerifiers.verifyRpDomain(origin, rpId, requestedParties));

        assertEquals("The origin " + origin + " is not listed in the allowed origins.", exception.getMessage());
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
        assertEquals("Counter did not increase", ex.getMessage());
        verify(log).debug("old counter {} new counter {} ", oldCounter, newCounter);
    }

    @Test
    void verifyCounter_ifCounterLessThanZero_fido2RuntimeException() {
        int counter = -1;

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyCounter(counter));
        assertNotNull(ex);
        assertEquals("Invalid field : counter", ex.getMessage());
    }

    @Test
    void verifyCounter_IfCounterGreeterThanZero_valid() {
        int counter = 0;

        commonVerifiers.verifyCounter(counter);
    }

    @Test
    void verifyAttestationOptions_paramsEmpty_fido2RuntimeException() {
        AttestationOptions attestationOptions = mock(AttestationOptions.class);
        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyAttestationOptions(attestationOptions));
        assertNotNull(ex);
        assertEquals("Username is a mandatory parameter", ex.getMessage());
    }

    @Test
    void verifyAttestationOptions_paramsWithValues_valid() {
        AttestationOptions params = new AttestationOptions();
        params.setUsername("TEST-username");
        params.setDisplayName("TEST-displayName");
        params.setAttestation(AttestationConveyancePreference.direct);

        commonVerifiers.verifyAttestationOptions(params);
    }

    @Test
    void verifyAssertionOptions_paramsEmpty_fido2RuntimeException() {
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        AssertionOptions assertionOptions = mock(AssertionOptions.class);
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyAssertionOptions(assertionOptions));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyAssertionOptions_paramsWithValues_valid() {
        AssertionOptions assertionOptions = new AssertionOptions();
        assertionOptions.setUsername("TEST-username");


        commonVerifiers.verifyAssertionOptions(assertionOptions);
    }

    @Test
    void verifyBasicPayload_paramsEmpty_fido2RuntimeException() {
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        AssertionResult assertionResult = mock(AssertionResult.class);
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyBasicPayload(assertionResult));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyBasicPayload_paramsWithValues_valid() {
        AssertionResult assertionResult =new AssertionResult();
        assertionResult.setResponse(new io.jans.fido2.model.assertion.Response());
        assertionResult.setId("TEST-id");

        commonVerifiers.verifyBasicPayload(assertionResult);
    }

    @Test
    void verifyBase64UrlString_fieldIsBase64Url_valid() {
        ObjectNode node = mapper.createObjectNode();
        String fieldName = "TEST-fieldName";
        node.put(fieldName, "TEST-fieldNameValue");

        String response = commonVerifiers.verifyBase64UrlString(node, fieldName);
        assertEquals("TEST-fieldNameValue", response);
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
        assertEquals("Invalid \"" + fieldName + "\"", ex.getMessage());
    }

    @Test
    void verifyBase64String_ifValueIsEmpty_fido2RuntimeException() {
        JsonNode node = mock(JsonNode.class);
        when(node.isBinary()).thenReturn(true);
        when(node.asText()).thenReturn("");

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyBase64String(node));
        assertNotNull(ex);
        assertEquals("Invalid data", ex.getMessage());
    }

    @Test
    void verifyBase64String_ifNodeNotIsBase64_fido2RuntimeException() {
        JsonNode node = mock(JsonNode.class);
        when(node.isBinary()).thenReturn(true);
        when(node.asText()).thenReturn("TEST-value");
        when(base64Service.decode("TEST-value".getBytes())).thenThrow(new IllegalArgumentException());

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyBase64String(node));
        assertNotNull(ex);
        assertEquals("Invalid data", ex.getMessage());
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
        assertEquals(fieldValue, response);
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
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyThatNonEmptyString_ifValueNotIsEmpty_value() {
        ObjectNode node = mapper.createObjectNode();
        String fieldName = "TEST-fieldName";
        String fieldValue = "TEST-fieldValue";
        node.put(fieldName, fieldValue);

        String response = commonVerifiers.verifyThatNonEmptyString(node, fieldName);
        assertNotNull(response);
        assertEquals(fieldValue, response);
    }

    @Test
    void verifyThatBinary_ifNodeIsNotBinary_fido2RuntimeException() {
        ObjectNode node = mapper.createObjectNode();
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyThatBinary(node));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyThatBinary_ifNodeIsBinary_value() {
        String value = "TEST-value";
        JsonNode node = new BinaryNode(value.getBytes());

        String response = commonVerifiers.verifyThatBinary(node);
        assertNotNull(response);
        assertEquals("VEVTVC12YWx1ZQ==", response);
    }

    @Test
    void verifyAuthData_ifDataIsEmpty_fido2RuntimeException() {
        JsonNode node = new BinaryNode(new byte[]{});
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyAuthData(node));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyAuthData_ifDataNotIsEmpty_valid() {
        JsonNode node = new BinaryNode("TEST-value".getBytes());

        String response = commonVerifiers.verifyAuthData(node);
        assertNotNull(response);
        assertEquals("VEVTVC12YWx1ZQ==", response);
    }

    @Test
    void verifyAuthStatement_validValues_valid() {
        JsonNode node = new TextNode("TEST-value");

        JsonNode response = commonVerifiers.verifyAuthStatement(node);
        assertNotNull(response);
        assertEquals(node, response);
    }

    @Test
    void verifyAlgorithm_ifAlgorithmTypeNotEqualsRegisteredAlgorithmType_fido2RuntimeException() {
        JsonNode alg = new IntNode(1);
        int registeredAlgorithmType = -257;
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyAlgorithm(alg, registeredAlgorithmType));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyAlgorithm_ifAlgorithmTypeIsEqualsRegisteredAlgorithmType_valid() {
        JsonNode alg = new IntNode(-257);
        int registeredAlgorithmType = -257;

        int response = commonVerifiers.verifyAlgorithm(alg, registeredAlgorithmType);
        assertEquals(-257, response);
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
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyFmt_ifAlgorithmTypeIsEqualsRegisteredAlgorithmType_valid() {
        String fmt = "tpm";
        JsonNode fmtNode = mapper.createObjectNode().put("fmt", fmt);
        String fieldName = "fmt";
        when(supportedAttestationFormats.stream()).thenReturn(Stream.of(new TPMProcessor()));

        String response = commonVerifiers.verifyFmt(fmtNode, fieldName);
        assertNotNull(response);
        assertEquals(fmt, response);
    }

    @Test
    void verifyAAGUIDZeroed_ifAaguidNotContainsZero_fido2RuntimeException() {
        AuthData authData = new AuthData();
        authData.setAaguid("TEST-aaguid".getBytes());
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyAAGUIDZeroed(authData));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
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
        JsonNode clientJsonNode = mapper.createObjectNode().put("type", "webauthn.get");

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
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyClientJSONTypeIsGet_ifTypeIsWebauthnCreate_rejected() {
        // CONF-07: an assertion clientData.type other than "webauthn.get" must be rejected.
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("type", "webauthn.create");
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("wrong type").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSONTypeIsGet(clientJsonNode));
        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void verifyClientJSONTypeIsGet1_ifTypeNotFound_fido2RuntimeException() {
        JsonNode clientJsonNode = mock(JsonNode.class);
        String type = "TEST-type";
        when(clientJsonNode.hasNonNull("type")).thenReturn(false);
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        // FIDO2 conformance (CONF-07): an absent clientData.type must be rejected, not ignored.
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSONType(clientJsonNode, type));
        assertNotNull(ex);
        assertEquals(400, ex.getResponse().getStatus());
        verify(clientJsonNode, never()).get("type");
    }

    @Test
    void verifyClientJSONTypeIsGet1_ifClientJsonNodeHasTypeAndEquals_valid() {
        JsonNode clientJsonNode = mock(JsonNode.class);
        String type = "TEST-type";
        when(clientJsonNode.hasNonNull("type")).thenReturn(true);
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
    void verifyClientJSONTypeIsCreate_ifTypeAbsent_throws() {
        // CONF-07 (attestation mirror): registration clientData must carry type "webauthn.create".
        ObjectNode clientJsonNode = mapper.createObjectNode();
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("missing type").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSONTypeIsCreate(clientJsonNode));
        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void verifyClientJSONTypeIsCreate_ifTypeIsNotCreate_throws() {
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("type", "webauthn.get");
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("wrong type").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSONTypeIsCreate(clientJsonNode));
        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void verifyClientJSON_ifClientJsonNodeChallengeIsNull_fido2RuntimeException() {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-clientDataJSON");
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("origin", "TEST-origin");
        clientJsonNode.put("type", "TEST-type");

        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        String encodedClientDataJSON = base64Service.urlEncodeToString(clientJsonNode.toString().getBytes());
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSON(encodedClientDataJSON));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyClientJSON_ifClientJsonNodeOriginIsNull_fido2RuntimeException() {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-clientDataJSON");
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("challenge", "TEST-challenge");
        clientJsonNode.put("type", "TEST-type");
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        String encodedClientDataJSON = base64Service.urlEncodeToString(clientJsonNode.toString().getBytes());
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSON(encodedClientDataJSON));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyClientJSON_ifClientJsonNodeTypeIsNull_fido2RuntimeException() {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-clientDataJSON");
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("challenge", "TEST-challenge");
        clientJsonNode.put("origin", "TEST-origin");
        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        String encodedClientDataJSON = base64Service.urlEncodeToString(clientJsonNode.toString().getBytes());
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSON(encodedClientDataJSON));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyClientJSON_ifTokenBindingIsNotNull_fido2RuntimeException() {
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("challenge", "TEST-challenge");
        clientJsonNode.put("origin", "TEST-origin");
        clientJsonNode.put("type", "TEST-type");
        clientJsonNode.put("tokenBinding", mapper.createObjectNode());

        when(errorResponseFactory.invalidRequest(any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        String encodedClientDataJSON = base64Service.urlEncodeToString(clientJsonNode.toString().getBytes());

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> commonVerifiers.verifyClientJSON(encodedClientDataJSON));

        // Assertions for the expected exception
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyClientJSON_ifTokenBindingIsNotNullAndStatusIsNotNull_value() throws IOException {
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("challenge", "TEST-challenge");
        clientJsonNode.put("origin", "TEST-origin");
        clientJsonNode.put("type", "TEST-type");
        ObjectNode tokenBinding = mapper.createObjectNode();
        tokenBinding.put("status", "supported");
        clientJsonNode.put("tokenBinding", tokenBinding);

        byte[] jsonBytes = clientJsonNode.toString().getBytes(StandardCharsets.UTF_8);

        String mockEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(jsonBytes);
        when(base64Service.encodeToString(jsonBytes)).thenReturn(Base64.getUrlEncoder().withoutPadding().encodeToString(jsonBytes));

        when(base64Service.urlDecode(mockEncoded)).thenReturn(jsonBytes);

        when(dataMapperService.readTree(new String(jsonBytes, StandardCharsets.UTF_8))).thenReturn(clientJsonNode);

        String encodedClientDataJSON = base64Service.encodeToString(jsonBytes);

        JsonNode response = commonVerifiers.verifyClientJSON(encodedClientDataJSON);

        // Assert the expected behavior
        assertNotNull(response);
        assertTrue(response.has("challenge"));
        assertTrue(response.has("origin"));
        assertTrue(response.has("type"));
        assertEquals("TEST-challenge", response.get("challenge").asText());
        assertEquals("TEST-origin", response.get("origin").asText());
        assertEquals("TEST-type", response.get("type").asText());
    }

    @Test
    void verifyClientJSON_ifOriginIsEmpty_fido2RuntimeException() {
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("challenge", "TEST-challenge");
        clientJsonNode.put("origin", "");
        clientJsonNode.put("type", "TEST-type");

        when(errorResponseFactory.invalidRequest(any()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        String encodedClientDataJSON = base64Service.urlEncodeToString(clientJsonNode.toString().getBytes());
        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> commonVerifiers.verifyClientJSON(encodedClientDataJSON));

        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyClientJSON_validValues_value() throws IOException {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataJSON", "TEST-challenge");

        when(base64Service.urlDecode("TEST-challenge")).thenReturn("TEST-clientDataJsonDecoded".getBytes());
        ObjectNode clientJsonNode = mapper.createObjectNode();
        clientJsonNode.put("challenge", "TEST-challenge");
        clientJsonNode.put("origin", "TEST-origin");
        clientJsonNode.put("type", "TEST-type");
        byte[] jsonBytes = clientJsonNode.toString().getBytes(StandardCharsets.UTF_8);

        String mockEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(jsonBytes);
        when(base64Service.encodeToString(jsonBytes)).thenReturn(Base64.getUrlEncoder().withoutPadding().encodeToString(jsonBytes));

        when(base64Service.urlDecode(mockEncoded)).thenReturn(jsonBytes);

        when(dataMapperService.readTree(new String(jsonBytes, StandardCharsets.UTF_8))).thenReturn(clientJsonNode);

        String encodedClientDataJSON = base64Service.encodeToString(jsonBytes);

        JsonNode response = commonVerifiers.verifyClientJSON(encodedClientDataJSON);
        assertNotNull(response);
        assertTrue(response.has("challenge"));
        assertTrue(response.has("origin"));
        assertTrue(response.has("type"));
        assertEquals("TEST-challenge", response.get("challenge").asText());
        assertEquals("TEST-origin", response.get("origin").asText());
        assertEquals("TEST-type", response.get("type").asText());
    }

    @Test
    void verifyClientRaw_ifClientDataRawIsNull_fido2RuntimeException() {
        ObjectNode responseNode = mapper.createObjectNode();

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyClientRaw(responseNode));
        assertNotNull(ex);
        assertEquals("Client data RAW is missing", ex.getMessage());
    }

    @Test
    void verifyClientRaw_ifClientDataRawIsNotNull_valid() {
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("clientDataRaw", "TEST-clientDataRaw");

        JsonNode response = commonVerifiers.verifyClientRaw(responseNode);
        assertNotNull(response);
        assertEquals("TEST-clientDataRaw", response.asText());
    }

    @Test
    void verifyTPMVersion_ifClientDataRawIsNotNull_valid() {
        JsonNode responseNode = new TextNode("1.0");

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyTPMVersion(responseNode));
        assertNotNull(ex);
        assertEquals("Invalid TPM Attestation version", ex.getMessage());
    }

    @Test
    void verifyTPMVersion_validVersion_valid() {
        JsonNode responseNode = new TextNode("2.0");

        commonVerifiers.verifyTPMVersion(responseNode);
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
        assertEquals(400, ex.getResponse().getStatus());
        assertEquals("test exception", ex.getResponse().getEntity());
    }

    @Test
    void verifyTokenBindingSupport_validValues_valid() {
        String status = "supported";

        TokenBindingSupport response = commonVerifiers.verifyTokenBindingSupport(status);
        assertNotNull(response);
        assertEquals("supported", response.getStatus());
    }

    @Test
    void verifyThatMetadataIsValid_ifReadTreeCausesAnException_fido2RuntimeException() throws IOException {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("metadataStatement", "TEST-metadataStatement");
        when(dataMapperService.readTree(new TextNode("TEST-metadataStatement").toPrettyString())).thenThrow(new IOException());

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyThatMetadataIsValid(metadata));
        assertNotNull(ex);
        assertEquals("Unable to process metadataStatement:", ex.getMessage());
    }

    @Test
    void verifyThatMetadataIsValid_ifAaguidIsNull_fido2RuntimeException() throws IOException {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("metadataStatement", "TEST-metadataStatement");
        ObjectNode metaDataStatementNode = mapper.createObjectNode();
        metaDataStatementNode.put("attestationTypes", "TEST-attestationTypes");
        metaDataStatementNode.put("description", "TEST-description");
        when(dataMapperService.readTree(new TextNode("TEST-metadataStatement").toPrettyString())).thenReturn(metaDataStatementNode);

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyThatMetadataIsValid(metadata));
        assertNotNull(ex);
        assertEquals("Invalid parameters in metadata", ex.getMessage());
    }

    @Test
    void verifyThatMetadataIsValid_ifAttestationTypesIsNull_fido2RuntimeException() throws IOException {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("metadataStatement", "TEST-metadataStatement");
        ObjectNode metaDataStatementNode = mapper.createObjectNode();
        metaDataStatementNode.put("aaguid", "TEST-aaguid");
        metaDataStatementNode.put("description", "TEST-description");
        when(dataMapperService.readTree(new TextNode("TEST-metadataStatement").toPrettyString())).thenReturn(metaDataStatementNode);

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyThatMetadataIsValid(metadata));
        assertNotNull(ex);
        assertEquals("Invalid parameters in metadata", ex.getMessage());
    }

    @Test
    void verifyThatMetadataIsValid_ifDescriptionIsNull_fido2RuntimeException() throws IOException {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("metadataStatement", "TEST-metadataStatement");
        ObjectNode metaDataStatementNode = mapper.createObjectNode();
        metaDataStatementNode.put("aaguid", "TEST-aaguid");
        metaDataStatementNode.put("attestationTypes", "TEST-attestationTypes");
        when(dataMapperService.readTree(new TextNode("TEST-metadataStatement").toPrettyString())).thenReturn(metaDataStatementNode);

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> commonVerifiers.verifyThatMetadataIsValid(metadata));
        assertNotNull(ex);
        assertEquals("Invalid parameters in metadata", ex.getMessage());
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
}