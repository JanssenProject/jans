package io.jans.as.server.model.common;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.common.SubjectType;
import io.jans.as.server.service.ClientService;
import io.jans.model.token.TokenEntity;
import jakarta.enterprise.inject.Instance;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

@Listeners(MockitoTestNGListener.class)
public class AuthorizationGrantListTest {

    @InjectMocks
    private AuthorizationGrantList authorizationGrantList;

    @Mock
    private Logger log;

    @Mock
    private Instance<AbstractAuthorizationGrant> grantInstance;

    @Mock
    private UserService userService;

    @Mock
    private ClientService clientService;

    @Test
    public void asGrant_whenEphemeralClientAndNoStoredSub_shouldReject() {
        mockGrantInstance(client(SubjectType.EPHEMERAL));

        assertNull(authorizationGrantList.asGrant(tokenEntity(null, "user-id")));
    }

    @Test
    public void asGrant_whenEphemeralClientAndStoredSub_shouldRestoreStoredSub() {
        mockGrantInstance(client(SubjectType.EPHEMERAL));

        final AuthorizationGrant grant = authorizationGrantList.asGrant(tokenEntity("stored-sub", "user-id"));

        assertNotNull(grant);
        assertEquals("stored-sub", grant.getEphemeralSub());
    }

    @Test
    public void asGrant_whenStoredSubAndUserNotFound_shouldReject() {
        mockGrantInstance(client(SubjectType.EPHEMERAL));
        when(userService.getUser("missing-user")).thenReturn(null);

        assertNull(authorizationGrantList.asGrant(tokenEntity("stored-sub", "missing-user")));
    }

    @Test
    public void asGrant_whenStoredSubAndNoUserId_shouldReject() {
        mockGrantInstance(client(SubjectType.EPHEMERAL));

        assertNull(authorizationGrantList.asGrant(tokenEntity("stored-sub", null)));
    }

    @Test
    public void asGrant_whenNonEphemeralClientAndNoStoredSub_shouldNotKeepEphemeralSub() {
        mockGrantInstance(client(SubjectType.PUBLIC));

        final AuthorizationGrant grant = authorizationGrantList.asGrant(tokenEntity(null, "user-id"));

        assertNotNull(grant);
        assertNull(grant.getEphemeralSub());
    }

    @Test
    public void asGrant_whenEphemeralClientWithoutUserAndNoStoredSub_shouldRestoreWithoutEphemeralSub() {
        mockGrantInstance(client(SubjectType.EPHEMERAL));

        final AuthorizationGrant grant = authorizationGrantList.asGrant(tokenEntity(null, null));

        assertNotNull(grant);
        assertNull(grant.getEphemeralSub());
    }

    @SuppressWarnings("unchecked")
    private void mockGrantInstance(Client client) {
        when(clientService.getClient(anyString())).thenReturn(client);
        lenient().when(userService.getUser(anyString())).thenReturn(new User());
        final Instance<ClientCredentialsGrant> selected = mock(Instance.class);
        when(grantInstance.select(ClientCredentialsGrant.class)).thenReturn(selected);
        when(selected.get()).thenReturn(new ClientCredentialsGrant());
    }

    private static Client client(SubjectType subjectType) {
        final Client client = new Client();
        client.setClientId("client-id");
        client.setSubjectType(subjectType);
        return client;
    }

    private static TokenEntity tokenEntity(String ephemeralSub, String userId) {
        final TokenEntity entity = new TokenEntity();
        entity.setGrantType("client_credentials");
        entity.setClientId("client-id");
        entity.setUserId(userId);
        entity.getAttributes().setEphemeralSub(ephemeralSub);
        return entity;
    }
}
