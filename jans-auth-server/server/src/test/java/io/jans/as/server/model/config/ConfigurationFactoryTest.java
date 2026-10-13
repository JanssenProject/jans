package io.jans.as.server.model.config;

import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.doReturn;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

@Listeners(MockitoTestNGListener.class)
public class ConfigurationFactoryTest {

    private static final String JWKS_URI = "https://ext.example.org/jwks";

    @Mock
    private Logger log;

    @Spy
    @InjectMocks
    private ConfigurationFactory configurationFactory;

    @Test
    public void initWebKeys_whenExternalFetchFailsAndFlagDisabled_shouldThrow() {
        doReturn(null).when(configurationFactory).fetchExternalWebKeys(JWKS_URI);

        expectThrows(IllegalStateException.class, () -> configurationFactory.initWebKeys(conf(false)));
    }

    @Test
    public void initWebKeys_whenExternalFetchReturnsNoKeysAndFlagDisabled_shouldThrow() {
        doReturn(new JSONObject()).when(configurationFactory).fetchExternalWebKeys(JWKS_URI);

        expectThrows(IllegalStateException.class, () -> configurationFactory.initWebKeys(conf(false)));
    }

    @Test
    public void initWebKeys_whenExternalFetchFailsAndFlagEnabled_shouldStartWithEmptyKeys() {
        doReturn(null).when(configurationFactory).fetchExternalWebKeys(JWKS_URI);

        configurationFactory.initWebKeys(conf(true));

        assertNotNull(configurationFactory.getWebKeysConfiguration());
        assertTrue(configurationFactory.getWebKeysConfiguration().getKeys().isEmpty());
    }

    @Test
    public void initWebKeys_whenExternalFetchFailsOnReloadAndFlagEnabled_shouldKeepPreviousKeys() {
        doReturn(new JSONObject("{\"keys\":[]}")).when(configurationFactory).fetchExternalWebKeys(JWKS_URI);
        configurationFactory.initWebKeys(conf(true));
        final Object previous = configurationFactory.getWebKeysConfiguration();

        doReturn(null).when(configurationFactory).fetchExternalWebKeys(JWKS_URI);
        configurationFactory.initWebKeys(conf(true));

        assertSame(configurationFactory.getWebKeysConfiguration(), previous);
    }

    @Test
    public void initWebKeys_whenExternalFetchSucceeds_shouldLoadKeys() {
        doReturn(new JSONObject("{\"keys\":[]}")).when(configurationFactory).fetchExternalWebKeys(JWKS_URI);

        configurationFactory.initWebKeys(conf(false));

        assertNotNull(configurationFactory.getWebKeysConfiguration());
        assertEquals(configurationFactory.getWebKeysConfiguration().getKeys().size(), 0);
    }

    private static Conf conf(boolean allowStartupOnJwksUriFetchFailure) {
        final AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setIssuer("https://as.example.org");
        appConfiguration.setJwksUri(JWKS_URI);
        appConfiguration.setAllowStartupOnJwksUriFetchFailure(allowStartupOnJwksUriFetchFailure);

        final Conf conf = new Conf();
        conf.setDynamic(appConfiguration);
        return conf;
    }
}
