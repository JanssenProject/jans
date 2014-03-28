package org.gluu.oxauth.client.util;

import java.io.IOException;
import java.security.Principal;

import javax.activation.FileTypeMap;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.gluu.oxauth.client.authentication.SimplePrincipal;
import org.gluu.oxauth.client.session.OAuthData;

/**
 * Implementation of a filter that wraps the normal HttpServletRequest with a
 * wrapper that overrides the following methods to provide data from the
 * OAuth data:
 * <ul>
 * <li>{@link HttpServletRequest#getUserPrincipal()}</li>
 * <li>{@link HttpServletRequest#getRemoteUser()}</li>
 * </ul>
 * <p/>
 * This filter needs to be configured in the chain so that it executes after
 * both the authentication and the validation filters.
 *
 * @author Yuriy Movchan
 * @version 0.1, 03/20/2013
 */
public final class HttpServletRequestWrapperFilter extends AbstractConfigurationFilter {

    public void init(final FilterConfig filterConfig) throws ServletException {
    }

    public void destroy() {
    }

    /**
     * Wraps the HttpServletRequest in a wrapper class that delegates
     * <code>request.getRemoteUser</code> to the underlying OAuthData object
     * stored in the user session.
     */
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        final SimplePrincipal principal = retrievePrincipalFromSessionOrRequest(servletRequest);
        
        if (principal == null) {
            log.trace("There is no principal");
        	filterChain.doFilter(servletRequest, servletResponse);
        } else {
        	filterChain.doFilter(new OAuthHttpServletRequestWrapper((HttpServletRequest) servletRequest, principal), servletResponse);
        }
    }

    protected SimplePrincipal retrievePrincipalFromSessionOrRequest(final ServletRequest servletRequest) {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpSession session = request.getSession(false);
        final OAuthData oAuthData = (OAuthData) (session == null ? request.getAttribute(Configuration.SESSION_OAUTH_DATA) : session.getAttribute(Configuration.SESSION_OAUTH_DATA));

        if (oAuthData == null) {
            log.trace("There is no OAuthData in session");
        	return null;
        }
        
        SimplePrincipal principanl = new SimplePrincipal(oAuthData.getUserUid()); 

        return principanl;
    }

    private final class OAuthHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private final SimplePrincipal principal;

        private OAuthHttpServletRequestWrapper(final HttpServletRequest request, final SimplePrincipal principal) {
            super(request);
            this.principal = principal;
        }

        public Principal getUserPrincipal() {
            return this.principal;
        }

        public String getRemoteUser() {
            return principal != null ? this.principal.getName() : null;
        }
    }
}
