package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.par.ws.rs.ParService;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.RedirectUriResponse;
import io.jans.as.server.service.RedirectionUriService;
import io.jans.as.server.service.RequestParameterService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

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

    @Test
    public void addDeviceSecretToSession_withoutUnabledConfiguration_shouldNotGenerateDeviceSecret() {
        when(appConfiguration.getReturnDeviceSecretFromAuthzEndpoint()).thenReturn(false);

        Client client = new Client();
        client.setGrantTypes(new GrantType[] { GrantType.AUTHORIZATION_CODE, GrantType.TOKEN_EXCHANGE});

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
        client.setGrantTypes(new GrantType[] { GrantType.AUTHORIZATION_CODE, GrantType.TOKEN_EXCHANGE});

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
        client.setGrantTypes(new GrantType[] { GrantType.AUTHORIZATION_CODE, GrantType.TOKEN_EXCHANGE});

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
        client.setGrantTypes(new GrantType[] { GrantType.AUTHORIZATION_CODE});

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
