package io.jans.as.server.authorize.ws.rs;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.ScopeConstants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.ciba.CIBAPingCallbackService;
import io.jans.as.server.ciba.CIBAPushTokenDeliveryService;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.*;
import io.jans.as.server.service.ciba.CibaRequestService;
import io.jans.as.server.service.external.ExternalPostAuthnService;
import io.jans.as.server.service.external.ExternalUpdateTokenService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AuthorizeRestWebServiceImplTest {

    @InjectMocks
    private AuthorizeRestWebServiceImpl authorizeRestWebService;

    @Mock
    private Logger log;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private AuthorizationGrantList authorizationGrantList;

    @Mock
    private ClientService clientService;

    @Mock
    private UserService userService;

    @Mock
    private Identity identity;

    @Mock
    private AuthenticationFilterService authenticationFilterService;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private CookieService cookieService;

    @Mock
    private ScopeChecker scopeChecker;

    @Mock
    private ClientAuthorizationsService clientAuthorizationsService;

    @Mock
    private RequestParameterService requestParameterService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ConfigurationFactory configurationFactory;

    @Mock
    private AuthorizeRestWebServiceValidator authorizeRestWebServiceValidator;

    @Mock
    private CIBAPushTokenDeliveryService cibaPushTokenDeliveryService;

    @Mock
    private CIBAPingCallbackService cibaPingCallbackService;

    @Mock
    private ExternalPostAuthnService externalPostAuthnService;

    @Mock
    private CibaRequestService cibaRequestService;

    @Mock
    private DeviceAuthorizationService deviceAuthorizationService;

    @Mock
    private AttributeService attributeService;

    @Mock
    private ExternalUpdateTokenService externalUpdateTokenService;

    @Mock
    private AuthzRequestService authzRequestService;

    @Test
    public void checkPromptLogin_whenDisablePromptLoginIsTrue_shouldNotClearSession() {
        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setSessionId("some_id");

        List<Prompt> promptList = new ArrayList<>();
        promptList.add(Prompt.LOGIN);

        when(appConfiguration.getDisablePromptLogin()).thenReturn(true);

        authorizeRestWebService.checkPromptLogin(authzRequest, promptList);
        assertEquals(authzRequest.getSessionId(), "some_id");
    }

    @Test
    public void checkPromptLogin_whenDisablePromptLoginIsFalse_shouldClearSession() {
        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setSessionId("some_id");

        List<Prompt> promptList = new ArrayList<>();
        promptList.add(Prompt.LOGIN);

        when(identity.getSessionId()).thenReturn(new SessionId());
        when(appConfiguration.getDisablePromptLogin()).thenReturn(false);

        authorizeRestWebService.checkPromptLogin(authzRequest, promptList);
        assertNull(authzRequest.getSessionId());
    }

    @Test
    public void checkOfflineAccessScopes_whenOfflineAccessIsPresentAndConsentNot_shouldRemoveOfflineAccess() {
        final Set<String> scopes = Sets.newHashSet(ScopeConstants.OFFLINE_ACCESS);
        authorizeRestWebService.checkOfflineAccessScopes(Lists.newArrayList(ResponseType.CODE), Lists.newArrayList(), new Client(), scopes);
        assertTrue(scopes.isEmpty());
    }

    @Test
    public void checkOfflineAccessScopes_whenOfflineAccessIsPresentAndConsentNotButAllowedByClient_shouldNotRemoveOfflineAccess() {
        final Set<String> scopes = Sets.newHashSet(ScopeConstants.OFFLINE_ACCESS);
        final Client client = new Client();
        client.getAttributes().setAllowOfflineAccessWithoutConsent(true);

        authorizeRestWebService.checkOfflineAccessScopes(Lists.newArrayList(ResponseType.CODE), Lists.newArrayList(), client, scopes);
        assertEquals(scopes.iterator().next(), ScopeConstants.OFFLINE_ACCESS);
    }

    @Test
    public void checkOfflineAccessScopes_whenOfflineAccessIsPresentAndResponseTypeCodeAbsent_shouldRemoveOfflineAccess() {
        final Set<String> scopes = Sets.newHashSet(ScopeConstants.OFFLINE_ACCESS);

        authorizeRestWebService.checkOfflineAccessScopes(Lists.newArrayList(ResponseType.TOKEN), Lists.newArrayList(), new Client(), scopes);
        assertTrue(scopes.isEmpty());
    }

    @Test
    public void checkOfflineAccessScopes_whenOfflineAccessIsPresentAndResponseTypeCodeAbsent_shouldRemoveOfflineAccessOnly() {
        final Set<String> scopes = Sets.newHashSet("openid", ScopeConstants.OFFLINE_ACCESS);

        authorizeRestWebService.checkOfflineAccessScopes(Lists.newArrayList(ResponseType.TOKEN), Lists.newArrayList(), new Client(), scopes);
        assertEquals(scopes.iterator().next(), "openid");
    }
}
