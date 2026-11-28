package io.jans.as.server.service;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.token.StatusListIndexService;
import io.jans.model.token.TokenEntity;
import io.jans.model.token.TokenType;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.CacheService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class GrantServiceTest {

    @InjectMocks
    private GrantService grantService;

    @Mock
    private Logger log;

    @Mock
    private PersistenceEntryManager persistenceEntryManager;

    @Mock
    private ClientService clientService;

    @Mock
    private CacheService cacheService;

    @Mock
    private StaticConfiguration staticConfiguration;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private StatusListIndexService statusListIndexService;

    @Test
    public void shouldPersist_byDefault_shouldReturnTrue() {
        assertTrue(grantService.shouldPersist());
    }

    @Test
    public void shouldPersist_whenDontPersistConfIsTrue_shouldReturnFalse() {
        Mockito.doReturn(true).when(appConfiguration).getSaveTokensInCacheAndDontSaveInPersistence();

        assertFalse(grantService.shouldPersist());
    }

    @Test
    public void shouldSaveInCache_byDefault_shoultReturnFalse() {
        assertFalse(grantService.shouldSaveInCache());
    }

    @Test
    public void shouldSaveInCache_whenAllowedByConfig_shoultReturnTrue() {
        Mockito.doReturn(true).when(appConfiguration).getSaveTokensInCacheAndDontSaveInPersistence();

        assertTrue(grantService.shouldSaveInCache());
    }

    @Test
    public void shouldSaveInCache_whenAllowedByMainConfig_shoultReturnTrue() {
        Mockito.doReturn(true).when(appConfiguration).getSaveTokensInCache();

        assertTrue(grantService.shouldSaveInCache());
    }

    @Test
    public void filterOutRefreshTokenFromDeletion_forTokenWithoutOnlineAccess_shouldFilterOut() {
        Mockito.doReturn(false).when(appConfiguration).getRemoveRefreshTokensForClientOnLogout();

        TokenEntity token = new TokenEntity();
        token.setTokenTypeEnum(TokenType.REFRESH_TOKEN);
        token.getAttributes().setOnlineAccess(false);

        List<TokenEntity> tokens = new ArrayList<>();
        tokens.add(token);

        grantService.filterOutRefreshTokenFromDeletion(tokens);
        assertTrue(tokens.isEmpty());
    }

    @Test
    public void filterOutRefreshTokenFromDeletion_forTokenWithOnlineAccess_shouldNotFilterOut() {
        Mockito.doReturn(false).when(appConfiguration).getRemoveRefreshTokensForClientOnLogout();

        TokenEntity token = new TokenEntity();
        token.setTokenTypeEnum(TokenType.REFRESH_TOKEN);
        token.getAttributes().setOnlineAccess(true);

        List<TokenEntity> tokens = new ArrayList<>();
        tokens.add(token);

        grantService.filterOutRefreshTokenFromDeletion(tokens);
        assertFalse(tokens.isEmpty());
    }

    @Test
    public void filterOutRefreshTokenFromDeletion_whenConfigurationRemoveRefreshTokensForClientOnLogoutIsTrue_shouldNotFilterOut() {
        Mockito.doReturn(true).when(appConfiguration).getRemoveRefreshTokensForClientOnLogout();

        TokenEntity token = new TokenEntity();
        token.setTokenTypeEnum(TokenType.REFRESH_TOKEN);
        token.getAttributes().setOnlineAccess(false);

        TokenEntity another = new TokenEntity();
        another.setTokenTypeEnum(TokenType.REFRESH_TOKEN);
        another.getAttributes().setOnlineAccess(true);

        List<TokenEntity> tokens = new ArrayList<>();
        tokens.add(token);
        tokens.add(another);

        grantService.filterOutRefreshTokenFromDeletion(tokens);
        assertEquals(tokens.size(), 2);
    }

    @Test
    public void removeSilently_whenStatusListIndexIsZero_shouldMarkIndexInvalid() throws InterruptedException {
        TokenEntity token = new TokenEntity();
        token.setTokenTypeEnum(TokenType.ID_TOKEN);
        token.getAttributes().setStatusListIndex(0);
        CountDownLatch updated = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            updated.countDown();
            return null;
        }).when(statusListIndexService).updateStatusAtIndexes(Mockito.anyList(), Mockito.any());

        grantService.removeSilently(token);

        assertTrue(updated.await(5, TimeUnit.SECONDS));
        Mockito.verify(statusListIndexService).updateStatusAtIndexes(Collections.singletonList(0), TokenStatus.INVALID);
    }
}
