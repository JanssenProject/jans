package org.gluu.oxauth.client.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gluu.oxauth.client.util.Configuration;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;

/**
 * Listener to detect when an HTTP session is destroyed and remove it from the map of
 * managed sessions.  Also allows for the programmatic removal of sessions.
 * <p>
 * Enables the CAS Single Sign out feature.
 *
 * @author Yuriy Movchan
 * @version 0.1, 03/20/2013
 */
public final class SignOutHandler {

	protected final Log log = LogFactory.getLog(getClass());

	private static class SignOutHandlerSingleton {
		static SignOutHandler INSTANCE = new SignOutHandler();
	}

	private SignOutHandler() {}

	public static SignOutHandler instance() {
		return SignOutHandlerSingleton.INSTANCE;
	}

    public String getOAuthLogoutUrl(final HttpServletRequest servletRequest) {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpSession session = request.getSession(false);

        if (session == null) {
        	log.trace("There is no exising session");
        	return null;
        }

        OAuthData oAuthData = (OAuthData) session.getAttribute(Configuration.SESSION_OAUTH_DATA);
        if (oAuthData == null) {
        	log.trace("There is no OAuthData in the session");
        	return null;
        }
        
        // TODO: Validate access token
        ClientRequest clientRequest = new ClientRequest(Configuration.instance().getPropertyValue(Configuration.OAUTH_PROPERTY_LOGOUT_URL));

		clientRequest.queryParameter(Configuration.OAUTH_ID_TOKEN_HINT, oAuthData.getAccessToken());
		clientRequest.queryParameter(Configuration.OAUTH_POST_LOGOUT_REDIRECT_URI, constructRedirectUrl(request));

		// Remove OAuth data from session
        session.removeAttribute(Configuration.SESSION_OAUTH_DATA);

		try {
			return clientRequest.getUri();
		} catch (Exception ex) {
			log.error("Failed to prepare OAuth log out URL", ex);
		}

		return null;
    }

    protected final String constructRedirectUrl(final HttpServletRequest request) {
    	log.trace("Starting constructRedirectUrl");
    	String redirectUri = null;
    	String[] redirectUriParameters = (String[])request.getParameterMap().get(Configuration.OAUTH_POST_LOGOUT_REDIRECT_URI);
    	if(redirectUriParameters != null && redirectUriParameters.length > 0){
    		redirectUri = redirectUriParameters[0];
    	}
    	
    	log.trace("redirectUri from request = " + redirectUri);
    	if(redirectUri == null || redirectUri.equals("")){
	    	int serverPort = request.getServerPort();
	    	if ((serverPort == 80) || (serverPort == 443)) {
	    		redirectUri = String.format("%s://%s%s", request.getScheme(), request.getServerName(), "/identity");
	    	} else {
	    		redirectUri = String.format("%s://%s:%s%s", request.getScheme(), request.getServerName(), request.getServerPort(), "/identity");
	    	}
	    
    	}
    	log.trace("Final redirectUri = " + redirectUri);
    	return redirectUri;
    }
}
