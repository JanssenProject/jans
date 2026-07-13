package io.jans.as.server.service.net;

import com.google.common.collect.Lists;
import io.jans.as.model.configuration.AppConfiguration;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.net.InetAddress;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class SectorIdentifierUriServiceTest {

    @InjectMocks
    @Spy
    private SectorIdentifierUriService sectorIdentifierUriService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void isAllowedSectorIdentifierUri_httpsWithoutBlockList_shouldReturnTrue() {
        doReturn(false).when(sectorIdentifierUriService).isPrivateOrUnresolvableHost(anyString());

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
        doReturn(false).when(sectorIdentifierUriService).isPrivateOrUnresolvableHost(anyString());
        when(appConfiguration.getRequestUriBlockList()).thenReturn(Lists.newArrayList("https://internal.example/*"));

        assertFalse(sectorIdentifierUriService.isAllowedSectorIdentifierUri("https://internal.example/sector.json"));
    }

    @Test
    public void isAllowedSectorIdentifierUri_httpsAndNotBlockListed_shouldReturnTrue() {
        doReturn(false).when(sectorIdentifierUriService).isPrivateOrUnresolvableHost(anyString());
        when(appConfiguration.getRequestUriBlockList()).thenReturn(Lists.newArrayList("https://internal.example/*"));

        assertTrue(sectorIdentifierUriService.isAllowedSectorIdentifierUri("https://rp.example/sector.json"));
    }

    @Test
    public void isAllowedSectorIdentifierUri_loopbackIpLiteral_shouldReturnFalseWithoutEvaluatingBlockList() {
        assertFalse(sectorIdentifierUriService.isAllowedSectorIdentifierUri("https://127.0.0.1/sector.json"));
    }

    @Test
    public void isAllowedSectorIdentifierUri_linkLocalIpLiteral_shouldReturnFalse() {
        assertFalse(sectorIdentifierUriService.isAllowedSectorIdentifierUri("https://169.254.169.254/latest/meta-data/"));
    }

    @Test
    public void isAllowedSectorIdentifierUri_privateNetworkIpLiteral_shouldReturnFalse() {
        assertFalse(sectorIdentifierUriService.isAllowedSectorIdentifierUri("https://10.0.0.5/sector.json"));
    }

    @Test
    public void isPrivateOrUnresolvableHost_loopbackIpLiteral_shouldReturnTrue() {
        assertTrue(sectorIdentifierUriService.isPrivateOrUnresolvableHost("127.0.0.1"));
    }

    @Test
    public void isPrivateOrUnresolvableHost_blankHost_shouldReturnTrue() {
        assertTrue(sectorIdentifierUriService.isPrivateOrUnresolvableHost(""));
    }

    @Test
    public void isPrivateOrUnresolvableHost_publicIpLiteral_shouldReturnFalse() {
        assertFalse(sectorIdentifierUriService.isPrivateOrUnresolvableHost("8.8.8.8"));
    }

    @Test
    public void isPrivateAddress_withLoopback_shouldReturnTrue() throws Exception {
        assertTrue(sectorIdentifierUriService.isPrivateAddress(InetAddress.getByName("127.0.0.1")));
    }

    @Test
    public void isPrivateAddress_with10Network_shouldReturnTrue() throws Exception {
        assertTrue(sectorIdentifierUriService.isPrivateAddress(InetAddress.getByName("10.0.0.1")));
    }

    @Test
    public void isPrivateAddress_with192168Network_shouldReturnTrue() throws Exception {
        assertTrue(sectorIdentifierUriService.isPrivateAddress(InetAddress.getByName("192.168.1.1")));
    }

    @Test
    public void isPrivateAddress_with172Network_shouldReturnTrue() throws Exception {
        assertTrue(sectorIdentifierUriService.isPrivateAddress(InetAddress.getByName("172.16.0.1")));
    }

    @Test
    public void isPrivateAddress_withLinkLocal_shouldReturnTrue() throws Exception {
        assertTrue(sectorIdentifierUriService.isPrivateAddress(InetAddress.getByName("169.254.169.254")));
    }

    @Test
    public void isPrivateAddress_withPublicAddress_shouldReturnFalse() throws Exception {
        assertFalse(sectorIdentifierUriService.isPrivateAddress(InetAddress.getByName("8.8.8.8")));
    }
}
