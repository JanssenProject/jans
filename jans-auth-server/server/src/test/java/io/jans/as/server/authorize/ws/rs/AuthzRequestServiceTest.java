package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
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
}
