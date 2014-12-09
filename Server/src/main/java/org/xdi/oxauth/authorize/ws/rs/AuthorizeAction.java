/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.authorize.ws.rs;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.FacesManager;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Identity;
import org.xdi.model.AuthenticationScriptUsageType;
import org.xdi.model.config.CustomAuthenticationConfiguration;
import org.xdi.oxauth.auth.Authenticator;
import org.xdi.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.xdi.oxauth.model.authorize.AuthorizeParamsValidator;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.SessionId;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.config.Constants;
import org.xdi.oxauth.model.custom.auth.ExternalAuthenticatorConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.federation.FederationTrust;
import org.xdi.oxauth.model.federation.FederationTrustStatus;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.util.LocaleUtil;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.AuthenticationService;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.ExternalAuthenticationService;
import org.xdi.oxauth.service.FederationDataService;
import org.xdi.oxauth.service.RedirectionUriService;
import org.xdi.oxauth.service.ScopeService;
import org.xdi.oxauth.service.SessionIdService;
import org.xdi.oxauth.service.UserGroupService;
import org.xdi.oxauth.service.UserService;
import org.xdi.util.StringHelper;

/**
 * @author Javier Rojas Blum Date: 11.21.2011
 * @author Yuriy Movchan Date: 10/01/2014
 */
@Name("authorizeAction")
@Scope(ScopeType.EVENT) // Do not change scope, we try to keep server without http sessions
public class AuthorizeAction {

    public static final List<String> ALLOWED_PARAMETER = Collections.unmodifiableList(Arrays.asList(
            "scope", "response_type", "client_id", "redirect_uri", "state", "nonce", "display", "prompt", "max_age",
            "ui_locales", "id_token_hint", "login_hint", "acr_values", "amr_values", "session_id", "request", "request_uri"));

    @Logger
    private Log log;

    @In
    private ClientService clientService;

    @In
    private ErrorResponseFactory errorResponseFactory;

    @In
    private UserGroupService userGroupService;

    @In
    private FederationDataService federationDataService;

    @In
    private SessionIdService sessionIdService;

    @In
    private UserService userService;

    @In
    private RedirectionUriService redirectionUriService;

    @In
    private AuthenticationService authenticationService;

    @In
    private ExternalAuthenticationService externalAuthenticationService;

    @In
    private SessionId sessionUser;

    @In("org.jboss.seam.international.localeSelector")
    private LocaleSelector localeSelector;

    @RequestParameter("auth_level")
    private String authLevel;

    @RequestParameter("auth_mode")
    private String authMode;

    @In
    private Identity identity;

    // OAuth 2.0 request parameters
    private String scope;
    private String responseType;
    private String clientId;
    private String redirectUri;
    private String state;

    // OpenID Connect request parameters
    private String nonce;
    private String display;
    private String prompt;
    private Integer maxAge;
    private String uiLocales;
    private String idTokenHint;
    private String loginHint;
    private String acrValues;
    private String amrValues;
    private String request;
    private String requestUri;

    // custom oxAuth parameters
    private String sessionId;

    public void checkUiLocales() {
        List<String> uiLocalesList = null;
        if (StringUtils.isNotBlank(uiLocales)) {
            uiLocalesList = Util.splittedStringAsList(uiLocales, " ");

            FacesContext facesContext = FacesContext.getCurrentInstance();
            List<Locale> supportedLocales = new ArrayList<Locale>();
            for (Iterator<Locale> it = facesContext.getApplication().getSupportedLocales(); it.hasNext(); ) {
                supportedLocales.add(it.next());
            }
            Locale matchingLocale = LocaleUtil.localeMatch(uiLocalesList, supportedLocales);

            if (matchingLocale != null) {
                localeSelector.setLocale(matchingLocale);
            }
        }
    }

    private SessionId getSession() {
        initSessionId();

        // todo : quickfix
        if (!identity.isLoggedIn()) {
            final Authenticator authenticator = (Authenticator) Component.getInstance(Authenticator.class, true);
            authenticator.authenticateBySessionId(sessionId);
        }
        SessionId ldapSessionId = sessionIdService.getSessionId(sessionId);
        if (ldapSessionId == null) {
        	identity.logout();
        }
        
        return ldapSessionId;
    }

