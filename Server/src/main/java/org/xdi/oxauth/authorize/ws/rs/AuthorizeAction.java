/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.authorize.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.gluu.jsf2.message.FacesMessages;
import org.gluu.jsf2.service.FacesService;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.slf4j.Logger;
import org.xdi.model.AuthenticationScriptUsageType;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.i18n.LanguageBean;
import org.xdi.oxauth.model.auth.AuthenticationMode;
import org.xdi.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.xdi.oxauth.model.authorize.AuthorizeParamsValidator;
import org.xdi.oxauth.model.authorize.AuthorizeRequestParam;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.SessionId;
import org.xdi.oxauth.model.common.SessionIdState;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.config.Constants;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.exception.AcrChangedException;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.ldap.ClientAuthorizations;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.util.LocaleUtil;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.*;
import org.xdi.oxauth.service.external.ExternalAuthenticationService;
import org.xdi.oxauth.service.external.ExternalConsentGatheringService;
import org.xdi.service.net.NetworkService;
import org.xdi.util.StringHelper;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version January 17, 2018
 */
@RequestScoped
@Named
public class AuthorizeAction {

    @Inject
    private Logger log;

    @Inject
    private ClientService clientService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private UserService userService;

    @Inject
    private RedirectionUriService redirectionUriService;

    @Inject
    private ClientAuthorizationsService clientAuthorizationsService;

    @Inject
    private ExternalAuthenticationService externalAuthenticationService;

    @Inject
    private ExternalConsentGatheringService externalConsentGatheringService;

    @Inject
    private AuthenticationMode defaultAuthenticationMode;

    @Inject
    private LanguageBean languageBean;

    @Inject
    private NetworkService networkService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private FacesService facesService;

    @Inject
    private FacesMessages facesMessages;

    @Inject
    private FacesContext facesContext;

    @Inject
    private ExternalContext externalContext;

    @Inject
    private ConsentGathererService consentGatherer;

    @Inject
    private AuthorizeService authorizeService;

    @Inject
    private RequestParameterService requestParameterService;

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
    private String codeChallenge;
    private String codeChallengeMethod;
    private String claims;

    // custom oxAuth parameters
    private String sessionId;

    public void checkUiLocales() {
        List<String> uiLocalesList = null;
        if (StringUtils.isNotBlank(uiLocales)) {
            uiLocalesList = Util.splittedStringAsList(uiLocales, " ");

            List<Locale> supportedLocales = new ArrayList<Locale>();
            for (Iterator<Locale> it = facesContext.getApplication().getSupportedLocales(); it.hasNext(); ) {
                supportedLocales.add(it.next());
            }
            Locale matchingLocale = LocaleUtil.localeMatch(uiLocalesList, supportedLocales);

            if (matchingLocale != null)
                languageBean.setLocaleCode(matchingLocale.getLanguage());
        } else {
            Locale defaultLocale = facesContext.getApplication().getDefaultLocale();
            if (defaultLocale != null) {
                languageBean.setLocaleCode(defaultLocale.getLanguage());
            }
        }
    }

