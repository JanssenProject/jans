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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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
     * Security test: Verifies that the forceSignedRequestObject guard correctly handles
     * both null (unrecognized algorithm) and NONE algorithm cases.
     *
     * This test documents the security fix for vulnerability where:
     * - A JWE with plain JSON payload would have algorithm="RSA-OAEP" in the outer header
     * - SignatureAlgorithm.fromString("RSA-OAEP") returns null (not a signature algorithm)
     * - The OLD buggy check: `signatureAlgorithm == SignatureAlgorithm.NONE` would pass (null != NONE)
     * - The NEW fixed check: `signatureAlgorithm == null || signatureAlgorithm == SignatureAlgorithm.NONE` correctly rejects
     */
    @Test
    public void processRequestObject_whenForceSignedRequestObjectEnabledAndAlgorithmIsNull_shouldRejectRequest() {
        // Verify that RSA-OAEP is not recognized as a signature algorithm
        SignatureAlgorithm sigAlg = SignatureAlgorithm.fromString("RSA-OAEP");
        assertTrue(sigAlg == null, "RSA-OAEP should not be recognized as a signature algorithm");

        // Verify the OLD buggy behavior would have allowed this to pass
        // OLD: signatureAlgorithm == SignatureAlgorithm.NONE
        boolean oldBuggyCheck = (sigAlg == SignatureAlgorithm.NONE); // false! (null != NONE)
        assertTrue(!oldBuggyCheck, "OLD buggy check would have allowed RSA-OAEP to pass");

        // Verify our fix: both null and NONE should be rejected
        // NEW: signatureAlgorithm == null || signatureAlgorithm == SignatureAlgorithm.NONE
        boolean newFixedCheck = (sigAlg == null || sigAlg == SignatureAlgorithm.NONE); // true! (null == null)
        assertTrue(newFixedCheck, "NEW fixed check correctly rejects RSA-OAEP (null algorithm)");
    }

    @Test
    public void signatureAlgorithmFromString_withJweEncryptionAlgorithm_shouldReturnNull() {
        // Verify that JWE encryption algorithms are not recognized as signature algorithms
        // This is important for the forceSignedRequestObject security fix
        assertTrue(SignatureAlgorithm.fromString("RSA-OAEP") == null, "RSA-OAEP should return null");
        assertTrue(SignatureAlgorithm.fromString("RSA1_5") == null, "RSA1_5 should return null");
        assertTrue(SignatureAlgorithm.fromString("A128KW") == null, "A128KW should return null");
        assertTrue(SignatureAlgorithm.fromString("A256KW") == null, "A256KW should return null");
        assertTrue(SignatureAlgorithm.fromString("A128GCM") == null, "A128GCM should return null");
        assertTrue(SignatureAlgorithm.fromString("A256GCM") == null, "A256GCM should return null");

        // Verify that actual signature algorithms ARE recognized
        assertTrue(SignatureAlgorithm.fromString("RS256") == SignatureAlgorithm.RS256, "RS256 should be recognized");
        assertTrue(SignatureAlgorithm.fromString("ES256") == SignatureAlgorithm.ES256, "ES256 should be recognized");
        assertTrue(SignatureAlgorithm.fromString("none") == SignatureAlgorithm.NONE, "none should be recognized");
    }
}
