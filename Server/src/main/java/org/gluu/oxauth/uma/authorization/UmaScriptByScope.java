package org.gluu.oxauth.uma.authorization;

import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.oxauth.persistence.model.Scope;

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