    public void checkPermissionGranted() {
        final ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();

        final SessionId session = getSession();
        authenticationService.storeRequestHeadersInSession((HttpServletRequest) externalContext.getRequest());

        if (session == null || session.getUserDn() == null) {
            Map<String, String> parameterMap = externalContext.getRequestParameterMap();

            String redirectTo = "/login.xhtml";

            boolean useExternalAuthenticator = externalAuthenticationService.isEnabled(AuthenticationScriptUsageType.INTERACTIVE);
            if (useExternalAuthenticator) {
            	
            	List<String> acrValuesList = null;
            	try {
					acrValuesList = Util.jsonArrayStringAsList(this.acrValues);
				} catch (JSONException ex) {
					invalidRequest();
					return;
				}
            	
            	ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration;
            	
            	if ((acrValuesList != null) && !acrValuesList.isEmpty()) {
                	externalAuthenticatorConfiguration = externalAuthenticationService.determineExternalAuthenticatorConfiguration(AuthenticationScriptUsageType.INTERACTIVE, acrValuesList);
            	} else {
            		externalAuthenticatorConfiguration = externalAuthenticationService.determineExternalAuthenticatorConfiguration(AuthenticationScriptUsageType.INTERACTIVE, 1, this.authLevel, this.authMode);
            	}

                if (externalAuthenticatorConfiguration == null) {
                    log.error("Failed to get ExternalAuthenticatorConfiguration. auth_step: {0}, auth_mode: {1}, auth_level: {2}", 1, authMode, authLevel);
                    permissionDenied();
                    return;
                }

                this.authMode = externalAuthenticatorConfiguration.getName();

                // Set updated authentication parameters
                parameterMap = new HashMap<String, String>(parameterMap);
                parameterMap.remove("auth_level");

                parameterMap.put("auth_mode", this.authMode);
                parameterMap.put("auth_step", Integer.toString(1));

                String tmpRedirectTo = externalAuthenticationService.executeExternalAuthenticatorGetPageForStep(externalAuthenticatorConfiguration, 1);
                if (StringHelper.isNotEmpty(tmpRedirectTo)) {
                    log.trace("Redirect to custom authentication login page: {0}", tmpRedirectTo);
                    redirectTo = tmpRedirectTo;
                }
            }

            FacesManager.instance().redirect(redirectTo, (Map) parameterMap, false);
            return;
        }

        if (clientId != null && !clientId.isEmpty()) {
            final Client client = clientService.getClient(clientId);

            if (client != null) {
                if (StringUtils.isBlank(redirectionUriService.validateRedirectionUri(clientId, redirectUri))) {
                    permissionDenied();
                }

                final User user = userService.getUserByDn(session.getUserDn());
                log.trace("checkPermissionGranted, user = " + user);

                // OXAUTH-87 : if user is not in group then deny permission
                if (user != null && client.hasUserGroups()) {
                    // if user is not in any group then deny permissions
                    if (!userGroupService.isInAnyGroup(client.getUserGroups(), user.getDn())) {
                        permissionDenied();
                    }
                }

                // OXAUTH-88 : federation support
                if (ConfigurationFactory.getConfiguration().getFederationEnabled()) {
                    final List<FederationTrust> list = federationDataService.getTrustByClient(client, FederationTrustStatus.ACTIVE);

                    if (list == null || list.isEmpty()) {
                        log.trace("Deny authorization, client is not in any federation trust, client: {0}", client.getDn());
                        permissionDenied();
                    } else if (FederationDataService.skipAuthorization(list)) {
                        log.trace("Skip authorization (permissions granted), client is in federation trust where skip is allowed, client: {1}", client.getDn());
                        permissionGranted();
                    }
                }

                List<Prompt> prompts = Prompt.fromString(prompt, " ");
                if (AuthorizeParamsValidator.validatePrompt(prompts)) {
                    // if trusted client = true, then skip authorization page and grant access directly
                    if (ConfigurationFactory.getConfiguration().getTrustedClientEnabled()) {
                        if (Boolean.parseBoolean(client.getTrustedClient()) && !prompts.contains(Prompt.CONSENT)) {
                            permissionGranted();
                        }
                    } else {
                        consentRequired();
                    }
                } else {
                    invalidRequest();
                }
            }
        }
    }

