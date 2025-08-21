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
        
        // Use centralized session validation service for cleanup
        io.jans.casa.core.SessionValidationService validationService = 
            io.jans.service.cdi.util.CdiUtil.bean(io.jans.casa.core.SessionValidationService.class);
        validationService.cleanupStaleSession();
        
        // Invalidate session
        WebUtils.invalidateSession(req);
        
        // Set cache control headers
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Expires", "0");
        resp.setHeader("Content-Type", "text/html");
    }

}
