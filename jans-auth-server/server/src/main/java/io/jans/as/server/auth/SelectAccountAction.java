/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.auth;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.CookieService;
import io.jans.as.server.service.RequestParameterService;
import io.jans.as.server.service.SessionIdService;
import io.jans.jsf2.service.FacesService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 */
@RequestScoped
@Named
public class SelectAccountAction {

    private static final String FORM_ID = "selectForm";
    private static final String LOGIN_BUTTON_REF = FORM_ID + ":loginButton";

    @Inject
    private Logger log;

    @Inject
    private Identity identity;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private FacesService facesService;

    @Inject
    private CookieService cookieService;

    @Inject
    private ExternalContext externalContext;

    @Inject
    private RequestParameterService requestParameterService;

    @Inject
    private Authenticator authenticator;


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
    private String authReqId;
    private String bindingMessage;
    private String sessionId;
    private String allowedScope;

    private List<SessionId> currentSessions = Lists.newArrayList();
    private String selectedSessionId;

    @PostConstruct
    public void prepare() {
        currentSessions = Lists.newArrayList();
        Set<String> uids = Sets.newHashSet();
        for (SessionId session : sessionIdService.getCurrentSessions()) {
            final User user = sessionIdService.getUser(session);
            if (user == null) {
                log.error("Failed to get user for session. Skipping it from current_sessions, id: {}", session.getId());
                continue;
            }
            final String uid = StringUtils.isNotBlank(user.getUserId()) ? user.getUserId() : user.getDn();
            if (!currentSessions.contains(session) && !uids.contains(uid)) {
                log.trace("User: {}, sessionId: {}", uid, session.getId());
                currentSessions.add(session);
                uids.add(uid);
            }
        }
        log.trace("Found {} sessions", currentSessions.size());
    }

    public List<SessionId> getCurrentSessions() {
        return currentSessions;
    }

    public void select() {
        try {
            log.debug("Selected account: {}", selectedSessionId);
            clearSessionIdCookie();
            Optional<SessionId> selectedSession = currentSessions.stream().filter(s -> s.getId().equals(selectedSessionId)).findAny();
            if (!selectedSession.isPresent()) {
                log.debug("Unable to find session.");
                return;
            }
            cookieService.createSessionIdCookie(selectedSession.get(), false);
            identity.setSessionId(selectedSession.get());
            authenticator.authenticateBySessionId(selectedSessionId);
            String uri = buildAuthorizationUrl();
            log.trace("RedirectTo: {}", uri);
            facesService.redirectToExternalURL(uri);
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getName(SessionId sessionId) {
        final User user = sessionId.getUser();
        final String displayName = user.getAttribute("displayName");
        if (StringUtils.isNotBlank(displayName)) {
            return displayName;
        }
        if (StringUtils.isNotBlank(displayName)) {
            return user.getUserId();
        }
        return user.getDn();
    }

    public void login() {
        try {
            clearSessionIdCookie();
            String uri = buildAuthorizationUrl();
            log.trace("RedirectTo: {}", uri);
            facesService.redirectToExternalURL(uri);
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void clearSessionIdCookie() {
        final Object response = externalContext.getResponse();
        if (!(response instanceof HttpServletResponse)) {
            log.error("Unknown http response.");
            return;
        }

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        cookieService.removeSessionIdCookie(httpResponse);
        cookieService.removeOPBrowserStateCookie(httpResponse);

        if (identity != null) {
            identity.logout();
        }
        log.trace("Removed session_id and opbs cookies.");
    }

    private String buildAuthorizationUrl() throws UnsupportedEncodingException {
        final HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getRequest();
        return httpRequest.getContextPath() + "/restv1/authorize?" + requestParameterService.parametersAsString(getFilteredParameters());
    }

    private Map<String, String> getFilteredParameters() {
        final Map<String, String> parameterMap = externalContext.getRequestParameterMap();
        final Map<String, String> filtered = Maps.newHashMap();
        final String formIdWithColon = FORM_ID + ":";

        for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
            final String key = entry.getKey();
            if (key.equals("jakarta.faces.ViewState") || key.equals(FORM_ID) || key.contains(LOGIN_BUTTON_REF)) {
                continue;
            }
            if (key.startsWith(formIdWithColon)) {
                filtered.put(StringUtils.removeStart(key, formIdWithColon), entry.getValue());
                continue;
            }
            filtered.put(StringUtils.removeStart(key, formIdWithColon), entry.getValue());
        }
        return filtered;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getPrompt() {
        return prompt;
    }

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

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
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

    public String getAuthReqId() {
        return authReqId;
    }

    public void setAuthReqId(String authReqId) {
        this.authReqId = authReqId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAllowedScope() {
        return allowedScope;
    }

    public void setAllowedScope(String allowedScope) {
        this.allowedScope = allowedScope;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public void setBindingMessage(String bindingMessage) {
        this.bindingMessage = bindingMessage;
    }

    public String getSelectedSessionId() {
        return selectedSessionId;
    }

    public void setSelectedSessionId(String selectedSessionId) {
        this.selectedSessionId = selectedSessionId;
    }
}
