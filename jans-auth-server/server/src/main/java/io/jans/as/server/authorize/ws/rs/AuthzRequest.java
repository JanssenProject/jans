package io.jans.as.server.authorize.ws.rs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.SecurityContext;

/**
 * @author Yuriy Zabrovarnyy
 */
public class AuthzRequest {

    private String scope;
    private String responseType;
    private String clientId;
    private String redirectUri;
    private String state;
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
    private String sessionId;
    private String originHeaders;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String customResponseHeaders;
    private String claims;
    private String authReqId;
    private String httpMethod;
    private HttpServletRequest httpRequest;
    private HttpServletResponse httpResponse;
    private SecurityContext securityContext;

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getOriginHeaders() {
        return originHeaders;
    }

    public void setOriginHeaders(String originHeaders) {
        this.originHeaders = originHeaders;
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

    public String getCustomResponseHeaders() {
        return customResponseHeaders;
    }

    public void setCustomResponseHeaders(String customResponseHeaders) {
        this.customResponseHeaders = customResponseHeaders;
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

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    public void setHttpRequest(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public HttpServletResponse getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(HttpServletResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    public void setSecurityContext(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    @Override
    public String toString() {
        return "AuthzRequest{" +
                "scope='" + scope + '\'' +
                ", responseType='" + responseType + '\'' +
                ", clientId='" + clientId + '\'' +
                ", redirectUri='" + redirectUri + '\'' +
                ", state='" + state + '\'' +
                ", responseMode='" + responseMode + '\'' +
                ", nonce='" + nonce + '\'' +
                ", display='" + display + '\'' +
                ", prompt='" + prompt + '\'' +
                ", maxAge=" + maxAge +
                ", uiLocales='" + uiLocales + '\'' +
                ", idTokenHint='" + idTokenHint + '\'' +
                ", loginHint='" + loginHint + '\'' +
                ", acrValues='" + acrValues + '\'' +
                ", amrValues='" + amrValues + '\'' +
                ", request='" + request + '\'' +
                ", requestUri='" + requestUri + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", originHeaders='" + originHeaders + '\'' +
                ", codeChallenge='" + codeChallenge + '\'' +
                ", codeChallengeMethod='" + codeChallengeMethod + '\'' +
                ", customResponseHeaders='" + customResponseHeaders + '\'' +
                ", claims='" + claims + '\'' +
                ", authReqId='" + authReqId + '\'' +
                ", httpRequest=" + httpRequest +
                ", httpResponse=" + httpResponse +
                ", securityContext=" + securityContext +
                '}';
    }
}
