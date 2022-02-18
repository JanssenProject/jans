package io.jans.as.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParAttributes implements Serializable {

    @JsonProperty
    Integer maxAge;
    @JsonProperty
    Integer nbf;
    @JsonProperty
    private String scope;
    @JsonProperty
    private String responseType;
    @JsonProperty
    private String clientId;
    @JsonProperty
    private String redirectUri;
    @JsonProperty
    private String state;
    @JsonProperty
    private String responseMode;
    @JsonProperty
    private String nonce;
    @JsonProperty
    private String display;
    @JsonProperty
    private String prompt;
    @JsonProperty
    private String uiLocales;
    @JsonProperty
    private String idTokenHint;
    @JsonProperty
    private String loginHint;
    @JsonProperty
    private String acrValuesStr;
    @JsonProperty
    private String amrValuesStr;
    @JsonProperty
    private String request;
    @JsonProperty
    private String requestUri;
    @JsonProperty
    private String sessionId;
    @JsonProperty
    private String originHeaders;
    @JsonProperty
    private String codeChallenge;
    @JsonProperty
    private String codeChallengeMethod;
    @JsonProperty
    private String customResponseHeaders;
    @JsonProperty
    private String claims;
    @JsonProperty
    private Map<String, String> customParameters;

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

    public Integer getNbf() {
        return nbf;
    }

    public void setNbf(Integer nbf) {
        this.nbf = nbf;
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

    public String getAcrValuesStr() {
        return acrValuesStr;
    }

    public void setAcrValuesStr(String acrValuesStr) {
        this.acrValuesStr = acrValuesStr;
    }

    public String getAmrValuesStr() {
        return amrValuesStr;
    }

    public void setAmrValuesStr(String amrValuesStr) {
        this.amrValuesStr = amrValuesStr;
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

    public Map<String, String> getCustomParameters() {
        if (customParameters == null) customParameters = new HashMap<>();
        return customParameters;
    }

    public void setCustomParameters(Map<String, String> customParameters) {
        this.customParameters = customParameters;
    }

    @Override
    public String toString() {
        return "ParAttributes{" +
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
                ", nbf=" + nbf +
                ", uiLocales='" + uiLocales + '\'' +
                ", idTokenHint='" + idTokenHint + '\'' +
                ", loginHint='" + loginHint + '\'' +
                ", acrValuesStr='" + acrValuesStr + '\'' +
                ", amrValuesStr='" + amrValuesStr + '\'' +
                ", request='" + request + '\'' +
                ", requestUri='" + requestUri + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", originHeaders='" + originHeaders + '\'' +
                ", codeChallenge='" + codeChallenge + '\'' +
                ", codeChallengeMethod='" + codeChallengeMethod + '\'' +
                ", customResponseHeaders='" + customResponseHeaders + '\'' +
                ", claims='" + claims + '\'' +
                ", customParameters='" + customParameters + '\'' +
                '}';
    }
}
