package io.jans.casa.core;

import io.jans.casa.misc.Utils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Optional;

/**
 * Service for handling session validation and cleanup to prevent stale session issues
 * that cause TouchID/passkey authentication problems on first login
 */
@ApplicationScoped
public class SessionValidationService {

    @Inject
    private Logger logger;

    /**
     * Validates if the current session is fresh and valid for authentication
     * @return true if session is valid, false if stale or invalid
     */
    public boolean isSessionValid() {
        try {
            HttpServletRequest request = Utils.managedBean(HttpServletRequest.class);
            HttpSession session = request.getSession(false);
            
            if (session == null) {
                logger.debug("No active session found");
                return false;
            }
            
            // Check session age
            long sessionAge = System.currentTimeMillis() - session.getCreationTime();
            long maxSessionAge = 30 * 60 * 1000; // 30 minutes
            
            if (sessionAge > maxSessionAge) {
                logger.debug("Session is too old ({} ms), considering stale", sessionAge);
                return false;
            }
            
            // Check for authentication state
            Object authUser = session.getAttribute("user");
            if (authUser == null) {
                logger.debug("No authenticated user in session");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.debug("Error validating session: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Cleans up stale session data and cookies to force fresh authentication
     */
    public void cleanupStaleSession() {
        try {
            HttpServletRequest request = Utils.managedBean(HttpServletRequest.class);
            HttpServletResponse response = Utils.managedBean(HttpServletResponse.class);
            
            // Clean up session
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
                logger.debug("Invalidated stale session");
            }
            
            // Clean up authentication-related cookies
            cleanupAuthCookies(request, response);
            
        } catch (Exception e) {
            logger.debug("Error cleaning up stale session: {}", e.getMessage());
        }
    }
    
    /**
     * Cleans up authentication-related cookies that might cause stale session issues
     */
    private void cleanupAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Arrays.stream(cookies)
                .filter(cookie -> isAuthCookie(cookie.getName()))
                .forEach(cookie -> {
                    Cookie expiredCookie = new Cookie(cookie.getName(), "");
                    expiredCookie.setPath("/");
                    expiredCookie.setMaxAge(0);
                    expiredCookie.setSecure(cookie.getSecure());
                    expiredCookie.setHttpOnly(cookie.isHttpOnly());
                    
                    if (cookie.getDomain() != null) {
                        expiredCookie.setDomain(cookie.getDomain());
                    }
                    
                    response.addCookie(expiredCookie);
                    logger.debug("Expired auth cookie: {}", cookie.getName());
                });
        }
    }
    
    /**
     * Checks if a cookie name is related to authentication
     */
    private boolean isAuthCookie(String cookieName) {
        return "allowList".equals(cookieName) ||
               "JSESSIONID".equals(cookieName) ||
               cookieName.startsWith("casa_") ||
               cookieName.startsWith("fido_") ||
               cookieName.startsWith("auth_");
    }
    
    /**
     * Validates device fingerprint data to ensure it's reliable for trusted device evaluation
     * @param deviceJson JSON string containing device fingerprint data
     * @return true if device data is valid, false otherwise
     */
    public boolean isDeviceDataValid(String deviceJson) {
        if (deviceJson == null || deviceJson.trim().isEmpty()) {
            logger.debug("Device data is null or empty");
            return false;
        }
        
        try {
            org.json.JSONObject device = new org.json.JSONObject(deviceJson);
            String browserName = device.optString("name");
            String browserVersion = device.optString("version");
            String osName = device.optString("osName");
            String osVersion = device.optString("osVersion");
            
            // Check for required fields
            if (browserName == null || browserName.trim().isEmpty() ||
                browserVersion == null || browserVersion.trim().isEmpty() ||
                osName == null || osName.trim().isEmpty() ||
                osVersion == null || osVersion.trim().isEmpty()) {
                
                logger.debug("Device data missing required fields");
                return false;
            }
            
            // Additional validation for Chrome
            if ("Chrome".equalsIgnoreCase(browserName)) {
                if (!browserVersion.matches("\\d+\\.\\d+\\.\\d+.*")) {
                    logger.debug("Chrome version format invalid: {}", browserVersion);
                    return false;
                }
                
                // Check for fallback values
                if (browserVersion.equals("0.0.0") || browserVersion.equals("100.0.0")) {
                    logger.debug("Chrome version appears to be fallback value: {}", browserVersion);
                    return false;
                }
            }
            
            // Check for suspicious OS version numbers
            if (osVersion.equals("0.0.0") || osVersion.equals("10.0.0") || 
                osVersion.equals("12.0.0") || osVersion.equals("5.0.0")) {
                logger.debug("OS version appears to be fallback value: {}", osVersion);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.debug("Error validating device data: {}", e.getMessage());
            return false;
        }
    }
}
