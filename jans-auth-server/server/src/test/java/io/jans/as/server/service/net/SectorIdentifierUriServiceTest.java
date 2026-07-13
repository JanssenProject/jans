package io.jans.as.server.service.net;

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
public class SectorIdentifierUriServiceTest {

    @InjectMocks
    private SectorIdentifierUriService sectorIdentifierUriService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void isAllowedSectorIdentifierUri_httpsWithoutBlockList_shouldReturnTrue() {
        assertTrue(sectorIdentifierUriService.isAllowedSectorIdentifierUri("https://rp.example/sector.json"));
    }

    @Test
    public void isAllowedSectorIdentifierUri_httpScheme_shouldReturnFalse() {
        assertFalse(sectorIdentifierUriService.isAllowedSectorIdentifierUri("http://169.254.169.254/latest/meta-data/"));
    }

    @Test
    public void isAllowedSectorIdentifierUri_nonHttpsScheme_shouldReturnFalseWithoutEvaluatingBlockList() {
        assertFalse(sectorIdentifierUriService.isAllowedSectorIdentifierUri("file:///etc/passwd"));
    }

    @Test
    public void isAllowedSectorIdentifierUri_malformedUri_shouldReturnFalse() {
        assertFalse(sectorIdentifierUriService.isAllowedSectorIdentifierUri("not a uri"));
    }

    @Test
    public void isAllowedSectorIdentifierUri_httpsAndBlockListed_shouldReturnFalse() {
        when(appConfiguration.getRequestUriBlockList()).thenReturn(Lists.newArrayList("https://internal.example/*"));

        assertFalse(sectorIdentifierUriService.isAllowedSectorIdentifierUri("https://internal.example/sector.json"));
    }

    @Test
    public void isAllowedSectorIdentifierUri_httpsAndNotBlockListed_shouldReturnTrue() {
        when(appConfiguration.getRequestUriBlockList()).thenReturn(Lists.newArrayList("https://internal.example/*"));

        assertTrue(sectorIdentifierUriService.isAllowedSectorIdentifierUri("https://rp.example/sector.json"));
    }
}
