package io.jans.as.server.model.common;

import io.jans.as.model.configuration.AppConfiguration;
import jakarta.enterprise.inject.Instance;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;

public class CacheGrantTest {

    private static final String REDIRECT_URI = "https://rp.example.org/cb";

    @Test
    public void constructor_whenAuthorizationGrant_shouldCopyRedirectUri() {
        final AuthorizationGrant grant = mock(AuthorizationGrant.class);
        when(grant.getRedirectUri()).thenReturn(REDIRECT_URI);
        final AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setAccessTokenLifetime(300);

        final CacheGrant cacheGrant = new CacheGrant(grant, appConfiguration);

        assertEquals(REDIRECT_URI, cacheGrant.getRedirectUri());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void asCodeGrant_whenRedirectUriIsSet_shouldRestoreRedirectUri() {
        final AuthorizationGrant source = mock(AuthorizationGrant.class);
        when(source.getRedirectUri()).thenReturn(REDIRECT_URI);
        when(source.getAuthorizationCode()).thenReturn(new AuthorizationCode(60));
        final CacheGrant cacheGrant = new CacheGrant(source, new AppConfiguration());

        final AuthorizationCodeGrant codeGrant = mock(AuthorizationCodeGrant.class);
        final Instance<AuthorizationCodeGrant> selected = mock(Instance.class);
        final Instance<AbstractAuthorizationGrant> grantInstance = mock(Instance.class);
        when(grantInstance.select(AuthorizationCodeGrant.class)).thenReturn(selected);
        when(selected.get()).thenReturn(codeGrant);

        cacheGrant.asCodeGrant(grantInstance);

        verify(codeGrant).setRedirectUri(REDIRECT_URI);
    }

    @Test
    public void constructor_whenAuthorizationGrant_shouldCopyEphemeralSub() {
        final AuthorizationGrant grant = mock(AuthorizationGrant.class);
        when(grant.getEphemeralSub()).thenReturn("ephemeral-sub");
        final AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setAccessTokenLifetime(300);

        final CacheGrant cacheGrant = new CacheGrant(grant, appConfiguration);

        assertEquals("ephemeral-sub", cacheGrant.getEphemeralSub());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void asCodeGrant_whenEphemeralSubIsSet_shouldRestoreEphemeralSub() {
        final AuthorizationGrant source = mock(AuthorizationGrant.class);
        when(source.getEphemeralSub()).thenReturn("ephemeral-sub");
        when(source.getAuthorizationCode()).thenReturn(new AuthorizationCode(60));
        final CacheGrant cacheGrant = new CacheGrant(source, new AppConfiguration());

        final AuthorizationCodeGrant codeGrant = mock(AuthorizationCodeGrant.class);
        final Instance<AuthorizationCodeGrant> selected = mock(Instance.class);
        final Instance<AbstractAuthorizationGrant> grantInstance = mock(Instance.class);
        when(grantInstance.select(AuthorizationCodeGrant.class)).thenReturn(selected);
        when(selected.get()).thenReturn(codeGrant);

        cacheGrant.asCodeGrant(grantInstance);

        verify(codeGrant).setEphemeralSub("ephemeral-sub");
    }
}
