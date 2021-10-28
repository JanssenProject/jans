package io.jans.as.server.model;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.model.common.AuthorizationGrantType;
import io.jans.as.server.model.common.CIBAGrant;
import io.jans.as.server.model.common.CacheGrant;
import io.jans.as.server.model.common.CibaRequestCacheControl;
import io.jans.as.server.model.token.IdTokenFactory;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.GrantService;
import io.jans.as.server.service.MetricService;
import io.jans.as.server.service.SectorIdentifierService;
import io.jans.as.server.service.external.ExternalIntrospectionService;
import io.jans.as.server.service.stat.StatService;
import io.jans.service.CacheService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.testng.Assert.*;

@Listeners(MockitoTestNGListener.class)
public class CIBAGrantTest {
    
    @InjectMocks
    private CIBAGrant cibaGrant;

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
    private AttributeService attributeService;

    @Mock
    private SectorIdentifierService sectorIdentifierService;

    @Mock
    private MetricService metricService;

    @Mock
    private StatService statService;

    @Mock
    protected AppConfiguration appConfiguration;

    @Mock
    protected ScopeChecker scopeChecker;

    @Test
    public void getGrantType_shouldBeCIBA() {
        GrantType grantType = cibaGrant.getGrantType();

        assertNotNull(grantType);
        assertEquals(grantType, GrantType.CIBA);
    }

    @Test
    public void save_CacheValueInserted() {
        final String authReqId = "any-id-123";
        cibaGrant.setAuthReqId(authReqId);

        cibaGrant.save();

        verify(cacheService).put(anyInt(), eq(authReqId), any(CacheGrant.class));
        verifyNoMoreInteractions(cacheService);
    }

    @Test
    public void init_allFieldsInitiated() {
        CibaRequestCacheControl cibaRequest = buildCibaRequestCacheControl();

        cibaGrant.init(cibaRequest);

        assertEquals(cibaGrant.getAuthReqId(), cibaRequest.getAuthReqId());
        assertEquals(cibaGrant.getAcrValues(), cibaRequest.getAcrValues());
        assertEquals(cibaGrant.getScopes(), cibaRequest.getScopes());
        assertTrue(cibaGrant.isCachedWithNoPersistence());
        assertNull(cibaGrant.getAuthenticationTime());
        assertEquals(cibaGrant.getUser(), cibaRequest.getUser());
        assertEquals(cibaGrant.getAuthorizationGrantType(), AuthorizationGrantType.CIBA);
        assertEquals(cibaGrant.getClient(), cibaRequest.getClient());
        assertNotNull(cibaGrant.getGrantId());
    }

    private CibaRequestCacheControl buildCibaRequestCacheControl() {
        User user = new User();
        user.setDn("user-dn");
        user.setUserId("user-id");

        Client client = new Client();
        client.setClientId("client-id");
        client.setDn("client-dn");

        List<String> scopes = Arrays.asList("openid", "profile");

        return new CibaRequestCacheControl(user, client, 300,
                scopes, "client-notification-token", "binding-message", 1L, "acr-values");
    }
    
}
