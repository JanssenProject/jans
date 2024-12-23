package io.jans.as.server.userinfo.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantType;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;

import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class UserInfoServiceTest {

    @InjectMocks
    private UserInfoService userInfoService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void fillJwr_whenCalled_shouldFillRequiredClaims() {
        when(appConfiguration.getUserInfoLifetime()).thenReturn(3600);

        Client client = new Client();
        client.setClientId("1234");
        AuthorizationGrant grant = getTestGrant(client);

        JsonWebResponse jwr = new JsonWebResponse();

        userInfoService.fillJwr(jwr, grant);

        assertEquals("1234", jwr.getClaims().getClaimAsString("client_id"));
        assertNotNull(jwr.getClaims().getClaimAsString("jti"));
        assertNotNull(jwr.getClaims().getClaimAsDate("exp"));
        assertNotNull(jwr.getClaims().getClaimAsDate("iat"));
        assertNotNull(jwr.getClaims().getClaimAsDate("nbf"));
    }

    private static AuthorizationGrant getTestGrant(final Client client) {
        final AuthorizationGrant grant = new AuthorizationGrant() {
            @Override
            public GrantType getGrantType() {
                return GrantType.AUTHORIZATION_CODE;
            }
        };
        grant.init(new User(), AuthorizationGrantType.AUTHORIZATION_CODE, client, new Date());
        return grant;
    }
}
