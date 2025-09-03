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
        resp.setHeader("Content-Type", "text/html; charset=UTF-8");
        
        // Redirect to login page
        resp.sendRedirect(req.getContextPath() + "/");
        
    }
    
    /**
     * Cleans up authentication-related cookies to prevent stale session issues
     */
    private void cleanupAuthCookies(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    String name = cookie.getName();
                    // Remove authentication and session related cookies
                    if (name != null && (
                        name.startsWith("JSESSIONID") ||
                        name.startsWith("auth_") ||
                        name.startsWith("session_") ||
                        name.startsWith("casa_") ||
                        name.equals("remember-me") ||
                        name.equals("auth_token")
                    )) {
                        cookie.setValue("");
                        cookie.setPath("/");
                        cookie.setMaxAge(0);
                        resp.addCookie(cookie);
                    }
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the logout process
            System.err.println("Error cleaning up cookies: " + e.getMessage());
        }
    }
}