    private void initSessionId() {
        if (StringUtils.isBlank(sessionId)) {
            try {
                final Object request = FacesContext.getCurrentInstance().getExternalContext().getRequest();
                sessionId = SessionIdService.instance().getSessionIdFromCookie((HttpServletRequest) request);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Observer(Constants.EVENT_OXAUTH_CUSTOM_LOGIN_SUCCESSFUL)
    public void onSuccessfulLogin(String authMode, Map<String, String> requestParameterMap) {
        onSuccessfulLoginImpl(authMode, requestParameterMap);
    }

    @Observer(Identity.EVENT_LOGIN_SUCCESSFUL)
    public void onSuccessfulLogin() {
        onSuccessfulLoginImpl(null, null);
    }

    public void onSuccessfulLoginImpl(String authMode, Map<String, String> requestParameterMap) {
        log.info("Attempting to redirect user. SessionUser: {0}", sessionUser);

        User user = sessionUser != null && StringUtils.isNotBlank(sessionUser.getUserDn()) ?
                userService.getUserByDn(sessionUser.getUserDn()) : null;
        if (sessionUser != null) {
            sessionUser.setAuthenticationTime(new Date());
        }
        sessionIdService.updateSessionWithLastUsedDate(sessionUser, Prompt.fromString(prompt, " "));

        log.info("Attempting to redirect user. User: {0}", user);

        if (user != null) {
            final Map<String, Object> map;
            if (requestParameterMap == null) {
                map = parametersForRedirect(user);
            } else {
                map = parametersForRedirect(user, requestParameterMap);
            }

            addAuthModeParameters(map, authMode);

            log.trace("Logged in successfully! User: {0}, page: /authorize.xhtml, map: {1}", user, map);
            FacesManager.instance().redirect("/authorize.xhtml", map, false);
        }
    }

    private Map<String, Object> parametersForRedirect(User p_user) {
        final Map<String, String> parameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        return parametersForRedirect(p_user, parameterMap);
    }

    private Map<String, Object> parametersForRedirect(User p_user, final Map<String, String> requestParameterMap) {
        final Map<String, Object> result = new HashMap<String, Object>();
        if (requestParameterMap != null && !requestParameterMap.isEmpty()) {
            final Set<Map.Entry<String, String>> set = requestParameterMap.entrySet();
            for (Map.Entry<String, String> entry : set) {
                if (ALLOWED_PARAMETER.contains(entry.getKey())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }

        if (!result.isEmpty()) {
            if (sessionUser == null || sessionUser.getId() == null) {
                sessionUser = sessionIdService.generateSessionIdInteractive(p_user.getDn());
            }

            result.put("session_id", sessionUser.getId());
        }

        return result;
    }

    public void addAuthModeParameters(final Map<String, Object> parameterMap, String authMode) {
        // Add authentication mode and authentication level parameters
        if (StringHelper.isNotEmpty(authMode)) {
            ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration = externalAuthenticationService.getExternalAuthenticatorConfiguration(AuthenticationScriptUsageType.INTERACTIVE, authMode);
            if (externalAuthenticatorConfiguration != null) {
                parameterMap.put("auth_mode", externalAuthenticatorConfiguration.getName());
                parameterMap.put("auth_level", externalAuthenticatorConfiguration.getLevel());
            }
        }
    }

    public List<org.xdi.oxauth.model.common.Scope> getScopes() {
        List<org.xdi.oxauth.model.common.Scope> scopes = new ArrayList<org.xdi.oxauth.model.common.Scope>();
        ScopeService scopeService = ScopeService.instance();

        if (scope != null && !scope.isEmpty()) {
            String[] scopesName = scope.split(" ");
            for (String scopeName : scopesName) {
                org.xdi.oxauth.model.common.Scope s = scopeService.getScopeByDisplayName(scopeName);
                if (s != null && s.getDescription() != null) {
                    scopes.add(s);
                }
            }
        }

        return scopes;
    }

    /**
     * Returns the scope of the access request.
     *
     * @return The scope of the access request.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Sets the scope of the access request.
     *
     * @param scope The scope of the access request.
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Returns the response type: <code>code</code> for requesting an authorization code (authorization code grant) or
     * <strong>token</strong> for requesting an access token (implicit grant).
     *
     * @return The response type.
     */
    public String getResponseType() {
        return responseType;
    }

    /**
     * Sets the response type.
     *
     * @param responseType The response type.
     */
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    /**
     * Returns the client identifier.
     *
     * @return The client identifier.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the client identifier.
     *
     * @param clientId The client identifier.
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Returns the redirection URI.
     *
     * @return The redirection URI.
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Sets the redirection URI.
     *
     * @param redirectUri The redirection URI.
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * Returns an opaque value used by the client to maintain state between the request and callback. The authorization
     * server includes this value when redirecting the user-agent back to the client. The parameter should be used for
     * preventing cross-site request forgery.
     *
     * @return The state between the request and callback.
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the state between the request and callback.
     *
     * @param state The state between the request and callback.
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Return a string value used to associate a user agent session with an ID Token, and to mitigate replay attacks.
     *
     * @return The nonce value.
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Sets a string value used to associate a user agent session with an ID Token, and to mitigate replay attacks.
     *
     * @param nonce The nonce value.
     */
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    /**
     * Returns an ASCII string value that specifies how the Authorization Server displays the authentication page
     * to the End-User.
     *
     * @return The display value.
     */
    public String getDisplay() {
        return display;
    }

    /**
     * Sets an ASCII string value that specifies how the Authorization Server displays the authentication page
     * to the End-User.
     *
     * @param display The display value
     */
    public void setDisplay(String display) {
        this.display = display;
    }

    /**
     * Returns a space delimited list of ASCII strings that can contain the values
     * login, consent, select_account, and none.
     *
     * @return A list of prompt options.
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Sets a space delimited list of ASCII strings that can contain the values
     * login, consent, select_account, and none.
     *
     * @param prompt A list of prompt options.
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public String getUiLocales() {
        return uiLocales;
    }

    public void setUiLocales(String uiLocales) {
        this.uiLocales = uiLocales;
    }

    public String getIdTokenHint() {
        return idTokenHint;
    }

    public void setIdTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

	public String getAmrValues() {
		return amrValues;
	}

	public void setAmrValues(String amrValues) {
		this.amrValues = amrValues;
	}

	/**
     * Returns a JWT encoded OpenID Request Object.
     *
     * @return A JWT encoded OpenID Request Object.
     */
    public String getRequest() {
        return request;
    }

    /**
     * Sets a JWT encoded OpenID Request Object.
     *
     * @param request A JWT encoded OpenID Request Object.
     */
    public void setRequest(String request) {
        this.request = request;
    }

    /**
     * Returns an URL that points to an OpenID Request Object.
     *
     * @return An URL that points to an OpenID Request Object.
     */
    public String getRequestUri() {
        return requestUri;
    }

    /**
     * Sets an URL that points to an OpenID Request Object.
     *
     * @param requestUri An URL that points to an OpenID Request Object.
     */
    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String p_sessionId) {
        sessionId = p_sessionId;
    }

    public void permissionGranted() {
        try {
            SessionId session = getSession();
            session.addPermission(clientId, true);
            sessionIdService.updateSessionWithLastUsedDate(session, Prompt.fromString(prompt, " "));

            // OXAUTH-297 - set session_id cookie, secure=true
            SessionIdService.instance().createSessionIdCookie(sessionId);

            final String parametersAsString = authenticationService.parametersAsString();
            final String uri = "seam/resource/restv1/oxauth/authorize?" + parametersAsString;
            log.trace("permissionGranted, redirectTo: {0}", uri);
            FacesManager.instance().redirectToExternalURL(uri);
        } catch (UnsupportedEncodingException e) {
            log.trace(e.getMessage(), e);
        }
    }

    public void permissionDenied() {
        log.trace("permissionDenied");
        StringBuilder sb = new StringBuilder();

        sb.append(redirectUri);
        if (redirectUri != null && redirectUri.contains("?")) {
            sb.append("&");
        } else {
            sb.append("?");
        }
        sb.append(errorResponseFactory.getErrorAsQueryString(AuthorizeErrorResponseType.ACCESS_DENIED,
                getState()));

        FacesManager.instance().redirectToExternalURL(sb.toString());
    }

    public void invalidRequest() {
        log.trace("invalidRequest");
        StringBuilder sb = new StringBuilder();

        sb.append(redirectUri);
        if (redirectUri != null && redirectUri.contains("?")) {
            sb.append("&");
        } else {
            sb.append("?");
        }
        sb.append(errorResponseFactory.getErrorAsQueryString(AuthorizeErrorResponseType.INVALID_REQUEST,
                getState()));

        FacesManager.instance().redirectToExternalURL(sb.toString());
    }

    public void consentRequired() {
        StringBuilder sb = new StringBuilder();

        sb.append(redirectUri);
        if (redirectUri != null && redirectUri.contains("?")) {
            sb.append("&");
        } else {
            sb.append("?");
        }
        sb.append(errorResponseFactory.getErrorAsQueryString(AuthorizeErrorResponseType.CONSENT_REQUIRED, getState()));

        FacesManager.instance().redirectToExternalURL(sb.toString());
    }
}