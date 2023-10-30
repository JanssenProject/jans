package io.jans.as.server.revoke;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.common.AuthorizationGrantType;
import io.jans.as.server.model.common.SimpleAuthorizationGrant;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.GrantService;
import io.jans.as.server.service.external.ExternalRevokeTokenService;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.UUID;

import static io.jans.as.server.model.config.Constants.REVOKE_ANY_TOKEN_SCOPE;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class RevokeRestWebServiceImplTest {

    @InjectMocks
    @Spy
    private RevokeRestWebServiceImpl service;

    @Mock
    private Logger log;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Mock
    private Identity identity;

    @Mock
    private AuthorizationGrantList authorizationGrantList;

    @Mock
    private GrantService grantService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private ClientService clientService;

    @Mock
    private ExternalRevokeTokenService externalRevokeTokenService;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void validateSameClient_whenClientIsSame_shouldNotRaiseException() {
        final Client client = new Client();
        client.setClientId(UUID.randomUUID().toString());

        AuthorizationGrant grant = new SimpleAuthorizationGrant();
        grant.init(new User(), AuthorizationGrantType.AUTHORIZATION_CODE, client, new Date());
        service.validateSameClient(grant, client);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateSameClient_whenClientIsNotSame_shouldRaiseException() {
        Mockito.doReturn(false).when(appConfiguration).getAllowRevokeForOtherClients();

        final Client client = new Client();
        client.setClientId(UUID.randomUUID().toString());

        final Client anotherClient = new Client();
        anotherClient.setClientId(UUID.randomUUID().toString());

        AuthorizationGrant grant = new SimpleAuthorizationGrant();
        grant.init(new User(), AuthorizationGrantType.AUTHORIZATION_CODE, client, new Date());
        service.validateSameClient(grant, anotherClient);
    }

    @Test
    public void validateSameClient_whenClientIsNotSameButAllowedByConfig_shouldNotRaiseException() {
        Mockito.doReturn(true).when(appConfiguration).getAllowRevokeForOtherClients();

        final Client client = new Client();
        client.setClientId(UUID.randomUUID().toString());

        final Client anotherClient = new Client();
        anotherClient.setClientId(UUID.randomUUID().toString());

        AuthorizationGrant grant = new SimpleAuthorizationGrant();
        grant.init(new User(), AuthorizationGrantType.AUTHORIZATION_CODE, client, new Date());
        service.validateSameClient(grant, anotherClient);
    }

    @Test
    public void validateScope_whenClientHasRevokeAnyTokenClient_shouldPassSuccessfully() {
        final Client client = new Client();
        client.setClientId(UUID.randomUUID().toString());
        client.setScopes(new String[] {REVOKE_ANY_TOKEN_SCOPE});

        final Client anotherClient = new Client();
        anotherClient.setClientId(UUID.randomUUID().toString());

        AuthorizationGrant grant = new SimpleAuthorizationGrant();
        grant.init(new User(), AuthorizationGrantType.AUTHORIZATION_CODE, anotherClient, new Date());
        service.validateScope(grant, client);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateScope_whenClientHasNoRevokeAnyTokenScope_shouldFail() {
        final Client client = new Client();
        client.setClientId(UUID.randomUUID().toString());
        client.setScopes(new String[] {"openid"});

        final Client anotherClient = new Client();
        anotherClient.setClientId(UUID.randomUUID().toString());

        AuthorizationGrant grant = new SimpleAuthorizationGrant();
        grant.init(new User(), AuthorizationGrantType.AUTHORIZATION_CODE, anotherClient, new Date());
        service.validateScope(grant, client);
    }

    @Test
    public void validateScope_whenClientHasNoRevokeAnyTokenScopeButRevokeOwnToken_shouldPassSuccessfully() {
        final Client client = new Client();
        client.setClientId(UUID.randomUUID().toString());
        client.setScopes(new String[] {"openid"});

        AuthorizationGrant grant = new SimpleAuthorizationGrant();
        grant.init(new User(), AuthorizationGrantType.AUTHORIZATION_CODE, client, new Date());
        service.validateScope(grant, client);
    }
}
