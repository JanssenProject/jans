package io.jans.as.server.authorize.ws.rs;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.service.ScopeService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AuthorizationChallengeValidatorTest {

    @InjectMocks
    private AuthorizationChallengeValidator authorizationChallengeValidator;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private ScopeService scopeService;

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccess_whenClientIsNull_shouldThrowError() {
        when(errorResponseFactory.newErrorResponse(Response.Status.BAD_REQUEST)).thenCallRealMethod();

        authorizationChallengeValidator.validateAccess(null);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccess_whenClientDoesNotHaveRequiredScopes_shouldThrowError() {
        when(errorResponseFactory.newErrorResponse(Response.Status.BAD_REQUEST)).thenCallRealMethod();

        Client client = new Client();
        client.setScopes(new String[] {"id1"});

        authorizationChallengeValidator.validateAccess(client);
    }

    @Test
    public void validateAccess_whenClientHasAuthorizationChallengeScope_shouldPass() {
        when(scopeService.getScopeIdsByDns(Lists.newArrayList("id1"))).thenReturn(Lists.newArrayList("authorization_challenge"));

        Client client = new Client();
        client.setScopes(new String[] {"id1"});

        authorizationChallengeValidator.validateAccess(client);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateGrantType_whenClientIsNull_shouldThrowError() {
        when(errorResponseFactory.newErrorResponse(Response.Status.BAD_REQUEST)).thenCallRealMethod();

        authorizationChallengeValidator.validateGrantType(null, null);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateGrantType_whenClientGrantTypesAreNull_shouldThrowError() {
        when(errorResponseFactory.newErrorResponse(Response.Status.BAD_REQUEST)).thenCallRealMethod();

        authorizationChallengeValidator.validateGrantType(new Client(), null);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateGrantType_whenClientGrantTypesDoesNotHaveAuthorizationCode_shouldThrowError() {
        when(errorResponseFactory.newErrorResponse(Response.Status.BAD_REQUEST)).thenCallRealMethod();

        final Client client = new Client();
        client.setGrantTypes(new GrantType[]{GrantType.CLIENT_CREDENTIALS});

        authorizationChallengeValidator.validateGrantType(client, null);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateGrantType_whenGrantTypeIsNotAllowedByConfig_shouldThrowError() {
        when(errorResponseFactory.newErrorResponse(Response.Status.BAD_REQUEST)).thenCallRealMethod();
        when(appConfiguration.getGrantTypesSupported()).thenReturn(Sets.newHashSet(GrantType.IMPLICIT));

        final Client client = new Client();
        client.setGrantTypes(new GrantType[]{GrantType.AUTHORIZATION_CODE});

        authorizationChallengeValidator.validateGrantType(client, null);
    }

    @Test
    public void validateGrantType_whenGrantTypeIsAllowedByConfigAndClient_shouldPassSuccessfully() {
        when(appConfiguration.getGrantTypesSupported()).thenReturn(Sets.newHashSet(GrantType.IMPLICIT, GrantType.AUTHORIZATION_CODE));

        final Client client = new Client();
        client.setGrantTypes(new GrantType[]{GrantType.AUTHORIZATION_CODE});

        authorizationChallengeValidator.validateGrantType(client, "state");
    }
}
