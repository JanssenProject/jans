/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.service.util;

import io.jans.fido2.model.metric.Fido2MetricsData.DeviceInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for extracting device information from HTTP requests
 * for FIDO2 metrics collection
 *
 * @author Janssen Project
 * @version 1.0
 */
@Singleton
public class DeviceInfoExtractor {
    
    private static final String UNKNOWN_VALUE = "Unknown";

    @Inject
    private Logger log;

    // Common browser patterns
    private static final Pattern CHROME_PATTERN = Pattern.compile("Chrome/([\\d.]+)");
    private static final Pattern FIREFOX_PATTERN = Pattern.compile("Firefox/([\\d.]+)");
    private static final Pattern SAFARI_PATTERN = Pattern.compile("Safari/([\\d.]+)");
    private static final Pattern EDGE_PATTERN = Pattern.compile("Edg/([\\d.]+)");
    private static final Pattern OPERA_PATTERN = Pattern.compile("OPR/([\\d.]+)");
    
    // OS patterns
    private static final Pattern WINDOWS_PATTERN = Pattern.compile("Windows NT ([\\d.]+)");
    private static final Pattern MAC_PATTERN = Pattern.compile("Mac OS X ([\\d._]+)");
    private static final Pattern LINUX_PATTERN = Pattern.compile("Linux");
    private static final Pattern ANDROID_PATTERN = Pattern.compile("Android ([\\d.]+)");
    private static final Pattern IOS_PATTERN = Pattern.compile("iPhone OS ([\\d._]+)");

    /**
     * Extract device information from HTTP request
     *
     * @param request HTTP request
     * @return DeviceInfo object with extracted information
     */
    public DeviceInfo extractDeviceInfo(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        DeviceInfo deviceInfo = new DeviceInfo();
        String userAgent = request.getHeader("User-Agent");
        
        if (userAgent != null) {
            deviceInfo.setUserAgent(userAgent);
            extractBrowserInfo(deviceInfo, userAgent);
            extractOSInfo(deviceInfo, userAgent);
            determineDeviceType(deviceInfo, userAgent);
        }

        return deviceInfo;
    }

    /**
     * Extract browser information from user agent string
     */
    private void extractBrowserInfo(DeviceInfo deviceInfo, String userAgent) {
        // Chrome
        Matcher chromeMatcher = CHROME_PATTERN.matcher(userAgent);
        if (chromeMatcher.find()) {
            deviceInfo.setBrowser("Chrome");
            deviceInfo.setBrowserVersion(chromeMatcher.group(1));
            return;
        }

        // Firefox
        Matcher firefoxMatcher = FIREFOX_PATTERN.matcher(userAgent);
        if (firefoxMatcher.find()) {
            deviceInfo.setBrowser("Firefox");
            deviceInfo.setBrowserVersion(firefoxMatcher.group(1));
            return;
        }

        // Safari
        Matcher safariMatcher = SAFARI_PATTERN.matcher(userAgent);
        if (safariMatcher.find()) {
            deviceInfo.setBrowser("Safari");
            deviceInfo.setBrowserVersion(safariMatcher.group(1));
            return;
        }

        // Edge
        Matcher edgeMatcher = EDGE_PATTERN.matcher(userAgent);
        if (edgeMatcher.find()) {
            deviceInfo.setBrowser("Edge");
            deviceInfo.setBrowserVersion(edgeMatcher.group(1));
            return;
        }

        // Opera
        Matcher operaMatcher = OPERA_PATTERN.matcher(userAgent);
        if (operaMatcher.find()) {
            deviceInfo.setBrowser("Opera");
            deviceInfo.setBrowserVersion(operaMatcher.group(1));
            return;
        }

        // Default fallback
        deviceInfo.setBrowser(UNKNOWN_VALUE);
        deviceInfo.setBrowserVersion(UNKNOWN_VALUE);
    }

    /**
     * Extract operating system information from user agent string
     */
    private void extractOSInfo(DeviceInfo deviceInfo, String userAgent) {
        // Windows
        Matcher windowsMatcher = WINDOWS_PATTERN.matcher(userAgent);
        if (windowsMatcher.find()) {
            deviceInfo.setOperatingSystem("Windows");
            deviceInfo.setOsVersion(windowsMatcher.group(1));
            return;
        }

        // macOS
        Matcher macMatcher = MAC_PATTERN.matcher(userAgent);
        if (macMatcher.find()) {
            deviceInfo.setOperatingSystem("macOS");
            deviceInfo.setOsVersion(macMatcher.group(1).replace("_", "."));
            return;
        }

        // Linux
        if (LINUX_PATTERN.matcher(userAgent).find()) {
            deviceInfo.setOperatingSystem("Linux");
            deviceInfo.setOsVersion(UNKNOWN_VALUE);
            return;
        }

        // Android
        Matcher androidMatcher = ANDROID_PATTERN.matcher(userAgent);
        if (androidMatcher.find()) {
            deviceInfo.setOperatingSystem("Android");
            deviceInfo.setOsVersion(androidMatcher.group(1));
            return;
        }

        // iOS
        Matcher iosMatcher = IOS_PATTERN.matcher(userAgent);
        if (iosMatcher.find()) {
            deviceInfo.setOperatingSystem("iOS");
            deviceInfo.setOsVersion(iosMatcher.group(1).replace("_", "."));
            return;
        }

        // Default fallback
        deviceInfo.setOperatingSystem(UNKNOWN_VALUE);
        deviceInfo.setOsVersion(UNKNOWN_VALUE);
    }

    /**
     * Determine device type based on user agent
     */
    private void determineDeviceType(DeviceInfo deviceInfo, String userAgent) {
        String userAgentLower = userAgent.toLowerCase();
        
        if (userAgentLower.contains("mobile") || userAgentLower.contains("android") || userAgentLower.contains("iphone")) {
            deviceInfo.setDeviceType("MOBILE");
        } else if (userAgentLower.contains("tablet") || userAgentLower.contains("ipad")) {
            deviceInfo.setDeviceType("TABLET");
        } else {
            deviceInfo.setDeviceType("DESKTOP");
        }
    }

    /**
     * Create a minimal device info object for cases where full extraction isn't needed
     */
    public DeviceInfo createMinimalDeviceInfo() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setBrowser(UNKNOWN_VALUE);
        deviceInfo.setBrowserVersion(UNKNOWN_VALUE);
        deviceInfo.setOperatingSystem(UNKNOWN_VALUE);
        deviceInfo.setOsVersion(UNKNOWN_VALUE);
        deviceInfo.setDeviceType("UNKNOWN");
        return deviceInfo;
    }
}
