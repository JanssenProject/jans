package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.session.DeviceSession;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.CookieService;
import io.jans.as.server.service.RequestParameterService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.external.ExternalAuthorizationChallengeService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AuthorizationChallengeServiceTest {

    @InjectMocks
    private AuthorizationChallengeService authorizationChallengeService;

    @Mock
    private Logger log;

    @Mock
    private AuthzRequestService authzRequestService;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Mock
    private AuthorizeRestWebServiceValidator authorizeRestWebServiceValidator;

    @Mock
    private ScopeChecker scopeChecker;

    @Mock
    private AuthorizationGrantList authorizationGrantList;

    @Mock
    private AuthorizationChallengeValidator authorizationChallengeValidator;

    @Mock
    private ExternalAuthorizationChallengeService externalAuthorizationChallengeService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private DeviceSessionService deviceSessionService;

    @Mock
    private Identity identity;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private RequestParameterService requestParameterService;

    @Mock
    private CookieService cookieService;

    @Test
    public void prepareAuthzRequest_whenClientIdStoredInAttributes_shouldPopulateAuthzRequest() {
        final String deviceSessionId = "device_session_1234";

        final DeviceSession deviceSession = new DeviceSession();
        deviceSession.setId(deviceSessionId);
        deviceSession.getAttributes().getAttributes().put("client_id", "1234");

        final AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setDeviceSession(deviceSessionId);
        assertNull(authzRequest.getClientId());

        when(deviceSessionService.getDeviceSession(deviceSessionId)).thenReturn(deviceSession);

        authorizationChallengeService.prepareAuthzRequest(authzRequest);
        assertEquals("1234", authzRequest.getClientId());
    }
}
