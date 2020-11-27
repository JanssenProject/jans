/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.common.model.registration.Client;
import io.jans.as.server.model.common.SessionId;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
