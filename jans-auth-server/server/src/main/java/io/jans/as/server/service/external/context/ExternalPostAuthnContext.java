/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.common.Prompt;
import io.jans.as.server.authorize.ws.rs.AuthzRequest;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ExternalPostAuthnContext extends ExternalScriptContext {

    private final Client client;
    private final SessionId session;
    private CustomScriptConfiguration script;
    private AuthzRequest authzRequest;
    private List<Prompt> prompts;

    public ExternalPostAuthnContext(Client client, SessionId session, AuthzRequest authzRequest, List<Prompt> prompts) {
        super(authzRequest.getHttpRequest(), authzRequest.getHttpResponse());
        this.client = client;
        this.session = session;
        this.authzRequest = authzRequest;
        this.prompts = prompts;
    }

    public AuthzRequest getAuthzRequest() {
        return authzRequest;
    }

    public void setAuthzRequest(AuthzRequest authzRequest) {
        this.authzRequest = authzRequest;
    }

    public List<Prompt> getPrompts() {
        return prompts;
    }

    public void setPrompts(List<Prompt> prompts) {
        this.prompts = prompts;
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

    public SessionId getSession() {
        return session;
    }

    @Override
    public String toString() {
        return "ExternalPostAuthnContext{" +
                "client=" + client +
                ", session=" + (session != null ? session.getId() : "") +
                ", script=" + script +
                ", prompts=" + prompts +
                ", authzRequest=" + authzRequest +
                "} " + super.toString();
    }
}
