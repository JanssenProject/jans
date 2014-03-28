package org.gluu.oxauth.client.validation;

import org.gluu.oxauth.client.session.AbstractOAuthFilter;
import org.gluu.oxauth.client.session.OAuthData;
import org.gluu.oxauth.client.util.Configuration;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.util.AssertionHelper;
import org.xdi.util.StringHelper;
import org.xdi.util.security.StringEncrypter;
import org.xdi.util.security.StringEncrypter.EncryptionException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Validates grants recieved from OAuth server
 *
 * @author Yuriy Movchan
 * @version 0.1, 03/20/2013
 */
public class OAuthValidationFilter extends AbstractOAuthFilter {

    /**
     * OAuth properties
     */
    private String oAuthAuthorizeUrl;
    private String oAuthTokenUrl;
    private String oAuthValidationUrl;
    private String oAuthCheckSessionUrl;
    private String oAuthUserInfoUrl;

    private String oAuthHost;

    private String oAuthClientId;
    private String oAuthClientPassword;

    public final void init(final FilterConfig filterConfig) throws ServletException {
        this.oAuthAuthorizeUrl = getPropertyFromInitParams(filterConfig, Configuration.OAUTH_PROPERTY_AUTHORIZE_URL, null);
        if (this.oAuthAuthorizeUrl != null) {
            this.oAuthHost = getOAuthHost(this.oAuthAuthorizeUrl);
        }

        this.oAuthTokenUrl = getPropertyFromInitParams(filterConfig, Configuration.OAUTH_PROPERTY_TOKEN_URL, null);
        this.oAuthValidationUrl = getPropertyFromInitParams(filterConfig, Configuration.OAUTH_PROPERTY_TOKEN_VALIDATION_URL, null);
        this.oAuthCheckSessionUrl = getPropertyFromInitParams(filterConfig, Configuration.OAUTH_PROPERTY_CHECKSESSION_URL, null);
        this.oAuthUserInfoUrl = getPropertyFromInitParams(filterConfig, Configuration.OAUTH_PROPERTY_USERINFO_URL, null);

        this.oAuthClientId = getPropertyFromInitParams(filterConfig, Configuration.OAUTH_PROPERTY_CLIENT_ID, null);
        this.oAuthClientPassword = getPropertyFromInitParams(filterConfig, Configuration.OAUTH_PROPERTY_CLIENT_PASSWORD, null);

        if (this.oAuthClientPassword != null) {
            try {
                this.oAuthClientPassword = StringEncrypter.defaultInstance().decrypt(this.oAuthClientPassword);
            } catch (EncryptionException ex) {
                log.error("Failed to decrypt property: " + Configuration.OAUTH_PROPERTY_CLIENT_PASSWORD, ex);
            }
        }

        AssertionHelper.assertNotNull(this.oAuthAuthorizeUrl, Configuration.OAUTH_PROPERTY_AUTHORIZE_URL + "cannot be null");
    }

    public final void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain)
            throws IOException, ServletException {
        log.debug("Attempting to validate grants");
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;

        // TODO: check chain
        if (!preFilter(servletRequest, servletResponse, filterChain)) {
            filterChain.doFilter(request, response);
            return;
        }

        final HttpSession session = request.getSession(false);

        final String code = getParameter(request, Configuration.OAUTH_CODE);
        final String idToken = getParameter(request, Configuration.OAUTH_ID_TOKEN);

        log.debug("Attempting to validate code: " + code + " and id_token: " + idToken);
        try {
            OAuthData oAuthData = getOAuthData(request, code, idToken);
            session.setAttribute(Configuration.SESSION_OAUTH_DATA, oAuthData);
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            log.warn("Failed to validate code and id_token", ex);

            throw new ServletException(ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determine filter execution conditions
     */
    protected final boolean preFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                                      final FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        final String code = getParameter(request, Configuration.OAUTH_CODE);
        final String idToken = getParameter(request, Configuration.OAUTH_ID_TOKEN);
        if (StringHelper.isNotEmpty(code) && (StringHelper.isNotEmpty(idToken))) {
            return true;
        }

        return false;
    }

    private OAuthData getOAuthData(HttpServletRequest request, String authorizationCode, String idToken) throws Exception {
        String scopes = getParameter(request, Configuration.OAUTH_SCOPE);
        log.trace("scopes : " + scopes);

        // 1. Request access token using the authorization code
        log.trace("Getting access token");
        TokenClient tokenClient1 = new TokenClient(oAuthTokenUrl);

        String redirectURL = constructRedirectUrl(request);
        TokenResponse tokenResponse = tokenClient1.execAuthorizationCode(authorizationCode, redirectURL, oAuthClientId, oAuthClientPassword);

        log.trace("tokenResponse : " + tokenResponse);
        log.trace("tokenResponse.getErrorType() : " + tokenResponse.getErrorType());

        String accessToken = tokenResponse.getAccessToken();
        log.trace("accessToken : " + accessToken);

        // 2. Validate the access token
        log.trace("Validating access token ");
        ValidateTokenClient validateTokenClient = new ValidateTokenClient(this.oAuthValidationUrl);
        ValidateTokenResponse tokenValidationResponse = validateTokenClient.execValidateToken(accessToken);
        log.trace(" response3.getStatus() : " + tokenValidationResponse.getStatus());

        log.info("validate check session status:" + tokenValidationResponse.getStatus());
        if (tokenValidationResponse.getErrorDescription() != null) {
            log.error("validate token status message:" + tokenValidationResponse.getErrorDescription());
        }

        if (tokenValidationResponse.getStatus() == 200) {
            log.info("Session validation successful. User is logged in");
            UserInfoClient userInfoClient = new UserInfoClient(oAuthUserInfoUrl);
            UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

            OAuthData oAuthData = new OAuthData();
            oAuthData.setHost(oAuthHost);
            // Determine uid
            List<String> uidValues = userInfoResponse.getClaims().get(JwtClaimName.SUBJECT_IDENTIFIER);
            if ((uidValues == null) || (uidValues.size() == 0)) {
                log.error("User infor response doesn't contains uid claim");
                return null;
            }

            oAuthData.setUserUid(uidValues.get(0));
            oAuthData.setAccessToken(accessToken);
            oAuthData.setAccessTokenExpirationInSeconds(tokenValidationResponse.getExpiresIn());
            oAuthData.setScopes(scopes);
            oAuthData.setIdToken(idToken);

            log.trace("User uid:" + oAuthData.getUserUid());
            return oAuthData;
        }

        log.error("Token validation failed. User is NOT logged in");
        return null;
    }

    private String getOAuthHost(String oAuthAuthorizeUrl) {
        try {
            URL url = new URL(oAuthAuthorizeUrl);
            return String.format("%s://%s:%s", url.getProtocol(), url.getHost(), url.getPort());
        } catch (MalformedURLException ex) {
            log.error("Invalid oAuth authorization URI: " + oAuthAuthorizeUrl, ex);
        }

        return null;
    }

    public void destroy() {
    }

}
