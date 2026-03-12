package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.as.server.model.authorize.IdTokenMember;
import io.jans.as.server.model.authorize.JwtAuthorizationRequest;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.par.ws.rs.ParService;
import io.jans.as.server.service.*;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.mockito.*;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AuthzRequestServiceTest {

    @InjectMocks
    private AuthzRequestService authzRequestService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private AuthorizeRestWebServiceValidator authorizeRestWebServiceValidator;

    @Mock
    private ParService parService;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private ScopeChecker scopeChecker;

    @Mock
    private RequestParameterService requestParameterService;

    @Mock
    private WebKeysConfiguration webKeysConfiguration;

    @Mock
    private ClientService clientService;

    @Mock
    private RedirectionUriService redirectionUriService;

    @Mock
    private ExternalAuthenticationService externalAuthenticationService;

    @Spy
    private AcrService acrService;

    @Test
    public void checkIdTokenMember_whenAcrIsAliased_shouldUseMappedAcr() {
        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("urn:openbanking:psd2:sca");

        Map<String, String> acrMappings = new HashMap<>();
        acrMappings.put("urn:openbanking:psd2:sca", "psd2sca");
        acrMappings.put("urn:openbanking:psd2:ca", "psd2sca");

        when(appConfiguration.getAcrMappings()).thenReturn(acrMappings);

        IdTokenMember idTokenMember = new IdTokenMember(new JSONObject("{\n" +
                "      \"openbanking_intent_id\": {\n" +
                "        \"value\": \"urn-alphabank-intent-58923\",\n" +
                "        \"essential\": \"true\"\n" +
                "      },\n" +
                "      \"acr\": {\n" +
                "        \"essential\": \"true\",\n" +
                "        \"values\": [\n" +
                "          \"urn:openbanking:psd2:sca\",\n" +
                "          \"urn:openbanking:psd2:ca\"\n" +
                "        ]\n" +
                "      }\n" +
                "    }"));

        final RedirectUriResponse redirectUriResponse = new RedirectUriResponse(mock(RedirectUri.class), "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class));

        JwtAuthorizationRequest request = mock(JwtAuthorizationRequest.class);
        when(request.getIdTokenMember()).thenReturn(idTokenMember);

        authzRequestService.checkIdTokenMember(authzRequest, redirectUriResponse, new User(), request);

        assertEquals(authzRequest.getAcrValues(), "psd2sca");
    }

    @Test
    public void setAcrsIfNeeded_whenAcrsAreNotSetButDefaultAcrsAreConfigured_shouldSetDefaultAcrs() {
        Client client = new Client();
        client.setDefaultAcrValues(new String[]{"passkey"});

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setClient(client);

        authzRequestService.setAcrsIfNeeded(authzRequest);
        assertEquals(authzRequest.getAcrValues(), "passkey");
    }

    @Test
    public void setAcrsIfNeeded_whenAcrsHasEnoughLevel_shouldRaiseNoError() {
        when(externalAuthenticationService.acrToLevelMapping()).thenReturn(new HashMap<String, Integer>() {{
            put("basic", 1);
            put("otp", 5);
            put("u2f", 10);
            put("super_gluu", 11);
            put("passkey", 20);
            put("usb_fido_key", 30);
        }});

        Client client = new Client();
        client.getAttributes().setMinimumAcrLevel(14);

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("passkey");
        authzRequest.setClient(client);

        authzRequestService.setAcrsIfNeeded(authzRequest);
        assertEquals(authzRequest.getAcrValues(), "passkey");
    }

    @Test
    public void setAcrsIfNeeded_whenAcrsHasNoEnoughLevel_shouldRaiseError() {
        when(externalAuthenticationService.acrToLevelMapping()).thenReturn(new HashMap<String, Integer>() {{
            put("basic", 1);
            put("otp", 5);
            put("u2f", 10);
            put("super_gluu", 11);
            put("passkey", 20);
            put("usb_fido_key", 30);
        }});

        Client client = new Client();
        client.getAttributes().setMinimumAcrLevel(14);

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("super_gluu");
        authzRequest.setClient(client);

        try {
            authzRequestService.setAcrsIfNeeded(authzRequest);
        } catch (WebApplicationException e) {
            return; // successfully got error
        }

        fail("Failed to throw error.");
    }

    @Test
    public void setAcrsIfNeeded_whenAcrsHasNoEnoughLevelButAutoResolveIsTrue_shouldRaiseNoError() {
        when(externalAuthenticationService.acrToLevelMapping()).thenReturn(new HashMap<String, Integer>() {{
            put("basic", 1);
            put("otp", 5);
            put("u2f", 10);
            put("super_gluu", 11);
            put("passkey", 20);
            put("usb_fido_key", 30);
        }});

        Client client = new Client();
        client.getAttributes().setMinimumAcrLevel(14);
        client.getAttributes().setMinimumAcrLevelAutoresolve(true);

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("super_gluu");
        authzRequest.setClient(client);

        authzRequestService.setAcrsIfNeeded(authzRequest);

        assertEquals(authzRequest.getAcrValues(), "passkey");
        assertTrue(externalAuthenticationService.acrToLevelMapping().get(authzRequest.getAcrValues()) > 14);
    }

    @Test
    public void setAcrsIfNeeded_whenAcrsHasNoEnoughLevelButAutoResolveIsTrueAndPriorityListSet_shouldHaveAcrFromPriorityListSet() {
        when(externalAuthenticationService.acrToLevelMapping()).thenReturn(new HashMap<String, Integer>() {{
            put("basic", 1);
            put("otp", 5);
            put("u2f", 10);
            put("super_gluu", 11);
            put("passkey", 20);
            put("usb_fido_key", 30);
        }});

        Client client = new Client();
        client.getAttributes().setMinimumAcrLevel(14);
        client.getAttributes().setMinimumAcrLevelAutoresolve(true);
        client.getAttributes().setMinimumAcrPriorityList(Collections.singletonList("usb_fido_key"));

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("super_gluu");
        authzRequest.setClient(client);

        authzRequestService.setAcrsIfNeeded(authzRequest);

        assertEquals(authzRequest.getAcrValues(), "usb_fido_key");
    }

    @Test
    public void setAcrsIfNeeded_whenAcrsHasNoEnoughLevelButAutoResolveIsTrueAndPriorityListSet_shouldGetErrorIfPriorityListClashWithMinimalLevel() {
        when(externalAuthenticationService.acrToLevelMapping()).thenReturn(new HashMap<String, Integer>() {{
            put("basic", 1);
            put("otp", 5);
            put("u2f", 10);
            put("super_gluu", 11);
            put("passkey", 20);
            put("usb_fido_key", 30);
        }});

        Client client = new Client();
        client.getAttributes().setMinimumAcrLevel(14);
        client.getAttributes().setMinimumAcrLevelAutoresolve(true);
        client.getAttributes().setMinimumAcrPriorityList(Collections.singletonList("u2f"));

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("super_gluu");
        authzRequest.setClient(client);

        try {
            authzRequestService.setAcrsIfNeeded(authzRequest);
        } catch (WebApplicationException e) {
            return; // successfully got error
        }

        fail("Must fail because priority list has acr which has level lower then minumumAcrLevel");
    }

    @Test
    public void addDeviceSecretToSession_withoutUnabledConfiguration_shouldNotGenerateDeviceSecret() {
        when(appConfiguration.getReturnDeviceSecretFromAuthzEndpoint()).thenReturn(false);

        Client client = new Client();
        client.setGrantTypes(new GrantType[]{GrantType.AUTHORIZATION_CODE, GrantType.TOKEN_EXCHANGE});

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setScope("openid device_sso");
        authzRequest.setRedirectUriResponse(new RedirectUriResponse(mock(RedirectUri.class), "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class)));
        authzRequest.setClient(client);

        SessionId sessionId = new SessionId();

        authzRequestService.addDeviceSecretToSession(authzRequest, sessionId);
        assertTrue(sessionId.getDeviceSecrets().isEmpty());
    }

    @Test
    public void addDeviceSecretToSession_withoutDeviceSsoScope_shouldNotGenerateDeviceSecret() {
        when(appConfiguration.getReturnDeviceSecretFromAuthzEndpoint()).thenReturn(true);

        Client client = new Client();
        client.setGrantTypes(new GrantType[]{GrantType.AUTHORIZATION_CODE, GrantType.TOKEN_EXCHANGE});

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setScope("openid");
        authzRequest.setRedirectUriResponse(new RedirectUriResponse(mock(RedirectUri.class), "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class)));
        authzRequest.setClient(client);

        SessionId sessionId = new SessionId();

        authzRequestService.addDeviceSecretToSession(authzRequest, sessionId);
        assertTrue(sessionId.getDeviceSecrets().isEmpty());
    }

    @Test
    public void addDeviceSecretToSession_withDeviceSsoScope_shouldGenerateDeviceSecret() {
        when(appConfiguration.getReturnDeviceSecretFromAuthzEndpoint()).thenReturn(true);

        Client client = new Client();
        client.setGrantTypes(new GrantType[]{GrantType.AUTHORIZATION_CODE, GrantType.TOKEN_EXCHANGE});

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setRedirectUriResponse(new RedirectUriResponse(mock(RedirectUri.class), "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class)));
        authzRequest.setScope("openid device_sso");
        authzRequest.setClient(client);

        SessionId sessionId = new SessionId();

        assertTrue(sessionId.getDeviceSecrets().isEmpty());
        authzRequestService.addDeviceSecretToSession(authzRequest, sessionId);
        assertEquals(sessionId.getDeviceSecrets().size(), 1);
        assertTrue(StringUtils.isNotBlank(sessionId.getDeviceSecrets().get(0)));
    }

    @Test
    public void addDeviceSecretToSession_withClientWithoutTokenExchangeGrantType_shouldNotGenerateDeviceSecret() {
        when(appConfiguration.getReturnDeviceSecretFromAuthzEndpoint()).thenReturn(true);

        Client client = new Client();
        client.setGrantTypes(new GrantType[]{GrantType.AUTHORIZATION_CODE});

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setRedirectUriResponse(new RedirectUriResponse(mock(RedirectUri.class), "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class)));
        authzRequest.setScope("openid device_sso");
        authzRequest.setClient(client);

        SessionId sessionId = new SessionId();

        assertTrue(sessionId.getDeviceSecrets().isEmpty());
        authzRequestService.addDeviceSecretToSession(authzRequest, sessionId);
        assertTrue(sessionId.getDeviceSecrets().isEmpty());
    }

    /**
     * Security test: Verifies that processRequestObject rejects JWE request objects
     * when forceSignedRequestObject=true and the nested JWT has SignatureAlgorithm.NONE.
     *
     * This tests the fix for the vulnerability where:
     * - For JWE, we now extract signature algorithm from nested JWT header (not outer JWE header)
     * - Both null and NONE algorithms are rejected when forceSignedRequestObject=true
     */
    @Test
    public void processRequestObject_whenForceSignedRequestObjectEnabledAndNestedJwtHasNoneAlgorithm_shouldRejectRequest() {
        // Setup: forceSignedRequestObject = true
        when(appConfiguration.getForceSignedRequestObject()).thenReturn(true);

        // Create mock exception to be thrown
        WebApplicationException expectedException = new WebApplicationException("A signed request object is required");
        when(authorizeRestWebServiceValidator.createInvalidJwtRequestException(any(), anyString()))
                .thenReturn(expectedException);

        // Create mock JwtAuthorizationRequest representing a JWE with nested JWT using NONE algorithm
        JwtAuthorizationRequest mockJwtRequest = mock(JwtAuthorizationRequest.class);
        when(mockJwtRequest.isJws()).thenReturn(false); // It's a JWE, not JWS
        // Use lenient for stubs that may not be called depending on code path
        lenient().when(mockJwtRequest.isJwe()).thenReturn(true);
        lenient().when(mockJwtRequest.getScopes()).thenReturn(Collections.emptyList());
        when(mockJwtRequest.getRedirectUri()).thenReturn(null);

        // Mock nested JWT with NONE signature algorithm
        Jwt mockNestedJwt = mock(Jwt.class);
        JwtHeader mockHeader = mock(JwtHeader.class);
        when(mockHeader.getSignatureAlgorithm()).thenReturn(SignatureAlgorithm.NONE);
        when(mockNestedJwt.getHeader()).thenReturn(mockHeader);
        when(mockJwtRequest.getNestedJwt()).thenReturn(mockNestedJwt);

        // Setup AuthzRequest
        Client client = new Client();
        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setRequest("dummy-jwe-token");
        authzRequest.setRedirectUriResponse(new RedirectUriResponse(
                mock(RedirectUri.class), "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class)));
        Set<String> scopes = new HashSet<>();
        scopes.add("openid");

        // Use MockedStatic to mock the static createJwtRequest method
        try (MockedStatic<JwtAuthorizationRequest> mockedStatic = Mockito.mockStatic(JwtAuthorizationRequest.class)) {
            mockedStatic.when(() -> JwtAuthorizationRequest.createJwtRequest(
                    anyString(), any(), any(), any(), any(), any()
            )).thenReturn(mockJwtRequest);

            // Call processRequestObject and expect rejection
            try {
                authzRequestService.processRequestObject(authzRequest, client, scopes, null);
                fail("Should have thrown WebApplicationException for NONE algorithm with forceSignedRequestObject=true");
            } catch (WebApplicationException e) {
                // Expected - request should be rejected
                assertTrue(true, "Request correctly rejected for NONE signature algorithm");
            }
        }
    }

    /**
     * Security test: Verifies that processRequestObject accepts JWE request objects
     * when forceSignedRequestObject=true and the nested JWT has a valid signature algorithm (RS256).
     */
    @Test
    public void processRequestObject_whenForceSignedRequestObjectEnabledAndNestedJwtHasRS256Algorithm_shouldAcceptRequest() {
        // Setup: forceSignedRequestObject = true
        when(appConfiguration.getForceSignedRequestObject()).thenReturn(true);
        lenient().when(appConfiguration.isFapi()).thenReturn(false);

        // Create mock JwtAuthorizationRequest representing a JWE with properly signed nested JWT
        JwtAuthorizationRequest mockJwtRequest = mock(JwtAuthorizationRequest.class);
        when(mockJwtRequest.isJws()).thenReturn(false); // It's a JWE, not JWS
        lenient().when(mockJwtRequest.isJwe()).thenReturn(true);
        when(mockJwtRequest.getScopes()).thenReturn(Collections.emptyList());
        when(mockJwtRequest.getRedirectUri()).thenReturn(null);
        when(mockJwtRequest.getPrompts()).thenReturn(Collections.emptyList());
        lenient().when(mockJwtRequest.getState()).thenReturn("test-state");

        // Mock nested JWT with RS256 signature algorithm (valid)
        Jwt mockNestedJwt = mock(Jwt.class);
        JwtHeader mockHeader = mock(JwtHeader.class);
        when(mockHeader.getSignatureAlgorithm()).thenReturn(SignatureAlgorithm.RS256);
        when(mockNestedJwt.getHeader()).thenReturn(mockHeader);
        when(mockJwtRequest.getNestedJwt()).thenReturn(mockNestedJwt);

        // Setup AuthzRequest
        Client client = new Client();
        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setRequest("dummy-jwe-token");
        authzRequest.setRedirectUriResponse(new RedirectUriResponse(
                mock(RedirectUri.class), "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class)));
        Set<String> scopes = new HashSet<>();
        scopes.add("openid");

        // Use MockedStatic to mock the static createJwtRequest method
        try (MockedStatic<JwtAuthorizationRequest> mockedStatic = Mockito.mockStatic(JwtAuthorizationRequest.class)) {
            mockedStatic.when(() -> JwtAuthorizationRequest.createJwtRequest(
                    anyString(), any(), any(), any(), any(), any()
            )).thenReturn(mockJwtRequest);

            // Call processRequestObject - should NOT throw exception for RS256
            try {
                authzRequestService.processRequestObject(authzRequest, client, scopes, null);
                // If we get here without exception, the request was accepted
                assertTrue(true, "Request correctly accepted for RS256 signature algorithm");
            } catch (WebApplicationException e) {
                // Should not happen for valid RS256 signature
                fail("Should NOT have thrown WebApplicationException for RS256 algorithm: " + e.getMessage());
            }
        }
    }

    /**
     * Security test: Verifies that processRequestObject rejects JWS request objects
     * when forceSignedRequestObject=true and the algorithm is "none".
     */
    @Test
    public void processRequestObject_whenForceSignedRequestObjectEnabledAndJwsHasNoneAlgorithm_shouldRejectRequest() {
        // Setup: forceSignedRequestObject = true
        when(appConfiguration.getForceSignedRequestObject()).thenReturn(true);

        // Create mock exception to be thrown
        WebApplicationException expectedException = new WebApplicationException("A signed request object is required");
        when(authorizeRestWebServiceValidator.createInvalidJwtRequestException(any(), anyString()))
                .thenReturn(expectedException);

        // Create mock JwtAuthorizationRequest representing a JWS with NONE algorithm
        JwtAuthorizationRequest mockJwtRequest = mock(JwtAuthorizationRequest.class);
        when(mockJwtRequest.isJws()).thenReturn(true); // It's a JWS
        lenient().when(mockJwtRequest.isJwe()).thenReturn(false);
        when(mockJwtRequest.getAlgorithm()).thenReturn("none"); // NONE algorithm
        lenient().when(mockJwtRequest.getScopes()).thenReturn(Collections.emptyList());
        when(mockJwtRequest.getRedirectUri()).thenReturn(null);

        // Setup AuthzRequest
        Client client = new Client();
        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setRequest("dummy-jws-token");
        authzRequest.setRedirectUriResponse(new RedirectUriResponse(
                mock(RedirectUri.class), "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class)));
        Set<String> scopes = new HashSet<>();
        scopes.add("openid");

        // Use MockedStatic to mock the static createJwtRequest method
        try (MockedStatic<JwtAuthorizationRequest> mockedStatic = Mockito.mockStatic(JwtAuthorizationRequest.class)) {
            mockedStatic.when(() -> JwtAuthorizationRequest.createJwtRequest(
                    anyString(), any(), any(), any(), any(), any()
            )).thenReturn(mockJwtRequest);

            // Call processRequestObject and expect rejection
            try {
                authzRequestService.processRequestObject(authzRequest, client, scopes, null);
                fail("Should have thrown WebApplicationException for 'none' algorithm with forceSignedRequestObject=true");
            } catch (WebApplicationException e) {
                // Expected - request should be rejected
                assertTrue(true, "Request correctly rejected for 'none' signature algorithm");
            }
        }
    }

    @Test
    public void signatureAlgorithmFromString_withJweEncryptionAlgorithm_shouldReturnNull() {
        // Verify that JWE encryption algorithms are not recognized as signature algorithms
        // This is important for the forceSignedRequestObject security fix
        assertNull(SignatureAlgorithm.fromString("RSA-OAEP"), "RSA-OAEP should return null");
        assertNull(SignatureAlgorithm.fromString("RSA1_5"), "RSA1_5 should return null");
        assertNull(SignatureAlgorithm.fromString("A128KW"), "A128KW should return null");
        assertNull(SignatureAlgorithm.fromString("A256KW"), "A256KW should return null");
        assertNull(SignatureAlgorithm.fromString("A128GCM"), "A128GCM should return null");
        assertNull(SignatureAlgorithm.fromString("A256GCM"), "A256GCM should return null");

        // Verify that actual signature algorithms ARE recognized
        assertEquals(SignatureAlgorithm.fromString("RS256"), SignatureAlgorithm.RS256, "RS256 should be recognized");
        assertEquals(SignatureAlgorithm.fromString("ES256"), SignatureAlgorithm.ES256, "ES256 should be recognized");
        assertEquals(SignatureAlgorithm.fromString("none"), SignatureAlgorithm.NONE, "none should be recognized");
    }
}