    public void checkPermissionGranted() {
        if ((clientId == null) || clientId.isEmpty()) {
            log.error("Permission denied. client_id should be not empty.");
            permissionDenied();
            return;
        }

        Client client = null;
        try {
            client = clientService.getClient(clientId);
        } catch (EntryPersistenceException ex) {
            log.error("Permission denied. Failed to find client by inum '{}' in LDAP.", clientId, ex);
            permissionDenied();
            return;
        }

        if (client == null) {
            log.error("Permission denied. Failed to find client_id '{}' in LDAP.", clientId);
            permissionDenied();
            return;
        }

        SessionId session = getSession();
        List<Prompt> prompts = Prompt.fromString(prompt, " ");

        try {
            session = sessionIdService.assertAuthenticatedSessionCorrespondsToNewRequest(session, acrValues);
        } catch (AcrChangedException e) {
            log.debug("There is already existing session which has another acr then {}, session: {}", acrValues, session.getId());
            if (prompts.contains(Prompt.LOGIN)) {
                session = handleAcrChange(session, prompts);
            } else {
                log.error("Please provide prompt=login to force login with new ACR or otherwise perform logout and re-authenticate.");
                permissionDenied();
                return;
            }
        }

        if (session == null || StringUtils.isBlank(session.getUserDn()) || SessionIdState.AUTHENTICATED != session.getState()) {
            Map<String, String> parameterMap = externalContext.getRequestParameterMap();
            Map<String, String> requestParameterMap = requestParameterService.getAllowedParameters(parameterMap);

            String redirectTo = "/login.xhtml";

            boolean useExternalAuthenticator = externalAuthenticationService.isEnabled(AuthenticationScriptUsageType.INTERACTIVE);
            if (useExternalAuthenticator) {
                List<String> acrValuesList = sessionIdService.acrValuesList(this.acrValues);
                if (acrValuesList.isEmpty()) {
                    if (StringHelper.isNotEmpty(defaultAuthenticationMode.getName())) {
                        acrValuesList = Arrays.asList(defaultAuthenticationMode.getName());
                    } else {
                        CustomScriptConfiguration defaultExternalAuthenticator = externalAuthenticationService.getDefaultExternalAuthenticator(AuthenticationScriptUsageType.INTERACTIVE);
                        if (defaultExternalAuthenticator != null) {
                            acrValuesList = Arrays.asList(defaultExternalAuthenticator.getName());
                        }
                    }

                }

                CustomScriptConfiguration customScriptConfiguration = externalAuthenticationService.determineCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, acrValuesList);

                if (customScriptConfiguration == null) {
                    log.error("Failed to get CustomScriptConfiguration. auth_step: {}, acr_values: {}", 1, this.acrValues);
                    permissionDenied();
                    return;
                }

                String acr = customScriptConfiguration.getName();

                requestParameterMap.put(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, acr);
                requestParameterMap.put("auth_step", Integer.toString(1));

                String tmpRedirectTo = externalAuthenticationService.executeExternalGetPageForStep(customScriptConfiguration, 1);
                if (StringHelper.isNotEmpty(tmpRedirectTo)) {
                    log.trace("Redirect to person authentication login page: {}", tmpRedirectTo);
                    redirectTo = tmpRedirectTo;
                }
            }

            // Store Remote IP
            String remoteIp = networkService.getRemoteIp();
            requestParameterMap.put(Constants.REMOTE_IP, remoteIp);

            // Create unauthenticated session
            SessionId unauthenticatedSession = sessionIdService.generateUnauthenticatedSessionId(null, new Date(), SessionIdState.UNAUTHENTICATED, requestParameterMap, false);
            unauthenticatedSession.setSessionAttributes(requestParameterMap);
            unauthenticatedSession.addPermission(clientId, false);
            boolean persisted = sessionIdService.persistSessionId(unauthenticatedSession, !prompts.contains(Prompt.NONE)); // always persist is prompt is not none
            if (persisted && log.isTraceEnabled()) {
                log.trace("Session '{}' persisted to LDAP", unauthenticatedSession.getId());
            }

            this.sessionId = unauthenticatedSession.getId();
            sessionIdService.createSessionIdCookie(this.sessionId, unauthenticatedSession.getSessionState(), false);

            Map<String, Object> loginParameters = new HashMap<String, Object>();
            if (requestParameterMap.containsKey(AuthorizeRequestParam.LOGIN_HINT)) {
                loginParameters.put(AuthorizeRequestParam.LOGIN_HINT,
                        requestParameterMap.get(AuthorizeRequestParam.LOGIN_HINT));
            }

            facesService.redirectWithExternal(redirectTo, loginParameters);

            return;
        }

        if (StringUtils.isBlank(redirectionUriService.validateRedirectionUri(clientId, redirectUri))) {
            permissionDenied();
        }

        final User user = userService.getUserByDn(session.getUserDn());
        log.trace("checkPermissionGranted, user = " + user);

