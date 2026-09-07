/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.util;

import io.jans.fido2.model.metric.Fido2MetricsData.DeviceInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for DeviceInfoExtractor
 *
 * @author Janssen Project
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceInfoExtractorTest {

    private static final String CHROME_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String SAFARI_MACOS =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15";
    private static final String SAFARI_IPHONE =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148";
    private static final String SAFARI_IPAD =
            "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15";
    private static final String FIREFOX_LINUX =
            "Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0";

    /**
     * The user agent the FIDO2 server actually sees today, since the Authorization Server
     * relays to it over a service-to-service HTTP client rather than the browser calling in.
     */
    private static final String APACHE_HTTP_CLIENT = "Apache-HttpClient/4.5.14 (Java/17.0.20)";

    @Mock
    private Logger log;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private DeviceInfoExtractor deviceInfoExtractor;

    /**
     * A non-browser caller must not be counted as a desktop. Before the UNKNOWN branch every
     * relayed call fell through to DESKTOP, so the device breakdown was populated entirely by
     * the calling service and looked plausible rather than empty.
     */
    @Test
    void testServiceToServiceUserAgentIsUnknownNotDesktop() {
        DeviceInfo deviceInfo = deviceInfoExtractor.extractDeviceInfo(APACHE_HTTP_CLIENT);

        assertEquals("UNKNOWN", deviceInfo.getDeviceType());
        assertEquals("Unknown", deviceInfo.getBrowser());
        assertEquals("Unknown", deviceInfo.getOperatingSystem());
        assertEquals(APACHE_HTTP_CLIENT, deviceInfo.getUserAgent());
    }

    @Test
    void testUnrecognisedUserAgentIsUnknown() {
        assertEquals("UNKNOWN", deviceInfoExtractor.extractDeviceInfo("curl/8.4.0").getDeviceType());
        assertEquals("UNKNOWN", deviceInfoExtractor.extractDeviceInfo("").getDeviceType());
    }

    @Test
    void testDesktopUserAgentsAreStillDesktop() {
        assertEquals("DESKTOP", deviceInfoExtractor.extractDeviceInfo(CHROME_WINDOWS).getDeviceType());
        assertEquals("DESKTOP", deviceInfoExtractor.extractDeviceInfo(SAFARI_MACOS).getDeviceType());
        assertEquals("DESKTOP", deviceInfoExtractor.extractDeviceInfo(FIREFOX_LINUX).getDeviceType());
    }

    /**
     * The desktop check includes a Linux marker, which Android user agents also carry. The
     * mobile test runs first, so an Android device must not be reclassified as a desktop.
     */
    @Test
    void testMobileAndTabletUserAgentsAreUnaffected() {
        assertEquals("MOBILE", deviceInfoExtractor.extractDeviceInfo(SAFARI_IPHONE).getDeviceType());
        assertEquals("TABLET", deviceInfoExtractor.extractDeviceInfo(SAFARI_IPAD).getDeviceType());
        assertEquals("MOBILE", deviceInfoExtractor.extractDeviceInfo(
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                .getDeviceType());
    }

    @Test
    void testBrowserAndOsAreParsedFromUserAgent() {
        DeviceInfo chrome = deviceInfoExtractor.extractDeviceInfo(CHROME_WINDOWS);
        assertEquals("Chrome", chrome.getBrowser());
        assertEquals("120.0.0.0", chrome.getBrowserVersion());
        assertEquals("Windows", chrome.getOperatingSystem());

        DeviceInfo safari = deviceInfoExtractor.extractDeviceInfo(SAFARI_MACOS);
        assertEquals("macOS", safari.getOperatingSystem());
        assertEquals("10.15.7", safari.getOsVersion());
    }

    /**
     * A missing user agent leaves the fields unset rather than filling in UNKNOWN, so the
     * entry is excluded from the breakdowns instead of forming a bucket of its own.
     */
    @Test
    void testNullUserAgentLeavesFieldsUnset() {
        DeviceInfo deviceInfo = deviceInfoExtractor.extractDeviceInfo((String) null);

        assertNotNull(deviceInfo);
        assertNull(deviceInfo.getDeviceType());
        assertNull(deviceInfo.getUserAgent());
        assertNull(deviceInfo.getBrowser());
    }

    @Test
    void testRequestOverloadReadsUserAgentHeader() {
        when(httpRequest.getHeader("User-Agent")).thenReturn(CHROME_WINDOWS);

        DeviceInfo deviceInfo = deviceInfoExtractor.extractDeviceInfo(httpRequest);

        assertEquals("DESKTOP", deviceInfo.getDeviceType());
        assertEquals(CHROME_WINDOWS, deviceInfo.getUserAgent());
    }

    @Test
    void testNullRequestReturnsNull() {
        assertNull(deviceInfoExtractor.extractDeviceInfo((HttpServletRequest) null));
    }

    @Test
    void testMinimalDeviceInfoUsesUnknownDeviceType() {
        DeviceInfo deviceInfo = deviceInfoExtractor.createMinimalDeviceInfo();

        assertEquals("UNKNOWN", deviceInfo.getDeviceType());
        assertEquals("Unknown", deviceInfo.getBrowser());
    }
}
