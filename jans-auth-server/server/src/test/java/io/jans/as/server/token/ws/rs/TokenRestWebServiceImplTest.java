package io.jans.as.server.token.ws.rs;

import io.jans.as.common.service.AttributeService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.auth.DpopService;
import io.jans.as.server.authorize.ws.rs.AuthzDetailsService;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.common.RefreshToken;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.*;
import io.jans.as.server.service.ciba.CibaRequestService;
import io.jans.as.server.service.external.ExternalResourceOwnerPasswordCredentialsService;
import io.jans.as.server.service.external.ExternalUpdateTokenService;
import io.jans.as.server.service.stat.StatService;
import io.jans.as.server.uma.service.UmaTokenService;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class TokenRestWebServiceImplTest {

    @Mock
    private Logger log;

    @Mock
    private Identity identity;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private AuthorizationGrantList authorizationGrantList;

    @Mock
    private UserService userService;

    @Mock
    private GrantService grantService;

    @Mock
    private AuthenticationFilterService authenticationFilterService;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private UmaTokenService umaTokenService;

    @Mock
    private ExternalResourceOwnerPasswordCredentialsService externalResourceOwnerPasswordCredentialsService;

    @Mock
    private AttributeService attributeService;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private CibaRequestService cibaRequestService;

    @Mock
    private DeviceAuthorizationService deviceAuthorizationService;

    @Mock
    private ExternalUpdateTokenService externalUpdateTokenService;

    @Mock
    private TokenRestWebServiceValidator tokenRestWebServiceValidator;

    @Mock
    private TokenExchangeService tokenExchangeService;

    @Mock
    private TokenCreatorService tokenCreatorService;

    @Mock
    private StatService statService;

    @Mock
    private DpopService dPoPService;

    @Mock
    private AuthzDetailsService authzDetailsService;

    @Mock
    private TxTokenService txTokenService;

    @Mock
    private JwtGrantService jwtGrantService;

    @InjectMocks
    private TokenRestWebServiceImpl tokenRestWebServiceImpl;

    @Test
    public void addRefreshTokenLifetime_whenIncludePropertyIsTrue_shouldIncludeRefreshTokenLifetime() {
        final int refreshTokenLifetime = 5;
        RefreshToken refreshToken = new RefreshToken(refreshTokenLifetime);
        JSONObject json = new JSONObject();
        TokenRestWebServiceImpl.addRefreshTokenLifetime(json, refreshToken, true);

        assertEquals(refreshTokenLifetime, json.getInt("refresh_token_expires_in"));
    }

    @Test
    public void addRefreshTokenLifetime_whenIncludePropertyIsFalse_shouldNotIncludeRefreshTokenLifetime() {
        final int refreshTokenLifetime = 5;
        RefreshToken refreshToken = new RefreshToken(refreshTokenLifetime);
        JSONObject json = new JSONObject();
        TokenRestWebServiceImpl.addRefreshTokenLifetime(json, refreshToken, false);

        assertFalse(json.has("refresh_token_expires_in"));
    }

    @Test
    public void addRefreshTokenLifetime_whenRefreshTokenIsNull_shouldNotIncludeRefreshTokenLifetime() {
        JSONObject json = new JSONObject();
        TokenRestWebServiceImpl.addRefreshTokenLifetime(json, null, true);

        assertFalse(json.has("refresh_token_expires_in"));
    }
}