        if (AuthorizeParamsValidator.noNonePrompt(prompts)) {
            if (appConfiguration.getTrustedClientEnabled()) { // if trusted client = true, then skip authorization page and grant access directly
                if (client.getTrustedClient() && !prompts.contains(Prompt.CONSENT)) {
                    permissionGranted(session);
                    return;
                }
            }

            ClientAuthorizations clientAuthorizations = clientAuthorizationsService.findClientAuthorizations(
                    user.getAttribute("inum"),
                    client.getClientId(),
                    client.getPersistClientAuthorizations());
            if (clientAuthorizations != null && clientAuthorizations.getScopes() != null &&
                    Arrays.asList(clientAuthorizations.getScopes()).containsAll(
                            org.xdi.oxauth.model.util.StringUtils.spaceSeparatedToList(scope))) {
                permissionGranted(session);
                return;
            }

        } else {
            invalidRequest();
        }

        if (externalConsentGatheringService.isEnabled()) {
            if (consentGatherer.isConsentGathered()) {
                log.trace("Consent-gathered flow passed successfully");
                permissionGranted(session);
                return;
            }

            log.trace("Starting external consent-gathering flow");

            boolean result = consentGatherer.configure(session.getUserDn(), clientId, state);
            if (!result) {
                log.error("Failed to initialize external consent-gathering flow.");
                permissionDenied();
                return;
            }
        }

        return;
    }

    private SessionId handleAcrChange(SessionId session, List<Prompt> prompts) {
        if (session != null && prompts.contains(Prompt.LOGIN)) { // change session state only if prompt=none
            if (session.getState() == SessionIdState.AUTHENTICATED) {
                session.getSessionAttributes().put("prompt", prompt);
                session.setState(SessionIdState.UNAUTHENTICATED);

                // Update Remote IP
                String remoteIp = networkService.getRemoteIp();
                session.getSessionAttributes().put(Constants.REMOTE_IP, remoteIp);

                sessionIdService.updateSessionId(session);
                sessionIdService.reinitLogin(session, false);
            }
        }
        return session;
    }

    private SessionId getSession() {
        return authorizeService.getSession(sessionId);
    }

    public List<org.xdi.oxauth.model.common.Scope> getScopes() {
        return authorizeService.getScopes(scope);
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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String p_sessionId) {
        sessionId = p_sessionId;
    }

    public void permissionGranted() {
        final SessionId session = getSession();
        permissionGranted(session);
    }

    public void permissionGranted(SessionId session) {
        final HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getRequest();
        authorizeService.permissionGranted(httpRequest, session);
    }

    public void permissionDenied() {
        final SessionId session = getSession();
        authorizeService.permissionDenied(session);
    }

    private void authenticationFailedSessionInvalid() {
        facesMessages.add(FacesMessage.SEVERITY_ERROR, "login.errorSessionInvalidMessage");
        facesService.redirect("/error.xhtml");
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

        facesService.redirectToExternalURL(sb.toString());
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

        facesService.redirectToExternalURL(sb.toString());
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public void setCodeChallengeMethod(String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }

    public String getClaims() {
        return claims;
    }

    public void setClaims(String claims) {
        this.claims = claims;
    }

    public String encodeParameters(String url, Map<String, Object> parameters) {
        if (parameters.isEmpty()) return url;

        StringBuilder builder = new StringBuilder(url);
        for (Map.Entry<String, Object> param : parameters.entrySet()) {
            String parameterName = param.getKey();
            if (!containsParameter(url, parameterName)) {
                Object parameterValue = param.getValue();
                if (parameterValue instanceof Iterable) {
                    for (Object value : (Iterable) parameterValue) {
                        builder.append('&')
                                .append(parameterName)
                                .append('=');
                        if (value != null) {
                            builder.append(encode(value));
                        }
                    }
                } else {
                    builder.append('&')
                            .append(parameterName)
                            .append('=');
                    if (parameterValue != null) {
                        builder.append(encode(parameterValue));
                    }
                }
            }
        }
        if (url.indexOf('?') < 0) {
            builder.setCharAt(url.length(), '?');
        }
        return builder.toString();
    }

    private boolean containsParameter(String url, String parameterName) {
        return url.indexOf('?' + parameterName + '=') > 0 ||
                url.indexOf('&' + parameterName + '=') > 0;
    }

    private String encode(Object value) {
        try {
            return URLEncoder.encode(String.valueOf(value), "UTF-8");
        } catch (UnsupportedEncodingException iee) {
            throw new RuntimeException(iee);
        }
    }

}
