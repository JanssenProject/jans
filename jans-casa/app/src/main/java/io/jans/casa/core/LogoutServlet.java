package io.jans.casa.core;

import io.jans.casa.misc.WebUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author jgomer
 */
@WebServlet("/autologout")
public class LogoutServlet extends HttpServlet {

    @Override
    public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        // Clean up authentication-related cookies
        cleanupAuthCookies(req, resp);
        
        // Invalidate session
        WebUtils.invalidateSession(req);
        
        // Set cache control headers
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Expires", "0");
        resp.setHeader("Content-Type", "text/html");
    }
    
    /**
     * Cleans up authentication-related cookies to prevent stale session issues
     */
    private void cleanupAuthCookies(HttpServletRequest req, HttpServletResponse resp) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                // Clean up authentication-related cookies
                if ("allowList".equals(cookie.getName()) || 
                    "JSESSIONID".equals(cookie.getName()) ||
                    cookie.getName().startsWith("casa_") ||
                    cookie.getName().startsWith("fido_")) {
                    
                    // Create a new cookie with the same name but expired
                    Cookie expiredCookie = new Cookie(cookie.getName(), "");
                    expiredCookie.setPath("/");
                    expiredCookie.setMaxAge(0); // Expire immediately
                    expiredCookie.setSecure(cookie.getSecure());
                    expiredCookie.setHttpOnly(cookie.isHttpOnly());
                    
                    // Set domain if it was set in the original cookie
                    if (cookie.getDomain() != null) {
                        expiredCookie.setDomain(cookie.getDomain());
                    }
                    
                    resp.addCookie(expiredCookie);
                }
            }
        }
    }

}
