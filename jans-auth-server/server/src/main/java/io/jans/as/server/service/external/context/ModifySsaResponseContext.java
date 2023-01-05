/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.Nullable;

public class ModifySsaResponseContext extends ExternalScriptContext {

    private final Client client;
    private final AuthorizationGrant grant;

    private final AppConfiguration appConfiguration;
    private final AttributeService attributeService;

    private CustomScriptConfiguration script;
    @Nullable
    private ExecutionContext executionContext;

    public ModifySsaResponseContext(HttpServletRequest httpRequest, AuthorizationGrant grant,
                                    Client client, AppConfiguration appConfiguration, AttributeService attributeService) {
        super(httpRequest);
        this.client = client;
        this.grant = grant;
        this.appConfiguration = appConfiguration;
        this.attributeService = attributeService;
    }

    public static ModifySsaResponseContext of(ExecutionContext executionContext) {
        ModifySsaResponseContext context = new ModifySsaResponseContext(executionContext.getHttpRequest(), executionContext.getGrant(), executionContext.getClient(), executionContext.getAppConfiguration(), executionContext.getAttributeService());
        context.setExecutionContext(executionContext);
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

    @Nullable
    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(@Nullable ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }
}
