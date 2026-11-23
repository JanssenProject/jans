package io.jans.as.server.rate;

import jakarta.servlet.annotation.WebFilter;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
public class RateLimitFilterTest {

    @Test
    public void webFilterMapping_shouldCoverTokenEndpoint() {
        assertTrue(isMapped("/restv1/token"), "RateLimitFilter must be mapped to /restv1/token");
    }

    @Test
    public void webFilterMapping_shouldCoverAuthorizeChallengeEndpoint() {
        assertTrue(isMapped("/restv1/authorize-challenge"), "RateLimitFilter must be mapped to /restv1/authorize-challenge");
    }

    @Test
    public void webFilterMapping_shouldCoverRegisterEndpoint() {
        assertTrue(isMapped("/restv1/register"), "RateLimitFilter must be mapped to /restv1/register");
    }

    private static boolean isMapped(String path) {
        WebFilter webFilter = RateLimitFilter.class.getAnnotation(WebFilter.class);
        assertNotNull(webFilter, "RateLimitFilter must be annotated with @WebFilter");

        for (String pattern : webFilter.urlPatterns()) {
            if (matches(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String pattern, String path) {
        if ("/*".equals(pattern)) {
            return true;
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
        return pattern.equals(path);
    }
}
