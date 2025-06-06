package io.jans.as.server.model.common;

import com.google.common.collect.Lists;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.model.token.IdTokenFactory;
import io.jans.as.server.service.*;
import io.jans.as.server.service.external.ExternalIntrospectionService;
import io.jans.as.server.service.external.ExternalUpdateTokenService;
import io.jans.as.server.service.logout.LogoutStatusJwtService;
import io.jans.as.server.service.stat.StatService;
import io.jans.as.server.service.token.StatusListIndexService;
import io.jans.as.server.service.token.StatusListService;
import io.jans.service.CacheService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AuthorizationGrantTest {

    @InjectMocks
    private SimpleAuthorizationGrant authorizationGrant;

    @Mock
    private CacheService cacheService;

    @Mock
    private GrantService grantService;

    @Mock
    private IdTokenFactory idTokenFactory;

    @Mock
    private WebKeysConfiguration webKeysConfiguration;

    @Mock
    private ClientService clientService;

    @Mock
    private ExternalIntrospectionService externalIntrospectionService;

    @Mock
    private ExternalUpdateTokenService externalUpdateTokenService;

    @Mock
    private AttributeService attributeService;

    @Mock
    private SectorIdentifierService sectorIdentifierService;

    @Mock
    private MetricService metricService;

    @Mock
    private StatService statService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private StatusListService statusListService;

    @Mock
    private StatusListIndexService statusListIndexService;

    @Mock
    private LogoutStatusJwtService logoutStatusJwtService;

    @Mock
    protected AppConfiguration appConfiguration;

    @Mock
    protected ScopeChecker scopeChecker;

    @Mock
    private KeyGeneratorTimer keyGeneratorTimer;

    @Test
    public void getGrantType_forSimpleGrantType_shouldReturnNone() {
        assertEquals(GrantType.NONE, authorizationGrant.getGrantType());
    }

    @Test
    public void fillPayloadOfAccessTokenJwt_whenCalled_shouldNotPutCode() {
        ExecutionContext context = new ExecutionContext();
        context.generateRandomTokenReferenceId();

        AccessToken accessToken = new AccessToken(100);
        JwtClaims claims = new JwtClaims();

        authorizationGrant.init(new User(), AuthorizationGrantType.AUTHORIZATION_CODE, new Client(), new Date());
        authorizationGrant.setScopes(Lists.newArrayList("openid"));
        authorizationGrant.fillPayloadOfAccessTokenJwt(claims, accessToken, context);

        assertNull(claims.getClaim("code"));
    }
}
