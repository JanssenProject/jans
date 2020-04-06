package org.gluu.oxauth.auth;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.common.SessionId;
import org.gluu.oxauth.model.common.User;
import org.gluu.oxauth.service.SessionIdService;
import org.slf4j.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 */
@RequestScoped
@Named
public class SelectAccountAction {

    @Inject
    private Logger log;

    @Inject
    private SessionIdService sessionIdService;

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

    private Set<SessionId> currentSessions = Sets.newHashSet();

    public void prepare() {
        currentSessions = Sets.newHashSet();
        for (SessionId sessionId : sessionIdService.getCurrentSessions()) {
            final User user = sessionIdService.getUser(sessionId);
            if (user == null) {
                log.error("Failed to get user for session. Skipping it from current_sessions, id: " + sessionId.getId());
                continue;
            }
            currentSessions.add(sessionId);
        }
    }

    public Set<SessionId> getCurrentSessions() {
        return currentSessions;
    }

    public void select(SessionId selectedSession) {
        log.debug("Selected account: " + selectedSession.getId());
    }

    public String getName(Object sessionId) {
        final User user = ((SessionId) sessionId).getUser();
        final String displayName = user.getAttribute("displayName");
        if (StringUtils.isNotBlank(displayName)) {
            return displayName;
        }
        return user.getUserId();
    }

    public void login() {
        // todo
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
}
