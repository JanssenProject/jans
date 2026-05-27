package io.jans.as.server.userinfo.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.UserService;
import io.jans.as.server.service.date.DateFormatterService;
import io.jans.as.server.service.external.ExternalDynamicScopeService;
import io.jans.as.server.service.token.StatusListIndexService;
import io.jans.as.server.service.token.StatusListService;
import io.jans.as.server.service.token.TokenService;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Listeners(MockitoTestNGListener.class)
public class UserInfoRestWebServiceImplTest {

    @InjectMocks
    private UserInfoRestWebServiceImpl userInfoRestWebServiceImpl;

    @Mock
    private Logger log;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private WebKeysConfiguration webKeysConfiguration;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private ClientService clientService;

    @Mock
    private ScopeService scopeService;

    @Mock
    private AttributeService attributeService;

    @Mock
    private UserService userService;

    @Mock
    private ExternalDynamicScopeService externalDynamicScopeService;

    @Mock
    private TokenService tokenService;

    @Mock
    private DateFormatterService dateFormatterService;

    @Mock
    private UserInfoService userInfoService;

    @Mock
    private StatusListService statusListService;

    @Mock
    private StatusListIndexService statusListIndexService;

    private User user;
    private AuthorizationGrant grant;
    private Client client;

    @BeforeMethod
    public void setUp() {
        user = mock(User.class);
        grant = mock(AuthorizationGrant.class);
        client = new Client();
        client.setClientId("test-client");

        lenient().when(grant.getClaims()).thenReturn(null);
        lenient().when(grant.getJwtAuthorizationRequest()).thenReturn(null);
        lenient().when(grant.getSub()).thenReturn("test-sub");
        lenient().when(grant.getClient()).thenReturn(client);
        lenient().when(appConfiguration.getIssuer()).thenReturn("https://example.com");
        lenient().when(externalDynamicScopeService.isEnabled()).thenReturn(false);
    }

    @Test
    public void createJwtClaims_whenStatusListEnabled_shouldCallAddStatusClaimWithIndex() throws Exception {
        when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)).thenReturn(true);
        when(statusListIndexService.next()).thenReturn(42);

        invokeCreateJwtClaims();

        ArgumentCaptor<ExecutionContext> contextCaptor = ArgumentCaptor.forClass(ExecutionContext.class);
        verify(statusListService).addStatusClaimWithIndex(any(JwtClaims.class), contextCaptor.capture());
        assertEquals(contextCaptor.getValue().getStatusListIndex(), Integer.valueOf(42));
        verify(statusListIndexService).next();
    }

    @Test
    public void createJwtClaims_whenStatusListDisabled_shouldNotCallAddStatusClaimWithIndex() throws Exception {
        when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)).thenReturn(false);

        invokeCreateJwtClaims();

        verify(statusListService, never()).addStatusClaimWithIndex(any(JwtClaims.class), any(ExecutionContext.class));
        verify(statusListIndexService, never()).next();
    }

    @Test
    public void createJwtClaims_whenStatusListEnabled_shouldReturnClaimsWithIssuer() throws Exception {
        when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)).thenReturn(false);

        JwtClaims claims = invokeCreateJwtClaims();

        assertNotNull(claims);
        assertEquals(claims.getClaimAsString("iss"), "https://example.com");
    }

    private JwtClaims invokeCreateJwtClaims() throws Exception {
        Method method = UserInfoRestWebServiceImpl.class.getDeclaredMethod(
                "createJwtClaims", User.class, AuthorizationGrant.class, java.util.Collection.class);
        method.setAccessible(true);
        return (JwtClaims) method.invoke(userInfoRestWebServiceImpl, user, grant, Collections.emptyList());
    }
}
