/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.ldap.TokenEntity;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ExecutionContext {

    private HttpServletRequest httpRequest;
    private HttpServletResponse httpResponse;

    private Client client;
    private AuthorizationGrant grant;

    private AppConfiguration appConfiguration;
    private AttributeService attributeService;

    private CustomScriptConfiguration script;
    private TokenEntity idTokenEntity;
    private TokenEntity accessTokenEntity;
    private TokenEntity refreshTokenEntity;

    private String dpop;

    @NotNull
    private final Map<String, String> attributes = new HashMap<>();

    public ExecutionContext() {
    }

    public ExecutionContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
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

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
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
}
