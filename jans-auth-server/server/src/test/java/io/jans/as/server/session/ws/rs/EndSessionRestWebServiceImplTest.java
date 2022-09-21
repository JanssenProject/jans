package io.jans.as.server.session.ws.rs;

import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.service.*;
import io.jans.as.server.service.external.ExternalApplicationSessionService;
import io.jans.as.server.service.external.ExternalEndSessionService;
import io.jans.model.security.Identity;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class EndSessionRestWebServiceImplTest {

    private static final String DUMMY_JWT = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2NjM2NzcxODUsImV4cCI6MTY5NTIxMzE4NSwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJFbWFpbCI6Impyb2NrZXRAZXhhbXBsZS5jb20iLCJzaWQiOiIxMjM0IiwiUm9sZSI6IlByb2plY3QgQWRtaW5pc3RyYXRvciJ9.pmJ5kTvxyfOUGOXTzYA1DMjbF96lfCF1dVSn_70nf2Q";
    private static final AuthorizationGrant GRANT = new AuthorizationGrant() {
        @Override
        public GrantType getGrantType() {
            return GrantType.AUTHORIZATION_CODE;
        }
    };

    @InjectMocks
    private EndSessionRestWebServiceImpl endSessionRestWebService;

    @Mock
    private Logger log;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private RedirectionUriService redirectionUriService;

    @Mock
    private AuthorizationGrantList authorizationGrantList;

    @Mock
    private ExternalApplicationSessionService externalApplicationSessionService;

    @Mock
    private ExternalEndSessionService externalEndSessionService;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private CookieService cookieService;

    @Mock
    private ClientService clientService;

    @Mock
    private GrantService grantService;

    @Mock
    private Identity identity;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private LogoutTokenFactory logoutTokenFactory;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Test
    public void validateIdTokenHint_whenIdTokenHintIsBlank_shouldGetNoError() {
        assertNull(endSessionRestWebService.validateIdTokenHint("", null, "http://postlogout.com"));
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateIdTokenHint_whenIdTokenHintIsBlankButRequired_shouldGetError() {
        when(appConfiguration.getForceIdTokenHintPrecense()).thenReturn(true);

        endSessionRestWebService.validateIdTokenHint("", null, "http://postlogout.com");
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateIdTokenHint_whenIdTokenIsNotInDbAndExpiredIsNotAllowed_shouldGetError() {
        when(appConfiguration.getRejectEndSessionIfIdTokenExpired()).thenReturn(true);
        when(endSessionRestWebService.getTokenHintGrant("test")).thenReturn(null);

        endSessionRestWebService.validateIdTokenHint("testToken", null, "http://postlogout.com");
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateIdTokenHint_whenIdTokenIsNotValidJwt_shouldGetError() {
        when(appConfiguration.getEndSessionWithAccessToken()).thenReturn(true);
        when(endSessionRestWebService.getTokenHintGrant("notValidJwt")).thenReturn(GRANT);

        endSessionRestWebService.validateIdTokenHint("notValidJwt", null, "http://postlogout.com");
    }

    @Test
    public void validateIdTokenHint_whenIdTokenIsValidJwt_shouldGetValidJwt() {
        when(appConfiguration.getEndSessionWithAccessToken()).thenReturn(true);
        when(endSessionRestWebService.getTokenHintGrant(DUMMY_JWT)).thenReturn(GRANT);

        final Jwt jwt = endSessionRestWebService.validateIdTokenHint(DUMMY_JWT, null, "http://postlogout.com");
        assertNotNull(jwt);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateIdTokenHint_whenIdTokenSignatureIsBad_shouldGetError() throws Exception {
        when(appConfiguration.getEndSessionWithAccessToken()).thenReturn(false);
        when(appConfiguration.getAllowEndSessionWithUnmatchedSid()).thenReturn(true);
        when(endSessionRestWebService.getTokenHintGrant(DUMMY_JWT)).thenReturn(null);
        when(cryptoProvider.verifySignature(anyString(), anyString(), anyString(), isNull(), isNull(), any())).thenReturn(false);

        assertNull(endSessionRestWebService.validateIdTokenHint(DUMMY_JWT, null, "http://postlogout.com"));
    }

    @Test
    public void validateIdTokenHint_whenIdTokenIsExpiredAndSidCheckIsNotRequired_shouldGetValidJwt() throws Exception {
        when(appConfiguration.getEndSessionWithAccessToken()).thenReturn(false);
        when(appConfiguration.getAllowEndSessionWithUnmatchedSid()).thenReturn(true);
        when(endSessionRestWebService.getTokenHintGrant(DUMMY_JWT)).thenReturn(null);
        when(cryptoProvider.verifySignature(anyString(), anyString(), isNull(), isNull(), isNull(), any())).thenReturn(true);

        final Jwt jwt = endSessionRestWebService.validateIdTokenHint(DUMMY_JWT, null, "http://postlogout.com");
        assertNotNull(jwt);
    }

    @Test
    public void validateIdTokenHint_whenIdTokenIsExpiredAndSidCheckIsRequired_shouldGetValidJwt() throws Exception {
        when(appConfiguration.getEndSessionWithAccessToken()).thenReturn(false);
        when(appConfiguration.getAllowEndSessionWithUnmatchedSid()).thenReturn(false);
        when(endSessionRestWebService.getTokenHintGrant(DUMMY_JWT)).thenReturn(null);
        when(cryptoProvider.verifySignature(anyString(), anyString(), isNull(), isNull(), isNull(), any())).thenReturn(true);

        SessionId sidSession = new SessionId();
        sidSession.setOutsideSid("1234"); // sid encoded into DUMMY_JWT

        final Jwt jwt = endSessionRestWebService.validateIdTokenHint(DUMMY_JWT, sidSession, "http://postlogout.com");
        assertNotNull(jwt);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateIdTokenHint_whenIdTokenIsExpiredAndSidCheckIsRequiredButSessionHasAnotherSid_shouldGetError() throws Exception {
        when(appConfiguration.getEndSessionWithAccessToken()).thenReturn(false);
        when(appConfiguration.getAllowEndSessionWithUnmatchedSid()).thenReturn(false);
        when(endSessionRestWebService.getTokenHintGrant(DUMMY_JWT)).thenReturn(null);
        when(cryptoProvider.verifySignature(anyString(), anyString(), isNull(), isNull(), isNull(), any())).thenReturn(true);

        SessionId sidSession = new SessionId();
        sidSession.setOutsideSid("12345"); // sid encoded into DUMMY_JWT

        final Jwt jwt = endSessionRestWebService.validateIdTokenHint(DUMMY_JWT, sidSession, "http://postlogout.com");
        assertNotNull(jwt);
    }
}