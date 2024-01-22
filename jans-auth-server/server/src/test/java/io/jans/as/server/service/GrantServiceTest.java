package io.jans.as.server.service;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.model.token.TokenEntity;
import io.jans.model.token.TokenType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.CacheService;
import io.jans.service.cache.CacheConfiguration;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

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
    private CacheConfiguration cacheConfiguration;

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
}
