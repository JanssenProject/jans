package io.jans.as.server.service;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class SpontaneousScopeServiceTest {

    @InjectMocks
    private SpontaneousScopeService spontaneousScopeService;

    @Mock
    private Logger log;
    @Mock
    private StaticConfiguration staticConfiguration;
    @Mock
    private AppConfiguration appConfiguration;
    @Mock
    private ScopeService scopeService;

    @Test
    public void isAllowedBySpontaneousScopes_whenGlobalConfigReturnsFalse_shouldReturnFalse() {
        Client client = new Client();
        client.getAttributes().setAllowSpontaneousScopes(true);

        assertFalse(spontaneousScopeService.isAllowedBySpontaneousScopes(client, "scope"));
    }
}
