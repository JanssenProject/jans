/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ExternalPostAuthnContext extends ExternalScriptContext {

    private final Client client;
    private final SessionId session;
    private CustomScriptConfiguration script;

    public ExternalPostAuthnContext(Client client, SessionId session, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        super(httpRequest, httpResponse);
        this.client = client;
        this.session = session;
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
                "} " + super.toString();
    }
}
