/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.ldap.TokenEntity;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ExecutionContext {

    private HttpServletRequest httpRequest;
    private HttpServletResponse httpResponse;

    private Response.ResponseBuilder responseBuilder;

    private Client client;
    private AuthorizationGrant grant;

    private AppConfiguration appConfiguration;
    private AttributeService attributeService;

    private CustomScriptConfiguration script;
    private TokenEntity idTokenEntity;
    private TokenEntity accessTokenEntity;
    private TokenEntity refreshTokenEntity;

    private String dpop;
    private String certAsPem;
    private String deviceSecret;

    private String nonce;
    private String state;

    private boolean includeIdTokenClaims;

    private Function<JsonWebResponse, Void> preProcessing;
    private Function<JsonWebResponse, Void> postProcessor;

    private Set<String> scopes;
    private String claimsAsString;
    private List<SessionId> userSessions;
    private OAuth2AuditLog auditLog;

    @NotNull
    private final Map<String, String> attributes = new HashMap<>();

    public ExecutionContext() {
    }

    public ExecutionContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
    }

    public String getDeviceSecret() {
        return deviceSecret;
    }

    public void setDeviceSecret(String deviceSecret) {
        this.deviceSecret = deviceSecret;
    }

    @NotNull
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Nullable
    public String getAttribute(@NotNull String key) {
        return attributes.get(key);
    }

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    public HttpServletResponse getHttpResponse() {
        return httpResponse;
    }

    public OAuth2AuditLog getAuditLog() {
        return auditLog;
    }

    public void setAuditLog(OAuth2AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    public Client getClient() {
        return client;
    }

    public ExecutionContext setClient(Client client) {
        this.client = client;
        return this;
    }

    public void setHttpRequest(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public void setHttpResponse(HttpServletResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    public AuthorizationGrant getGrant() {
        return grant;
    }

    public void setGrant(AuthorizationGrant grant) {
        this.grant = grant;
    }

    public AppConfiguration getAppConfiguration() {
        return appConfiguration;
    }

    public void setAppConfiguration(AppConfiguration appConfiguration) {
        this.appConfiguration = appConfiguration;
    }

    public AttributeService getAttributeService() {
        return attributeService;
    }

    public void setAttributeService(AttributeService attributeService) {
        this.attributeService = attributeService;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

    public TokenEntity getIdTokenEntity() {
        return idTokenEntity;
    }

    public void setIdTokenEntity(TokenEntity idTokenEntity) {
        this.idTokenEntity = idTokenEntity;
    }

    public TokenEntity getAccessTokenEntity() {
        return accessTokenEntity;
    }

    public void setAccessTokenEntity(TokenEntity accessTokenEntity) {
        this.accessTokenEntity = accessTokenEntity;
    }

    public TokenEntity getRefreshTokenEntity() {
        return refreshTokenEntity;
    }

    public void setRefreshTokenEntity(TokenEntity refreshTokenEntity) {
        this.refreshTokenEntity = refreshTokenEntity;
    }

    public String getDpop() {
        return dpop;
    }

    public void setDpop(String dpop) {
        this.dpop = dpop;
    }

    public String getCertAsPem() {
        return certAsPem;
    }

    public void setCertAsPem(String certAsPem) {
        this.certAsPem = certAsPem;
    }

    public boolean isIncludeIdTokenClaims() {
        return includeIdTokenClaims;
    }

    public void setIncludeIdTokenClaims(boolean includeIdTokenClaims) {
        this.includeIdTokenClaims = includeIdTokenClaims;
    }

    public Function<JsonWebResponse, Void> getPreProcessing() {
        return preProcessing;
    }

    public void setPreProcessing(Function<JsonWebResponse, Void> preProcessing) {
        this.preProcessing = preProcessing;
    }

    public Function<JsonWebResponse, Void> getPostProcessor() {
        return postProcessor;
    }

    public void setPostProcessor(Function<JsonWebResponse, Void> postProcessor) {
        this.postProcessor = postProcessor;
    }

    public Set<String> getScopes() {
        if (scopes == null) scopes = new HashSet<>();
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public String getClaimsAsString() {
        return claimsAsString;
    }

    public void setClaimsAsString(String claimsAsString) {
        this.claimsAsString = claimsAsString;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<SessionId> getUserSessions() {
        return userSessions;
    }

    public void setUserSessions(List<SessionId> userSessions) {
        this.userSessions = userSessions;
    }

    public Response.ResponseBuilder getResponseBuilder() {
        return responseBuilder;
    }

    public void setResponseBuilder(Response.ResponseBuilder responseBuilder) {
        this.responseBuilder = responseBuilder;
    }
}
