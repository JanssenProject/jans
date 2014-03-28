package org.gluu.oxauth.client.session;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.gluu.oxauth.client.util.Configuration;
import org.gluu.oxauth.client.util.OAuthDataHolder;

/**
 * Places the OAuth data in a ThreadLocal such that other resources can access it that do not have access to the web tier session
 *
 * @author Yuriy Movchan
 * @version 0.1, 03/20/2013
 */
public final class OAuthThreadLocalFilter implements Filter {

    public void init(final FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpSession session = request.getSession(false);
        final OAuthData oAuthData = (OAuthData) (session == null ? request.getAttribute(Configuration.SESSION_OAUTH_DATA) : session.getAttribute(Configuration.SESSION_OAUTH_DATA));

        try {
            OAuthDataHolder.setOAuthData(oAuthData);
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            OAuthDataHolder.clear();
        }
    }

    public void destroy() {
    }
}
