/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.authorization;

import io.jans.as.persistence.model.Scope;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;

/**
 * @author yuriyz
 */
public class UmaScriptByScope {

    private Scope scope;

    private CustomScriptConfiguration script;

    public UmaScriptByScope() {
    }

    public UmaScriptByScope(Scope scope, CustomScriptConfiguration script) {
        this.scope = scope;
        this.script = script;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

    @Override
    public String toString() {
        return "UmaScript{" +
                "scope=" + scope +
                ", script=" + script +
                '}';
    }
}
