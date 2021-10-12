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
import io.jans.as.server.model.common.AbstractAuthorizationGrant;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;

/**
 * @author Yuriy Movchan
 */
public class ExternalUpdateTokenContext extends ExternalScriptContext {

	private final Client client;
	private final AbstractAuthorizationGrant grant;

	private CustomScriptConfiguration script;

	private final AppConfiguration appConfiguration;
	private final AttributeService attributeService;

	public ExternalUpdateTokenContext(HttpServletRequest httpRequest, AbstractAuthorizationGrant grant,
			Client client, AppConfiguration appConfiguration, AttributeService attributeService) {
		super(httpRequest);
		this.client = client;
		this.grant = grant;
		this.appConfiguration = appConfiguration;
		this.attributeService = attributeService;
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

	public AbstractAuthorizationGrant getGrant() {
		return grant;
	}

	public AppConfiguration getAppConfiguration() {
		return appConfiguration;
	}

	public AttributeService getAttributeService() {
		return attributeService;
	}

}
