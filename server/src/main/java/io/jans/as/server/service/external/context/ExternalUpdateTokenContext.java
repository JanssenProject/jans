/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */

package io.jans.as.server.service.external.context;

import javax.servlet.http.HttpServletRequest;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yuriy Movchan
 */
public class ExternalUpdateTokenContext extends ExternalScriptContext {

	private final Client client;
	private final AuthorizationGrant grant;

    private final AppConfiguration appConfiguration;
    private final AttributeService attributeService;

    private CustomScriptConfiguration script;
    @Nullable
    private ExecutionContext executionContext;

	public ExternalUpdateTokenContext(HttpServletRequest httpRequest, AuthorizationGrant grant,
			Client client, AppConfiguration appConfiguration, AttributeService attributeService) {
		super(httpRequest);
		this.client = client;
		this.grant = grant;
		this.appConfiguration = appConfiguration;
		this.attributeService = attributeService;
	}

	public static ExternalUpdateTokenContext of(ExecutionContext executionContext) {
        ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(executionContext.getHttpRequest(), executionContext.getGrant(), executionContext.getClient(), executionContext.getAppConfiguration(), executionContext.getAttributeService());
        context.setExecutionContext(executionContext);
        return context;
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
