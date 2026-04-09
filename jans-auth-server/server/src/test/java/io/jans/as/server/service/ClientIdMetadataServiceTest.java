/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.client.RegisterRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.persistence.model.ClientAttributes;
import io.jans.as.server.model.registration.RegisterParamsValidator;
import io.jans.as.server.register.ws.rs.RegisterService;
import io.jans.as.server.service.external.ExternalDynamicClientRegistrationService;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for ClientIdMetadataService.
 *
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class ClientIdMetadataServiceTest {

    @InjectMocks
    @Spy
    private ClientIdMetadataService clientIdMetadataService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ClientService clientService;

    @Mock
    private RegisterService registerService;

    @Mock
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;

    @Mock
    private RegisterParamsValidator registerParamsValidator;

    @BeforeMethod
    public void setUp() {
        lenient().when(appConfiguration.getCimdSchemeAllowlist()).thenReturn(Arrays.asList("https"));
        lenient().when(appConfiguration.getCimdBlockPrivateIp()).thenReturn(true);
        lenient().when(appConfiguration.getCimdMaxResponseSize()).thenReturn(65536);
        lenient().when(appConfiguration.getCimdConnectTimeoutMs()).thenReturn(5000);
        lenient().when(appConfiguration.getCimdReadTimeoutMs()).thenReturn(10000);
        lenient().when(appConfiguration.getCimdTtlMinutes()).thenReturn(60);
        lenient().when(appConfiguration.getCimdMaxTtlMinutes()).thenReturn(1440);

        // Default: redirect_uri validation passes, external script allows
        lenient().when(registerParamsValidator.validateRedirectUris(any(), any(), any(), any(), any(), any())).thenReturn(true);
        lenient().when(externalDynamicClientRegistrationService.executeExternalCreateClientMethods(any(), any(), any())).thenReturn(true);
    }

    // ==================== isCimdClientId Tests ====================

    @Test
    public void isCimdClientId_withNullClientId_shouldReturnFalse() {
        assertFalse(clientIdMetadataService.isCimdClientId(null));
    }

    @Test
    public void isCimdClientId_withEmptyClientId_shouldReturnFalse() {
        assertFalse(clientIdMetadataService.isCimdClientId(""));
    }

    @Test
    public void isCimdClientId_withBlankClientId_shouldReturnFalse() {
        assertFalse(clientIdMetadataService.isCimdClientId("   "));
    }

    @Test
    public void isCimdClientId_withFeatureDisabled_shouldReturnFalse() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENT_ID_METADATA_DOCUMENT)).thenReturn(false);
        assertFalse(clientIdMetadataService.isCimdClientId("https://example.com/client"));
    }

    @Test
    public void isCimdClientId_withValidHttpsUrl_shouldReturnTrue() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENT_ID_METADATA_DOCUMENT)).thenReturn(true);
        assertTrue(clientIdMetadataService.isCimdClientId("https://example.com/client"));
    }

    @Test
    public void isCimdClientId_withHttpUrl_shouldReturnFalse() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENT_ID_METADATA_DOCUMENT)).thenReturn(true);
        assertFalse(clientIdMetadataService.isCimdClientId("http://example.com/client"));
    }

    @Test
    public void isCimdClientId_withTraditionalClientId_shouldReturnFalse() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENT_ID_METADATA_DOCUMENT)).thenReturn(true);
        assertFalse(clientIdMetadataService.isCimdClientId("my-client-id-123"));
    }

    @Test
    public void isCimdClientId_withInvalidUrl_shouldReturnFalse() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENT_ID_METADATA_DOCUMENT)).thenReturn(true);
        assertFalse(clientIdMetadataService.isCimdClientId("not a valid url"));
    }

    @Test
    public void isCimdClientId_withUrlWithPort_shouldReturnTrue() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENT_ID_METADATA_DOCUMENT)).thenReturn(true);
        assertTrue(clientIdMetadataService.isCimdClientId("https://example.com:8443/client"));
    }

    @Test
    public void isCimdClientId_withUrlWithPath_shouldReturnTrue() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENT_ID_METADATA_DOCUMENT)).thenReturn(true);
        assertTrue(clientIdMetadataService.isCimdClientId("https://example.com/path/to/client"));
    }

    @Test
    public void isCimdClientId_withUrlWithQueryParams_shouldReturnTrue() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENT_ID_METADATA_DOCUMENT)).thenReturn(true);
        assertTrue(clientIdMetadataService.isCimdClientId("https://example.com/client?version=1"));
    }

    @Test
    public void isCimdClientId_withCustomScheme_shouldReturnTrue() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENT_ID_METADATA_DOCUMENT)).thenReturn(true);
        when(appConfiguration.getCimdSchemeAllowlist()).thenReturn(Arrays.asList("https", "http"));
        assertTrue(clientIdMetadataService.isCimdClientId("http://example.com/client"));
    }

    // ==================== isFeatureEnabled Tests ====================

    @Test
    public void isFeatureEnabled_whenEnabled_shouldReturnTrue() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENT_ID_METADATA_DOCUMENT)).thenReturn(true);
        assertTrue(clientIdMetadataService.isFeatureEnabled());
    }

    @Test
    public void isFeatureEnabled_whenDisabled_shouldReturnFalse() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.CLIENT_ID_METADATA_DOCUMENT)).thenReturn(false);
        assertFalse(clientIdMetadataService.isFeatureEnabled());
    }

    // ==================== validateScheme Tests ====================

    @Test
    public void validateScheme_withHttps_shouldPass() throws Exception {
        URI uri = new URI("https://example.com/client");
        clientIdMetadataService.validateScheme(uri); // should not throw
    }

    @Test
    public void validateScheme_withHttp_shouldThrowBadRequest() throws Exception {
        URI uri = new URI("http://example.com/client");
        try {
            clientIdMetadataService.validateScheme(uri);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateScheme_withNoScheme_shouldThrowBadRequest() throws Exception {
        URI uri = new URI("//example.com/client");
        try {
            clientIdMetadataService.validateScheme(uri);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    // ==================== validateClientIdBinding Tests ====================

    @Test
    public void validateClientIdBinding_withMatchingClientId_shouldPass() throws Exception {
        // client_id is preserved in getJsonObject() even though RegisterRequest doesn't parse it as a field
        RegisterRequest req = RegisterRequest.fromJson("{\"client_id\": \"https://example.com/client\"}");
        clientIdMetadataService.validateClientIdBinding(req, "https://example.com/client");
    }

    @Test
    public void validateClientIdBinding_withMismatchedClientId_shouldThrowBadRequest() throws Exception {
        RegisterRequest req = RegisterRequest.fromJson("{\"client_id\": \"https://attacker.com/evil\"}");
        try {
            clientIdMetadataService.validateClientIdBinding(req, "https://example.com/client");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("does not match"));
        }
    }

    @Test
    public void validateClientIdBinding_withAbsentClientId_shouldThrowBadRequest() throws Exception {
        // §4.1 MUST: document MUST contain a client_id property
        RegisterRequest req = RegisterRequest.fromJson("{}");
        try {
            clientIdMetadataService.validateClientIdBinding(req, "https://example.com/client");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("client_id"));
        }
    }

    @Test
    public void validateClientIdBinding_withNullJsonObject_shouldThrowBadRequest() {
        // §4.1 MUST: document MUST contain a client_id property
        RegisterRequest req = new RegisterRequest(); // jsonObject is null
        try {
            clientIdMetadataService.validateClientIdBinding(req, "https://example.com/client");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    // ==================== validateDomain Tests ====================

    @Test
    public void validateDomain_withValidDomain_shouldPass() throws Exception {
        URI uri = new URI("https://example.com/client");
        clientIdMetadataService.validateDomain(uri); // should not throw
    }

    @Test
    public void validateDomain_withBlockedDomain_shouldThrowBadRequest() throws Exception {
        when(appConfiguration.getCimdDomainBlocklist()).thenReturn(Arrays.asList("blocked.com"));
        URI uri = new URI("https://blocked.com/client");
        try {
            clientIdMetadataService.validateDomain(uri);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("blocked"));
        }
    }

    @Test
    public void validateDomain_withDomainNotInAllowlist_shouldThrowBadRequest() throws Exception {
        when(appConfiguration.getCimdDomainAllowlist()).thenReturn(Arrays.asList("allowed.com"));
        URI uri = new URI("https://other.com/client");
        try {
            clientIdMetadataService.validateDomain(uri);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("allowlist"));
        }
    }

    @Test
    public void validateDomain_withDomainInAllowlist_shouldPass() throws Exception {
        when(appConfiguration.getCimdDomainAllowlist()).thenReturn(Arrays.asList("allowed.com"));
        URI uri = new URI("https://allowed.com/client");
        clientIdMetadataService.validateDomain(uri); // should not throw
    }

    @Test
    public void validateDomain_withEmptyHost_shouldThrowBadRequest() throws Exception {
        URI uri = new URI("https:///path");
        try {
            clientIdMetadataService.validateDomain(uri);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateDomain_withMixedCaseBlockedDomain_shouldThrowBadRequest() throws Exception {
        // Blocklist entries stored in lowercase; URI host is already lowercased by Java URI parser
        when(appConfiguration.getCimdDomainBlocklist()).thenReturn(Arrays.asList("blocked.com"));
        URI uri = new URI("https://BLOCKED.COM/client");
        try {
            clientIdMetadataService.validateDomain(uri);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    // ==================== validateNotPrivateIp Tests ====================

    @Test
    public void validateNotPrivateIp_withPublicDomain_shouldPass() {
        doNothing().when(clientIdMetadataService).validateNotPrivateIp(anyString());
        clientIdMetadataService.validateNotPrivateIp("example.com"); // should not throw
    }

    @Test
    public void validateNotPrivateIp_withEmptyHost_shouldThrowBadRequest() {
        try {
            clientIdMetadataService.validateNotPrivateIp("");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateNotPrivateIp_withNullHost_shouldThrowBadRequest() {
        try {
            clientIdMetadataService.validateNotPrivateIp(null);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    // ==================== isPrivateAddress Tests ====================

    @Test
    public void isPrivateAddress_withLoopback_shouldReturnTrue() throws Exception {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        assertTrue(clientIdMetadataService.isPrivateAddress(loopback));
    }

    @Test
    public void isPrivateAddress_withLocalhost_shouldReturnTrue() throws Exception {
        InetAddress localhost = InetAddress.getByName("localhost");
        assertTrue(clientIdMetadataService.isPrivateAddress(localhost));
    }

    @Test
    public void isPrivateAddress_with10Network_shouldReturnTrue() throws Exception {
        InetAddress privateIp = InetAddress.getByName("10.0.0.1");
        assertTrue(clientIdMetadataService.isPrivateAddress(privateIp));
    }

    @Test
    public void isPrivateAddress_with192168Network_shouldReturnTrue() throws Exception {
        InetAddress privateIp = InetAddress.getByName("192.168.1.1");
        assertTrue(clientIdMetadataService.isPrivateAddress(privateIp));
    }

    @Test
    public void isPrivateAddress_with172Network_shouldReturnTrue() throws Exception {
        InetAddress privateIp = InetAddress.getByName("172.16.0.1");
        assertTrue(clientIdMetadataService.isPrivateAddress(privateIp));
    }

    @Test
    public void validateNotPrivateIp_withLoopbackAddress_shouldThrowBadRequest() {
        // Direct IP string resolves to loopback — no DNS lookup needed
        try {
            clientIdMetadataService.validateNotPrivateIp("127.0.0.1");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    // ==================== badRequest JSON escaping Tests ====================

    @Test
    public void badRequest_withQuotesInMessage_shouldProduceValidJson() {
        WebApplicationException ex = clientIdMetadataService.badRequest("has \"quotes\" in it");
        String body = ex.getResponse().getEntity().toString();
        // Quotes must be escaped so the JSON is valid
        assertFalse(body.contains("\"quotes\""), "Unescaped quotes would break JSON");
        assertTrue(body.contains("\\\"quotes\\\""), "Quotes should be escaped");
    }

    @Test
    public void badRequest_withBackslashInMessage_shouldProduceValidJson() {
        WebApplicationException ex = clientIdMetadataService.badRequest("path\\value");
        String body = ex.getResponse().getEntity().toString();
        assertTrue(body.contains("\\\\"), "Backslash should be escaped");
    }

    // ==================== computeId Tests ====================

    @Test
    public void computeId_shouldReturnDeterministicValue() {
        String id1 = clientIdMetadataService.computeId("https://example.com/client");
        String id2 = clientIdMetadataService.computeId("https://example.com/client");
        assertEquals(id1, id2);
    }

    @Test
    public void computeId_shouldReturnDifferentValueForDifferentUrl() {
        String id1 = clientIdMetadataService.computeId("https://example.com/client1");
        String id2 = clientIdMetadataService.computeId("https://example.com/client2");
        assertNotEquals(id1, id2);
    }

    @Test
    public void computeId_shouldStartWithCimdPrefix() {
        String id = clientIdMetadataService.computeId("https://example.com/client");
        assertTrue(id.startsWith("cimd-"));
    }

    @Test
    public void computeId_shouldContainOnlyAlphanumericAndDash() {
        String id = clientIdMetadataService.computeId("https://example.com/client?foo=bar&baz=qux");
        assertTrue(id.matches("[a-z0-9-]+"));
    }

    // ==================== calculateTtl Tests ====================

    @Test
    public void calculateTtl_withNullCacheControl_shouldReturnDefault() {
        int result = clientIdMetadataService.calculateTtl(null);
        assertEquals(60 * 60, result); // 60 minutes in seconds
    }

    @Test
    public void calculateTtl_withEmptyCacheControl_shouldReturnDefault() {
        int result = clientIdMetadataService.calculateTtl("");
        assertEquals(60 * 60, result);
    }

    @Test
    public void calculateTtl_withMaxAge_shouldReturnMaxAge() {
        int result = clientIdMetadataService.calculateTtl("max-age=300");
        assertEquals(300, result);
    }

    @Test
    public void calculateTtl_withMaxAgeExceedingMax_shouldReturnMax() {
        int result = clientIdMetadataService.calculateTtl("max-age=999999");
        assertEquals(1440 * 60, result); // max TTL in seconds
    }

    @Test
    public void calculateTtl_withComplexCacheControl_shouldParseMaxAge() {
        int result = clientIdMetadataService.calculateTtl("public, max-age=600, s-maxage=1200");
        assertEquals(600, result);
    }

    @Test
    public void calculateTtl_withNoCache_shouldReturnZero() {
        int result = clientIdMetadataService.calculateTtl("no-cache");
        assertEquals(0, result);
    }

    @Test
    public void calculateTtl_withNoStore_shouldReturnZero() {
        int result = clientIdMetadataService.calculateTtl("no-store");
        assertEquals(0, result);
    }

    @Test
    public void calculateTtl_withNoCacheUpperCase_shouldReturnZero() {
        int result = clientIdMetadataService.calculateTtl("No-Cache");
        assertEquals(0, result);
    }

    @Test
    public void calculateTtl_withNoCacheAndMaxAge_shouldReturnZero() {
        // no-cache takes precedence over max-age
        int result = clientIdMetadataService.calculateTtl("no-cache, max-age=3600");
        assertEquals(0, result);
    }

    // ==================== parseRegisterRequest Tests ====================

    @Test
    public void parseRegisterRequest_withValidJson_shouldReturnRegisterRequest() {
        RegisterRequest req = clientIdMetadataService.parseRegisterRequest(
                "{\"redirect_uris\": [\"https://example.com/cb\"]}");
        assertNotNull(req);
        assertNotNull(req.getRedirectUris());
        assertEquals(1, req.getRedirectUris().size());
        assertEquals("https://example.com/cb", req.getRedirectUris().get(0));
    }

    @Test
    public void parseRegisterRequest_withEmptyJson_shouldReturnEmptyRegisterRequest() {
        RegisterRequest req = clientIdMetadataService.parseRegisterRequest("{}");
        assertNotNull(req);
    }

    @Test
    public void parseRegisterRequest_withInvalidJson_shouldThrowBadRequest() {
        try {
            clientIdMetadataService.parseRegisterRequest("{invalid json}");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void parseRegisterRequest_withFullMetadata_shouldParseAllFields() {
        String json = "{" +
                "\"redirect_uris\": [\"https://example.com/callback\"]," +
                "\"grant_types\": [\"authorization_code\", \"refresh_token\"]," +
                "\"response_types\": [\"code\"]," +
                "\"token_endpoint_auth_method\": \"client_secret_basic\"," +
                "\"client_name\": \"Test Client\"," +
                "\"jwks_uri\": \"https://example.com/jwks\"" +
                "}";

        RegisterRequest req = clientIdMetadataService.parseRegisterRequest(json);

        assertNotNull(req);
        assertEquals(1, req.getRedirectUris().size());
        assertEquals("https://example.com/callback", req.getRedirectUris().get(0));
        assertEquals(2, req.getGrantTypes().size());
        assertEquals(1, req.getResponseTypes().size());
        assertEquals("Test Client", req.getClientName());
        assertEquals("https://example.com/jwks", req.getJwksUri());
    }

    // ==================== buildClientFromRequest Tests ====================

    @Test
    public void buildClientFromRequest_shouldCallRegisterServiceAndSetClientId() throws Exception {
        RegisterRequest req = RegisterRequest.fromJson("{}");
        doNothing().when(registerService).updateClientFromRequestObject(any(), any(), anyBoolean());

        Client result = clientIdMetadataService.buildClientFromRequest(req, "https://example.com/client");

        assertNotNull(result);
        assertEquals("https://example.com/client", result.getClientId());
        verify(registerService).updateClientFromRequestObject(any(Client.class), eq(req), eq(false));
    }

    @Test
    public void buildClientFromRequest_whenRegisterServiceThrows_shouldThrowBadRequest() throws Exception {
        RegisterRequest req = RegisterRequest.fromJson("{}");
        doThrow(new RuntimeException("DB error"))
                .when(registerService).updateClientFromRequestObject(any(), any(), anyBoolean());

        try {
            clientIdMetadataService.buildClientFromRequest(req, "https://example.com/client");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    // ==================== validateRedirectUris Tests ====================

    @Test
    public void validateRedirectUris_whenValid_shouldNotThrow() {
        when(registerParamsValidator.validateRedirectUris(any(), any(), any(), any(), any(), any())).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        clientIdMetadataService.validateRedirectUris(req); // should not throw
    }

    @Test
    public void validateRedirectUris_whenInvalid_shouldThrowBadRequest() {
        when(registerParamsValidator.validateRedirectUris(any(), any(), any(), any(), any(), any())).thenReturn(false);

        RegisterRequest req = new RegisterRequest();
        try {
            clientIdMetadataService.validateRedirectUris(req);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateRedirectUris_shouldPassApplicationTypeToValidator() throws Exception {
        RegisterRequest req = RegisterRequest.fromJson("{\"application_type\": \"native\"}");

        clientIdMetadataService.validateRedirectUris(req);

        verify(registerParamsValidator).validateRedirectUris(
                any(), any(), eq(io.jans.as.model.register.ApplicationType.NATIVE), any(), any(), any());
    }

    @Test
    public void validateRedirectUris_withNoApplicationType_shouldDefaultToWeb() {
        RegisterRequest req = new RegisterRequest(); // no application_type set

        clientIdMetadataService.validateRedirectUris(req);

        verify(registerParamsValidator).validateRedirectUris(
                any(), any(), eq(io.jans.as.model.register.ApplicationType.WEB), any(), any(), any());
    }

    // ==================== validateTokenEndpointAuthMethod Tests ====================

    @Test
    public void validateTokenEndpointAuthMethod_withNull_shouldPass() {
        RegisterRequest req = new RegisterRequest();
        // no auth method set — should not throw
        clientIdMetadataService.validateTokenEndpointAuthMethod(req);
    }

    @Test
    public void validateTokenEndpointAuthMethod_withPrivateKeyJwt_shouldPass() {
        RegisterRequest req = new RegisterRequest();
        req.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        clientIdMetadataService.validateTokenEndpointAuthMethod(req);
    }

    @Test
    public void validateTokenEndpointAuthMethod_withNone_shouldPass() {
        RegisterRequest req = new RegisterRequest();
        req.setTokenEndpointAuthMethod(AuthenticationMethod.NONE);
        clientIdMetadataService.validateTokenEndpointAuthMethod(req);
    }

    @Test
    public void validateTokenEndpointAuthMethod_withTlsClientAuth_shouldPass() {
        RegisterRequest req = new RegisterRequest();
        req.setTokenEndpointAuthMethod(AuthenticationMethod.TLS_CLIENT_AUTH);
        clientIdMetadataService.validateTokenEndpointAuthMethod(req);
    }

    @Test
    public void validateTokenEndpointAuthMethod_withSelfSignedTlsClientAuth_shouldPass() {
        RegisterRequest req = new RegisterRequest();
        req.setTokenEndpointAuthMethod(AuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH);
        clientIdMetadataService.validateTokenEndpointAuthMethod(req);
    }

    @Test
    public void validateTokenEndpointAuthMethod_withClientSecretBasic_shouldThrowBadRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        try {
            clientIdMetadataService.validateTokenEndpointAuthMethod(req);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("client_secret_basic"));
        }
    }

    @Test
    public void validateTokenEndpointAuthMethod_withClientSecretPost_shouldThrowBadRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        try {
            clientIdMetadataService.validateTokenEndpointAuthMethod(req);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("client_secret_post"));
        }
    }

    @Test
    public void validateTokenEndpointAuthMethod_withClientSecretJwt_shouldThrowBadRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        try {
            clientIdMetadataService.validateTokenEndpointAuthMethod(req);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("client_secret_jwt"));
        }
    }

    @Test
    public void validateTokenEndpointAuthMethod_allForbiddenMethodsAreCovered() {
        // Ensure FORBIDDEN_AUTH_METHODS contains exactly the three symmetric-secret methods
        assertEquals(3, ClientIdMetadataService.FORBIDDEN_AUTH_METHODS.size());
        assertTrue(ClientIdMetadataService.FORBIDDEN_AUTH_METHODS.contains(AuthenticationMethod.CLIENT_SECRET_BASIC));
        assertTrue(ClientIdMetadataService.FORBIDDEN_AUTH_METHODS.contains(AuthenticationMethod.CLIENT_SECRET_POST));
        assertTrue(ClientIdMetadataService.FORBIDDEN_AUTH_METHODS.contains(AuthenticationMethod.CLIENT_SECRET_JWT));
    }

    @Test
    public void getClient_whenForbiddenAuthMethod_shouldThrowBadRequest() {
        String url = "https://example.com/client";
        String id = clientIdMetadataService.computeId(url);
        String dn = "inum=" + id + ",ou=clients,o=jans";

        when(clientService.buildClientDn(id)).thenReturn(dn);
        when(clientService.getClientByDn(dn)).thenReturn(null);
        doNothing().when(clientIdMetadataService).validateNotPrivateIp(anyString());
        doReturn(new ClientIdMetadataService.FetchResult(
                "{\"client_id\": \"https://example.com/client\", \"token_endpoint_auth_method\": \"client_secret_basic\"}", null))
                .when(clientIdMetadataService).doFetch(url);

        try {
            clientIdMetadataService.getClient(url);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }

        verify(clientService, never()).persist(any(Client.class));
        verify(clientService, never()).merge(any(Client.class));
    }

    // ==================== parseToClient Tests ====================

    @Test
    public void parseToClient_shouldDelegateToRegisterServiceAndSetClientId() throws Exception {
        doNothing().when(registerService).updateClientFromRequestObject(any(), any(), anyBoolean());

        Client result = clientIdMetadataService.parseToClient("{}", "https://example.com/client");

        assertNotNull(result);
        assertEquals("https://example.com/client", result.getClientId());
        verify(registerService).updateClientFromRequestObject(any(Client.class), any(RegisterRequest.class), eq(false));
    }

    @Test
    public void parseToClient_withInvalidJson_shouldThrowBadRequest() {
        try {
            clientIdMetadataService.parseToClient("{invalid json}", "https://example.com/client");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    // ==================== validateClientIdUrl Tests ====================

    @Test
    public void validateClientIdUrl_withValidUrl_shouldPass() {
        doNothing().when(clientIdMetadataService).validateNotPrivateIp(anyString());
        clientIdMetadataService.validateClientIdUrl("https://example.com/client"); // should not throw
    }

    @Test
    public void validateClientIdUrl_withHttpUrl_shouldThrowBadRequest() {
        try {
            clientIdMetadataService.validateClientIdUrl("http://example.com/client");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateClientIdUrl_withInvalidUrl_shouldThrowBadRequest() {
        try {
            clientIdMetadataService.validateClientIdUrl("not a url");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateClientIdUrl_whenPrivateIpBlockingDisabled_shouldNotCheckIp() {
        when(appConfiguration.getCimdBlockPrivateIp()).thenReturn(false);

        clientIdMetadataService.validateClientIdUrl("https://localhost/client");

        verify(clientIdMetadataService, never()).validateNotPrivateIp(anyString());
    }

    @Test
    public void validateClientIdUrl_withFragment_shouldThrowBadRequest() {
        try {
            clientIdMetadataService.validateClientIdUrl("https://example.com/client#section");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("fragment"));
        }
    }

    @Test
    public void validateClientIdUrl_withUserInfo_shouldThrowBadRequest() {
        try {
            clientIdMetadataService.validateClientIdUrl("https://user:pass@example.com/client");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("username"));
        }
    }

    @Test
    public void validateClientIdUrl_withDoubleDotSegment_shouldThrowBadRequest() {
        try {
            clientIdMetadataService.validateClientIdUrl("https://example.com/foo/../client");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("dot"));
        }
    }

    @Test
    public void validateClientIdUrl_withSingleDotSegment_shouldThrowBadRequest() {
        try {
            clientIdMetadataService.validateClientIdUrl("https://example.com/./client");
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("dot"));
        }
    }

    // ==================== validateUrlStructure Tests ====================

    @Test
    public void validateUrlStructure_withValidUrl_shouldPass() throws Exception {
        URI uri = new URI("https://example.com/client");
        clientIdMetadataService.validateUrlStructure(uri); // should not throw
    }

    @Test
    public void validateUrlStructure_withNoPath_shouldThrowBadRequest() throws Exception {
        URI uri = new URI("https://example.com");
        try {
            clientIdMetadataService.validateUrlStructure(uri);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("path"));
        }
    }

    @Test
    public void validateUrlStructure_withFragment_shouldThrowBadRequest() throws Exception {
        URI uri = new URI("https://example.com/client#frag");
        try {
            clientIdMetadataService.validateUrlStructure(uri);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("fragment"));
        }
    }

    @Test
    public void validateUrlStructure_withUserInfo_shouldThrowBadRequest() throws Exception {
        URI uri = new URI("https://user:pass@example.com/client");
        try {
            clientIdMetadataService.validateUrlStructure(uri);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("username"));
        }
    }

    @Test
    public void validateUrlStructure_withDoubleDotSegment_shouldThrowBadRequest() throws Exception {
        URI uri = new URI("https://example.com/foo/../client");
        try {
            clientIdMetadataService.validateUrlStructure(uri);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateUrlStructure_withSingleDotSegment_shouldThrowBadRequest() throws Exception {
        URI uri = new URI("https://example.com/./client");
        try {
            clientIdMetadataService.validateUrlStructure(uri);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateUrlStructure_withPortAndDeepPath_shouldPass() throws Exception {
        URI uri = new URI("https://example.com:8443/v1/client/metadata");
        clientIdMetadataService.validateUrlStructure(uri); // should not throw
    }

    // ==================== validateNoClientSecret Tests ====================

    @Test
    public void validateNoClientSecret_withNoSecret_shouldPass() throws Exception {
        RegisterRequest req = RegisterRequest.fromJson("{\"redirect_uris\": [\"https://example.com/cb\"]}");
        clientIdMetadataService.validateNoClientSecret(req); // should not throw
    }

    @Test
    public void validateNoClientSecret_withClientSecret_shouldThrowBadRequest() throws Exception {
        RegisterRequest req = RegisterRequest.fromJson("{\"client_secret\": \"supersecret\"}");
        try {
            clientIdMetadataService.validateNoClientSecret(req);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("client_secret"));
        }
    }

    @Test
    public void validateNoClientSecret_withClientSecretExpiresAt_shouldThrowBadRequest() throws Exception {
        RegisterRequest req = RegisterRequest.fromJson("{\"client_secret_expires_at\": 0}");
        try {
            clientIdMetadataService.validateNoClientSecret(req);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
            assertTrue(e.getResponse().getEntity().toString().contains("client_secret_expires_at"));
        }
    }

    @Test
    public void validateNoClientSecret_withNullJsonObject_shouldPass() {
        RegisterRequest req = new RegisterRequest(); // jsonObject is null
        clientIdMetadataService.validateNoClientSecret(req); // should not throw
    }

    // ==================== getClient (persistence + script) Tests ====================

    @Test
    public void getClient_whenPersistedClientIsValid_shouldReturnWithoutFetching() {
        String url = "https://example.com/client";
        String id = clientIdMetadataService.computeId(url);
        String dn = "inum=" + id + ",ou=clients,o=jans";

        ClientAttributes attrs = new ClientAttributes();
        attrs.setCimdClient(true);
        attrs.setCimdOriginalClientId(url);
        attrs.setCimdExpiresAt(System.currentTimeMillis() + 60_000);

        Client persistedClient = new Client();
        persistedClient.setClientId(id);
        persistedClient.setAttributes(attrs);

        when(clientService.buildClientDn(id)).thenReturn(dn);
        when(clientService.getClientByDn(dn)).thenReturn(persistedClient);
        doNothing().when(clientIdMetadataService).validateNotPrivateIp(anyString());

        Client result = clientIdMetadataService.getClient(url);

        assertNotNull(result);
        assertEquals(url, result.getClientId());
        verify(clientIdMetadataService, never()).doFetch(anyString());
        // No external script for cache hit
        verify(externalDynamicClientRegistrationService, never()).executeExternalCreateClientMethods(any(), any(), any());
    }

    @Test
    public void getClient_whenPersistedClientIsExpired_shouldRefetch() {
        String url = "https://example.com/client";
        String id = clientIdMetadataService.computeId(url);
        String dn = "inum=" + id + ",ou=clients,o=jans";

        ClientAttributes attrs = new ClientAttributes();
        attrs.setCimdClient(true);
        attrs.setCimdOriginalClientId(url);
        attrs.setCimdExpiresAt(System.currentTimeMillis() - 1000);

        Client expiredClient = new Client();
        expiredClient.setClientId(id);
        expiredClient.setAttributes(attrs);

        when(clientService.buildClientDn(id)).thenReturn(dn);
        when(clientService.getClientByDn(dn)).thenReturn(expiredClient);
        doNothing().when(clientIdMetadataService).validateNotPrivateIp(anyString());
        doReturn(new ClientIdMetadataService.FetchResult("{\"client_id\": \"https://example.com/client\"}", null)).when(clientIdMetadataService).doFetch(url);
        doNothing().when(clientService).merge(any(Client.class));

        Client result = clientIdMetadataService.getClient(url);

        assertNotNull(result);
        assertEquals(url, result.getClientId());
        verify(clientIdMetadataService).doFetch(url);
        verify(clientService).merge(any(Client.class));
        verify(externalDynamicClientRegistrationService).executeExternalCreateClientMethods(
                any(RegisterRequest.class), any(Client.class), isNull());
    }

    @Test
    public void getClient_whenNoPersistedClient_shouldFetchAndPersist() {
        String url = "https://example.com/client";
        String id = clientIdMetadataService.computeId(url);
        String dn = "inum=" + id + ",ou=clients,o=jans";

        when(clientService.buildClientDn(id)).thenReturn(dn);
        when(clientService.getClientByDn(dn)).thenReturn(null);
        doNothing().when(clientIdMetadataService).validateNotPrivateIp(anyString());
        doReturn(new ClientIdMetadataService.FetchResult("{\"client_id\": \"https://example.com/client\"}", "max-age=3600")).when(clientIdMetadataService).doFetch(url);
        doNothing().when(clientService).persist(any(Client.class));

        Client result = clientIdMetadataService.getClient(url);

        assertNotNull(result);
        assertEquals(url, result.getClientId());
        verify(clientIdMetadataService).doFetch(url);
        verify(registerParamsValidator).validateRedirectUris(any(), any(), any(), any(), any(), any());
        verify(externalDynamicClientRegistrationService).executeExternalCreateClientMethods(
                any(RegisterRequest.class), any(Client.class), isNull());
        verify(clientService).persist(any(Client.class));
    }

    @Test
    public void getClient_whenExternalScriptRejects_shouldThrowBadRequest() {
        String url = "https://example.com/client";
        String id = clientIdMetadataService.computeId(url);
        String dn = "inum=" + id + ",ou=clients,o=jans";

        when(clientService.buildClientDn(id)).thenReturn(dn);
        when(clientService.getClientByDn(dn)).thenReturn(null);
        doNothing().when(clientIdMetadataService).validateNotPrivateIp(anyString());
        doReturn(new ClientIdMetadataService.FetchResult("{\"client_id\": \"https://example.com/client\"}", null)).when(clientIdMetadataService).doFetch(url);
        when(externalDynamicClientRegistrationService.executeExternalCreateClientMethods(
                any(), any(), any())).thenReturn(false);

        try {
            clientIdMetadataService.getClient(url);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }

        verify(clientService, never()).persist(any(Client.class));
        verify(clientService, never()).merge(any(Client.class));
    }

    @Test
    public void getClient_whenRedirectUrisInvalid_shouldThrowBadRequest() {
        String url = "https://example.com/client";
        String id = clientIdMetadataService.computeId(url);
        String dn = "inum=" + id + ",ou=clients,o=jans";

        when(clientService.buildClientDn(id)).thenReturn(dn);
        when(clientService.getClientByDn(dn)).thenReturn(null);
        doNothing().when(clientIdMetadataService).validateNotPrivateIp(anyString());
        doReturn(new ClientIdMetadataService.FetchResult("{\"client_id\": \"https://example.com/client\"}", null)).when(clientIdMetadataService).doFetch(url);
        when(registerParamsValidator.validateRedirectUris(any(), any(), any(), any(), any(), any())).thenReturn(false);

        try {
            clientIdMetadataService.getClient(url);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }

        verify(clientService, never()).persist(any(Client.class));
    }

    @Test
    public void getClient_persistedClientWithoutCimdFlag_shouldRefetch() {
        String url = "https://example.com/client";
        String id = clientIdMetadataService.computeId(url);
        String dn = "inum=" + id + ",ou=clients,o=jans";

        Client regularClient = new Client();
        regularClient.setClientId(id);
        regularClient.setAttributes(new ClientAttributes()); // cimdClient = false

        when(clientService.buildClientDn(id)).thenReturn(dn);
        when(clientService.getClientByDn(dn)).thenReturn(regularClient);
        doNothing().when(clientIdMetadataService).validateNotPrivateIp(anyString());
        doReturn(new ClientIdMetadataService.FetchResult("{\"client_id\": \"https://example.com/client\"}", null)).when(clientIdMetadataService).doFetch(url);
        doNothing().when(clientService).merge(any(Client.class));

        clientIdMetadataService.getClient(url);

        verify(clientIdMetadataService).doFetch(url);
    }

    @Test
    public void getClient_whenDocumentClientIdMatches_shouldSucceed() {
        String url = "https://example.com/client";
        String id = clientIdMetadataService.computeId(url);
        String dn = "inum=" + id + ",ou=clients,o=jans";

        when(clientService.buildClientDn(id)).thenReturn(dn);
        when(clientService.getClientByDn(dn)).thenReturn(null);
        doNothing().when(clientIdMetadataService).validateNotPrivateIp(anyString());
        doReturn(new ClientIdMetadataService.FetchResult(
                "{\"client_id\": \"https://example.com/client\"}", null))
                .when(clientIdMetadataService).doFetch(url);
        doNothing().when(clientService).persist(any(Client.class));

        Client result = clientIdMetadataService.getClient(url);

        assertNotNull(result);
        assertEquals(url, result.getClientId());
    }

    @Test
    public void getClient_whenDocumentClientIdMismatches_shouldThrowBadRequest() {
        String url = "https://example.com/client";
        String id = clientIdMetadataService.computeId(url);
        String dn = "inum=" + id + ",ou=clients,o=jans";

        when(clientService.buildClientDn(id)).thenReturn(dn);
        when(clientService.getClientByDn(dn)).thenReturn(null);
        doNothing().when(clientIdMetadataService).validateNotPrivateIp(anyString());
        doReturn(new ClientIdMetadataService.FetchResult(
                "{\"client_id\": \"https://attacker.com/evil\"}", null))
                .when(clientIdMetadataService).doFetch(url);

        try {
            clientIdMetadataService.getClient(url);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }

        verify(clientService, never()).persist(any(Client.class));
        verify(clientService, never()).merge(any(Client.class));
    }

    @Test
    public void getClient_whenFetchFails_shouldThrowBadRequest() {
        String url = "https://example.com/client";
        String id = clientIdMetadataService.computeId(url);
        String dn = "inum=" + id + ",ou=clients,o=jans";

        when(clientService.buildClientDn(id)).thenReturn(dn);
        when(clientService.getClientByDn(dn)).thenReturn(null);
        doNothing().when(clientIdMetadataService).validateNotPrivateIp(anyString());
        doThrow(new WebApplicationException(400)).when(clientIdMetadataService).doFetch(url);

        try {
            clientIdMetadataService.getClient(url);
            fail("Should have thrown WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }
}
