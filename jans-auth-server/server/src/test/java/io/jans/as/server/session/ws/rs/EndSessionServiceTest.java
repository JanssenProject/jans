package io.jans.as.server.session.ws.rs;

import com.google.common.collect.Lists;
import io.jans.as.model.configuration.AppConfiguration;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class EndSessionServiceTest {

    @InjectMocks
    private EndSessionService endSessionService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void isUrlWhiteListed_whenClientWhiteListAllows_shouldReturnTrue() {
        when(appConfiguration.getClientWhiteList()).thenReturn(Lists.newArrayList("white.com"));

        assertTrue(endSessionService.isUrlWhiteListed("https://white.com/path"));
        assertTrue(endSessionService.isUrlWhiteListed("https://white.com/path?param=value"));
        assertTrue(endSessionService.isUrlWhiteListed("http://white.com/path?param=value"));
    }

    @Test
    public void isUrlWhiteListed_whenClientWhiteListDoesNotAllow_shouldReturnFalse() {
        when(appConfiguration.getClientWhiteList()).thenReturn(Lists.newArrayList("some.com"));

        assertFalse(endSessionService.isUrlWhiteListed("https://white.com/path"));
        assertFalse(endSessionService.isUrlWhiteListed("https://white.com/path?param=value"));
        assertFalse(endSessionService.isUrlWhiteListed("http://white.com/path?param=value"));
    }
}
