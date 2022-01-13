/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.common.model.common.User;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ExternalResourceOwnerPasswordCredentialsContext extends ExternalScriptContext {

    private final ExecutionContext executionContext;

    private User user;
    private CustomScriptConfiguration script;

    public ExternalResourceOwnerPasswordCredentialsContext(ExecutionContext executionContext) {
        super(executionContext.getHttpRequest(), executionContext.getHttpResponse());
        this.executionContext = executionContext;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

    @Override
    public String toString() {
        return "ExternalResourceOwnerPasswordCredentialsContext{" +
                "user=" + user +
                "script=" + script +
                "executionContext=" + executionContext +
                "} " + super.toString();
    }
}
