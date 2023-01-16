/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */

package io.jans.as.server.service.external.context;

import com.google.common.collect.Lists;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.server.model.common.AccessToken;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * @author Yuriy Movchan
 */
public class ExternalUpdateTokenContext extends ExternalScriptContext {

    private static final Logger log = LoggerFactory.getLogger(ExternalUpdateTokenContext.class);

    private final Client client;
    private final AuthorizationGrant grant;

    private final AppConfiguration appConfiguration;
    private final AttributeService attributeService;

    private CustomScriptConfiguration script;
    @Nullable
    private ExecutionContext executionContext;
    private JwtSigner jwtSigner;

    public ExternalUpdateTokenContext(HttpServletRequest httpRequest, AuthorizationGrant grant,
                                      Client client, AppConfiguration appConfiguration, AttributeService attributeService) {
        super(httpRequest);
        this.client = client;
        this.grant = grant;
        this.appConfiguration = appConfiguration;
        this.attributeService = attributeService;
    }

    public static ExternalUpdateTokenContext of(ExecutionContext executionContext) {
        return of(executionContext, null);
    }

    public static ExternalUpdateTokenContext of(ExecutionContext executionContext, JwtSigner jwtSigner) {
        ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(executionContext.getHttpRequest(), executionContext.getGrant(), executionContext.getClient(), executionContext.getAppConfiguration(), executionContext.getAttributeService());
        context.setExecutionContext(executionContext);
        context.setJwtSigner(jwtSigner);
        return context;
    }

    public ExecutionContext toExecutionContext() {
        if (executionContext == null) {
            executionContext = createExecutionContext();
        }
        return executionContext;
    }

    private ExecutionContext createExecutionContext() {
        ExecutionContext result = new ExecutionContext(httpRequest, httpResponse);
        result.setGrant(grant);
        result.setClient(client);
        result.setAppConfiguration(appConfiguration);
        result.setAttributeService(attributeService);
        result.setScript(script);
        return result;
    }

    public JwtClaims getClaims() {
        Jwt jwt = getJwt();
        return jwt != null ? jwt.getClaims() : null;
    }

    public Jwt getJwt() {
        return jwtSigner != null ? jwtSigner.getJwt() : null;
    }

    public JwtSigner getJwtSigner() {
        return jwtSigner;
    }

    public void setJwtSigner(JwtSigner jwtSigner) {
        this.jwtSigner = jwtSigner;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

    public Client getClient() {
        return client;
    }

    public AuthorizationGrant getGrant() {
        return grant;
    }

    public AppConfiguration getAppConfiguration() {
        return appConfiguration;
    }

    public AttributeService getAttributeService() {
        return attributeService;
    }

    public boolean isRefreshingGrant() {
        return grant != null && grant.getGrantType() == GrantType.REFRESH_TOKEN;
    }

    @Nullable
    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(@Nullable ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    // Usually expected to be called in : "def modifyAccessToken(self, accessToken, context):"
    public void overwriteAccessTokenScopes(AccessToken accessToken, Set<String> newScopes) {
        if (grant == null) {
            return;
        }

        grant.setScopes(newScopes);

        final Jwt jwt = getJwt();
        if (jwt != null) {
            jwt.getClaims().setClaim("scope", Lists.newArrayList(newScopes));
        }
    }

    private boolean isValidJwt(String jwt) {
        return Jwt.parseSilently(jwt) != null;
    }
}
