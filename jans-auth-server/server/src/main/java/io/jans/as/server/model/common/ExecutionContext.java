/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.model.authzdetails.AuthzDetail;
import io.jans.as.model.authzdetails.AuthzDetails;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.authorize.ws.rs.AuthzRequest;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.token.TokenEntity;
import io.jans.util.IdUtil;
import jakarta.faces.context.ExternalContext;
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
    private User user;

    private SessionId sessionId;
    private List<SessionId> currentSessions;
    private SessionId authorizationChallengeSessionId;

    private AuthzRequest authzRequest;
    private AuthzDetails authzDetails;
    private AuthzDetail authzDetail;

    private AppConfiguration appConfiguration;
    private AttributeService attributeService;

    private CustomScriptConfiguration script;
    private boolean skipModifyAccessTokenScript;
    private TokenEntity idTokenEntity;
    private TokenEntity accessTokenEntity;
    private TokenEntity refreshTokenEntity;

    private String dpop;
    private String certAsPem;
    private String deviceSecret;
    private String requestId;

    private String nonce;
    private String state;
    private String tokenReferenceId = IdUtil.randomShortUUID();
    private Integer statusListIndex;

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

    public static ExecutionContext of(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return new ExecutionContext(httpRequest, httpResponse);
    }

    public static ExecutionContext of(AuthzRequest authzRequest) {
        ExecutionContext executionContext = new ExecutionContext();
        if (authzRequest == null) {
            return executionContext;
        }

        executionContext.setHttpRequest(authzRequest.getHttpRequest());
        executionContext.setHttpResponse(authzRequest.getHttpResponse());
        executionContext.setClient(authzRequest.getClient());
        executionContext.setAuthzRequest(authzRequest);
        executionContext.setAuthzDetails(authzRequest.getAuthzDetails());
        return executionContext;
    }

    public static ExecutionContext of(ExternalContext externalContext) {
        ExecutionContext executionContext = new ExecutionContext();
        if (externalContext != null) {
            if (externalContext.getRequest() instanceof HttpServletRequest) {
                executionContext.setHttpRequest((HttpServletRequest) externalContext.getRequest());
            }
            if (externalContext.getResponse() instanceof HttpServletResponse) {
                executionContext.setHttpResponse((HttpServletResponse) externalContext.getResponse());
            }
        }
        return executionContext;
    }

    public static ExecutionContext of(ExecutionContext context) {
        ExecutionContext executionContext = new ExecutionContext();
        if (context == null) {
            return executionContext;
        }

        executionContext.httpRequest = context.httpRequest;
        executionContext.httpResponse = context.httpResponse;
        executionContext.responseBuilder = context.responseBuilder;
        executionContext.client = context.client;
        executionContext.grant = context.grant;
        executionContext.user = context.user;
        executionContext.sessionId = context.sessionId;
        executionContext.currentSessions = context.currentSessions;
        executionContext.authorizationChallengeSessionId = context.authorizationChallengeSessionId;
        executionContext.authzRequest = context.authzRequest;
        executionContext.authzDetails = context.authzDetails;
        executionContext.authzDetail = context.authzDetail;
        executionContext.appConfiguration = context.appConfiguration;
        executionContext.attributeService = context.attributeService;
        executionContext.script = context.script;
        executionContext.skipModifyAccessTokenScript = context.skipModifyAccessTokenScript;
        executionContext.idTokenEntity = context.idTokenEntity;
        executionContext.accessTokenEntity = context.accessTokenEntity;
        executionContext.refreshTokenEntity = context.refreshTokenEntity;
        executionContext.dpop = context.dpop;
        executionContext.certAsPem = context.certAsPem;
        executionContext.deviceSecret = context.deviceSecret;
        executionContext.nonce = context.nonce;
        executionContext.state = context.state;
        executionContext.includeIdTokenClaims = context.includeIdTokenClaims;
        executionContext.preProcessing = context.preProcessing;
        executionContext.postProcessor = context.postProcessor;
        executionContext.scopes = context.scopes;
        executionContext.claimsAsString = context.claimsAsString;
        executionContext.userSessions = context.userSessions;
        executionContext.auditLog = context.auditLog;
        executionContext.requestId = context.requestId;

        executionContext.attributes.clear();
        executionContext.attributes.putAll(context.attributes);

        return executionContext;
    }

    public Integer getStatusListIndex() {
        return statusListIndex;
    }

    public void setStatusListIndex(Integer statusListIndex) {
        this.statusListIndex = statusListIndex;
    }

    public String generateRandomTokenReferenceId() {
        tokenReferenceId = IdUtil.randomShortUUID();
        return tokenReferenceId;
    }

    public String getTokenReferenceId() {
        return tokenReferenceId;
    }

    public void setTokenReferenceId(String tokenReferenceId) {
        this.tokenReferenceId = tokenReferenceId;
    }

    public ExecutionContext copy() {
        return of(this);
    }

    public AuthzDetails getAuthzDetails() {
        return authzDetails;
    }

    public void setAuthzDetails(AuthzDetails authzDetails) {
        this.authzDetails = authzDetails;
    }

    public AuthzDetail getAuthzDetail() {
        return authzDetail;
    }

    public void setAuthzDetail(AuthzDetail authzDetail) {
        this.authzDetail = authzDetail;
    }

    public AuthzRequest getAuthzRequest() {
        return authzRequest;
    }

    public void setAuthzRequest(AuthzRequest authzRequest) {
        this.authzRequest = authzRequest;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SessionId getAuthorizationChallengeSessionId() {
        return authorizationChallengeSessionId;
    }

    public void setAuthorizationChallengeSessionId(SessionId authorizationChallengeSessionId) {
        this.authorizationChallengeSessionId = authorizationChallengeSessionId;
    }

    public List<SessionId> getCurrentSessions() {
        return currentSessions;
    }

    public void setCurrentSessions(List<SessionId> currentSessions) {
        this.currentSessions = currentSessions;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public void setSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
    }

    public String getDeviceSecret() {
        return deviceSecret;
    }

    public void setDeviceSecret(String deviceSecret) {
        this.deviceSecret = deviceSecret;
    }

    public boolean isSkipModifyAccessTokenScript() {
        return skipModifyAccessTokenScript;
    }

    public void setSkipModifyAccessTokenScript(boolean skipModifyAccessTokenScript) {
        this.skipModifyAccessTokenScript = skipModifyAccessTokenScript;
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

    public String getRequestId() {
        return requestId;
    }

    public ExecutionContext setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
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

    public void initFromGrantIfNeeded(AuthorizationGrant authorizationGrant) {
        if (client == null) {
            client = authorizationGrant.getClient();
        }
        if (grant == null) {
            grant = authorizationGrant;
        }
    }
}
