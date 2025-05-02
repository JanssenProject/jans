package io.jans.as.server.service;

import com.google.common.collect.Lists;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantType;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;

import static org.mockito.Mockito.when;
import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertNotNull;


/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class IntrospectionServiceTest {

    @InjectMocks
    private IntrospectionService introspectionService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private Logger log;

    @Mock
    private WebKeysConfiguration webKeysConfiguration;

    @Mock
    private ClientService clientService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    public void validateIntrospectionScopePresence_whenIntrospectionScopeCheckIsDisabled_shouldNotRaiseException() {
        when(appConfiguration.getIntrospectionAccessTokenMustHaveIntrospectionScope()).thenReturn(false);

        introspectionService.validateIntrospectionScopePresence(getTestGrant());
    }

    @Test
    public void validateIntrospectionScopePresence_whenIntrospectionScopeCheckIsEnabledAndIntrospectionScopePresent_shouldNotRaiseException() {
        when(appConfiguration.getIntrospectionAccessTokenMustHaveIntrospectionScope()).thenReturn(true);
        final AuthorizationGrant grant = getTestGrant();
        grant.setScopes(Lists.newArrayList("introspection"));

        introspectionService.validateIntrospectionScopePresence(grant);
    }

    @Test
    public void validateIntrospectionScopePresence_whenIntrospectionScopeCheckIsEnabledAndIntrospectionScopeIsNotPresent_shouldRaiseException() {
        when(appConfiguration.getIntrospectionAccessTokenMustHaveIntrospectionScope()).thenReturn(true);
        final AuthorizationGrant grant = getTestGrant();
        grant.setScopes(Lists.newArrayList("openid"));

        try {
            introspectionService.validateIntrospectionScopePresence(grant);
        } catch (WebApplicationException e) {
            return;
        }

        fail("Exception was not thrown however grant didn't have 'introspection' scope");
    }

    @Test
    public void isJwtResponse_whenNoParameterAndNoAcceptHeader_shouldReturnFalse() {
        assertFalse(introspectionService.isJwtResponse(null, null));
    }

    @Test
    public void isJwtResponse_whenParameterIsTrue_shouldReturnTrue() {
        assertTrue(introspectionService.isJwtResponse("true", null));
    }

    @Test
    public void isJwtResponse_whenNoParameterButAcceptHasJwtValue_shouldReturnTrue() {
        assertTrue(introspectionService.isJwtResponse(null, Constants.APPLICATION_TOKEN_INTROSPECTION_JWT));
    }

    @Test
    public void fillPayload_whenCalled_shouldProduceCorrectStructure() throws Exception {
        Mockito.doReturn("https://example.as.com").when(appConfiguration).getIssuer();

        Client client = new Client();
        client.setClientId("testClientId");
        final AuthorizationGrant grant = getTestGrant(client);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("newClaim", "newValue");

        Jwt jwt = new Jwt();
        introspectionService.fillPayload(jwt, jsonObject, grant);

        assertNotNull(jwt);
        assertEquals(jwt.getClaims().getClaimAsString("iss"), "https://example.as.com");
        assertEquals(jwt.getClaims().getClaimAsString("aud"), "testClientId");
        assertTrue(StringUtils.isNotBlank(jwt.getClaims().getClaimAsString("iat")));

        assertEquals(jwt.getClaims().getClaimAsJSON("token_introspection").getString("newClaim"), "newValue");
    }

    private static AuthorizationGrant getTestGrant() {
        return getTestGrant(null);
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
