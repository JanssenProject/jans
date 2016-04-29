/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.authorize.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesManager;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Identity;
import org.xdi.model.AuthenticationScriptUsageType;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.auth.Authenticator;
import org.xdi.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.xdi.oxauth.model.authorize.AuthorizeParamsValidator;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.SessionIdState;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.config.Constants;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.federation.FederationTrust;
import org.xdi.oxauth.model.federation.FederationTrustStatus;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.ldap.ClientAuthorizations;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.util.LocaleUtil;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.*;
import org.xdi.oxauth.service.external.ExternalAuthenticationService;
import org.xdi.service.net.NetworkService;
import org.xdi.util.StringHelper;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version December 15, 2015
 */
@Name("authorizeAction")
@Scope(ScopeType.EVENT) // Do not change scope, we try to keep server without http sessions
public class AuthorizeAction {

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
    private SessionStateService sessionStateService;

    @In
    private UserService userService;

    @In
    private RedirectionUriService redirectionUriService;

    @In
    private AuthenticationService authenticationService;

    @In
    private ClientAuthorizationsService clientAuthorizationsService;

    @In
    private ExternalAuthenticationService externalAuthenticationService;

    @In(value = AppInitializer.DEFAULT_AUTH_MODE_NAME, required = false)
    private String defaultAuthenticationMethod;

    @In("org.jboss.seam.international.localeSelector")
    private LocaleSelector localeSelector;

    @In
    private NetworkService networkService;

    @In
    private Identity identity;

    // OAuth 2.0 request parameters
    private String scope;
    private String responseType;
    private String clientId;
    private String redirectUri;
    private String state;

    // OpenID Connect request parameters
    private String responseMode;
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
    private String sessionState;

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

    public String checkPermissionGranted() {
        SessionState session = getSession();
        List<Prompt> prompts = Prompt.fromString(prompt, " ");

        try {
            session = sessionStateService.assertAuthenticatedSessionCorrespondsToNewRequest(session, redirectUri, acrValues);
        } catch (AcrChangedException e) {
            log.debug("There is already existing session which has another acr then {0}, session: {1}", acrValues, session.getId());
            if (prompts.contains(Prompt.LOGIN)) {
                session = handleAcrChange(session, prompts);
            } else {
                log.error("Please provide prompt=login to force login with new ACR or otherwise perform logout and re-authenticate.");
                permissionDenied();
                return Constants.RESULT_FAILURE;
            }
        }

        if (session == null || session.getUserDn() == null || SessionIdState.AUTHENTICATED != session.getState()) {
            final ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
            Map<String, String> parameterMap = externalContext.getRequestParameterMap();
            Map<String, String> requestParameterMap = authenticationService.getAllowedParameters(parameterMap);

            String redirectTo = "/login.xhtml";

            boolean useExternalAuthenticator = externalAuthenticationService.isEnabled(AuthenticationScriptUsageType.INTERACTIVE);
            if (useExternalAuthenticator) {
                List<String> acrValuesList = acrValuesList();
                if (acrValuesList.isEmpty()) {
                    if (StringHelper.isNotEmpty(defaultAuthenticationMethod)) {
                        acrValuesList = Arrays.asList(defaultAuthenticationMethod);
                    } else {
                        CustomScriptConfiguration defaultExternalAuthenticator = externalAuthenticationService.getDefaultExternalAuthenticator(AuthenticationScriptUsageType.INTERACTIVE);
                        if (defaultExternalAuthenticator != null) {
                            acrValuesList = Arrays.asList(defaultExternalAuthenticator.getName());
                        }
                    }

                }

                CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService.determineCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, acrValuesList);

                if (customScriptConfiguration == null) {
                    log.error("Failed to get CustomScriptConfiguration. auth_step: {0}, acr_values: {1}", 1, this.acrValues);
                    permissionDenied();
                    return Constants.RESULT_FAILURE;
                }

                String acr = customScriptConfiguration.getName();

                requestParameterMap.put(JwtClaimName.AUTHENTICATION_METHOD_REFERENCES, acr);
                requestParameterMap.put("auth_step", Integer.toString(1));

                String tmpRedirectTo = externalAuthenticationService.executeExternalGetPageForStep(customScriptConfiguration, 1);
                if (StringHelper.isNotEmpty(tmpRedirectTo)) {
                    log.trace("Redirect to person authentication login page: {0}", tmpRedirectTo);
                    redirectTo = tmpRedirectTo;
                }
            }
            
            // Store Remote IP
            String remoteIp = networkService.getRemoteIp();
            requestParameterMap.put(Constants.REMOTE_IP, remoteIp);

            // Create unauthenticated session
            SessionState unauthenticatedSession = sessionStateService.generateSessionState(null, new Date(), SessionIdState.UNAUTHENTICATED, requestParameterMap, false);
            unauthenticatedSession.setSessionAttributes(requestParameterMap);
            boolean persisted = sessionStateService.persistSessionState(unauthenticatedSession, !prompts.contains(Prompt.NONE)); // always persist is prompt is not none
            if (persisted && log.isTraceEnabled()) {
                log.trace("Session '{0}' persisted to LDAP", unauthenticatedSession.getId());
            }

            this.sessionState = unauthenticatedSession.getId();
            sessionStateService.createSessionStateCookie(this.sessionState);

            FacesManager.instance().redirect(redirectTo, null, false);
            return Constants.RESULT_FAILURE;
        }

