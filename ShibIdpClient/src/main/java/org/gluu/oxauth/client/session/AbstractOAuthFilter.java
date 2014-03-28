package org.gluu.oxauth.client.session;

import javax.servlet.http.HttpServletRequest;

import org.gluu.oxauth.client.util.AbstractConfigurationFilter;

/**
 *  Abstract filter that contains code that is common to all OAuth filters.
 *
 * @author Yuriy Movchan
 * @version 0.1, 03/20/2013
 */
public abstract class AbstractOAuthFilter extends AbstractConfigurationFilter {

    protected final String constructRedirectUrl(final HttpServletRequest request) {
    	int serverPort = request.getServerPort();

    	String redirectUrl;
    	if ((serverPort == 80) || (serverPort == 443)) {
    		redirectUrl = String.format("%s://%s%s%s", request.getScheme(), request.getServerName(), request.getContextPath(), "/auth-code.jsp");
    	} else {
    		redirectUrl = String.format("%s://%s:%s%s%s", request.getScheme(), request.getServerName(), request.getServerPort(), request.getContextPath(), "/auth-code.jsp");
    	}
    	
    	return redirectUrl.toLowerCase();
    }

    /**
     * Method for retrieving a parameter from the request without disrupting the reader UNLESS the parameter
     * actually exists in the query string.
     *
     * @param request the request to check.
     * @param parameter the parameter to look for.
     * @return the value of the parameter.
     */
    public static String getParameter(final HttpServletRequest request, final String parameter) {
        return request.getQueryString() == null || request.getQueryString().indexOf(parameter) == -1 ? null : request.getParameter(parameter);       
    }

}
