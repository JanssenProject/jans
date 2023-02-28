package io.jans.as.server.service.net;

import io.jans.as.model.configuration.AppConfiguration;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class UriServiceTest {

    @InjectMocks
    private UriService uriService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void canCall_whenExternalUriWhiteListIsBlank_shouldReturnTrue() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(new ArrayList<>());

        assertTrue(uriService.canCall("http://example.com"));
    }

    @Test
    public void canCall_whenUriAllowedByExternalUriWhiteList_shouldReturnTrue() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("example.com"));

        assertTrue(uriService.canCall("http://example.com"));
    }

    @Test
    public void canCall_whenUriNotAllowedByExternalUriWhiteList_shouldReturnFalse() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("my.com"));

        assertFalse(uriService.canCall("http://example.com"));
    }
}