        if (clientId != null && !clientId.isEmpty()) {

            final Client client = clientService.getClient(clientId);

            if (client != null) {
            	
            	if(!client.getPersistClientAuthorizations() || !client.getTrustedClient()){
            		return  Constants.RESULT_SUCCESS; 
            	}
            	
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
                if (ConfigurationFactory.instance().getConfiguration().getFederationEnabled()) {
                    final List<FederationTrust> list = federationDataService.getTrustByClient(client, FederationTrustStatus.ACTIVE);

                    if (list == null || list.isEmpty()) {
                        log.trace("Deny authorization, client is not in any federation trust, client: {0}", client.getDn());
                        permissionDenied();
                    } else if (FederationDataService.skipAuthorization(list)) {
                        log.trace("Skip authorization (permissions granted), client is in federation trust where skip is allowed, client: {1}", client.getDn());
                        permissionGranted(session);
                    }
                }

                if (AuthorizeParamsValidator.validatePrompt(prompts)) {
                    ClientAuthorizations clientAuthorizations = clientAuthorizationsService.findClientAuthorizations(user.getAttribute("inum"), client.getClientId());
                    if (clientAuthorizations != null && clientAuthorizations.getScopes() != null &&
                            Arrays.asList(clientAuthorizations.getScopes()).containsAll(
                                    org.xdi.oxauth.model.util.StringUtils.spaceSeparatedToList(scope))) {
                        permissionGranted(session);
                    } else if (ConfigurationFactory.instance().getConfiguration().getTrustedClientEnabled()) { // if trusted client = true, then skip authorization page and grant access directly
                        if (client.getTrustedClient() && !prompts.contains(Prompt.CONSENT)) {
                            permissionGranted(session);
                        }
                    } else {
                        consentRequired();
                    }
                } else {
                    invalidRequest();
                }
            }
        }
		return Constants.RESULT_FAILURE;
    }

    private SessionState handleAcrChange(SessionState session, List<Prompt> prompts) {
        if (session != null && prompts.contains(Prompt.LOGIN)) { // change session state only if prompt=none
            if (session.getState() == SessionIdState.AUTHENTICATED) {
                session.getSessionAttributes().put("prompt", prompt);
                session.setState(SessionIdState.UNAUTHENTICATED);

                // Update Remote IP
                String remoteIp = networkService.getRemoteIp();
               	session.getSessionAttributes().put(Constants.REMOTE_IP, remoteIp);

                sessionStateService.updateSessionState(session);
                sessionStateService.reinitLogin(session, false);
            }
        }
        return session;
    }

    /**
     * By definition we expects space separated acr values as it is defined in spec. But we also try maybe some client
     * sent it to us as json array. So we try both.
     *
     * @return acr value list
     */
    private List<String> acrValuesList() {
        List<String> acrs;
        try {
            acrs = Util.jsonArrayStringAsList(this.acrValues);
        } catch (JSONException ex) {
            acrs = Util.splittedStringAsList(acrValues, " ");
        }

        return acrs;
    }

    private SessionState getSession() {
        if (StringUtils.isBlank(sessionState)) {
            sessionState = sessionStateService.getSessionStateFromCookie();
            if (StringUtils.isBlank(this.sessionState)) {
                return null;
            }
        }

        if (!identity.isLoggedIn()) {
            final Authenticator authenticator = (Authenticator) Component.getInstance(Authenticator.class, true);
            authenticator.authenticateBySessionState(sessionState);
        }

        SessionState ldapSessionState = sessionStateService.getSessionState(sessionState);
        if (ldapSessionState == null) {
            identity.logout();
        }

        return ldapSessionState;
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
     * Returns the mechanism to be used for returning parameters from the Authorization Endpoint.
     *
     * @return The response mode.
     */
    public String getResponseMode() {
        return responseMode;
    }

    /**
     * Sets the mechanism to be used for returning parameters from the Authorization Endpoint.
     *
     * @param responseMode The response mode.
     */
    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
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

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String p_sessionState) {
        sessionState = p_sessionState;
    }

    public void permissionGranted() {
        final SessionState session = getSession();
        permissionGranted(session);
    }

    public void permissionGranted(SessionState session) {
        try {
            final User user = userService.getUserByDn(session.getUserDn());
            if (user == null) {
                log.error("Permission denied. Failed to find session user: userDn = " + session.getUserDn() + ".");
                permissionDenied();
                return;
            }

            final Client client = clientService.getClient(clientId);
            final List<String> scopes = org.xdi.oxauth.model.util.StringUtils.spaceSeparatedToList(scope);
            clientAuthorizationsService.add(user.getAttribute("inum"), client.getClientId(), scopes);

            session.addPermission(clientId, true);
            sessionStateService.updateSessionState(session);

            // OXAUTH-297 - set session_state cookie
            SessionStateService.instance().createSessionStateCookie(sessionState);

            Map<String, String> sessionAttribute = authenticationService.getAllowedParameters(session.getSessionAttributes());

            final String parametersAsString = authenticationService.parametersAsString(sessionAttribute);
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
