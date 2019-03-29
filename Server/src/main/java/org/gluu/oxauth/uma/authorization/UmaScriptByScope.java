package org.gluu.oxauth.uma.authorization;

import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.oxauth.model.uma.persistence.UmaScopeDescription;

/**
 * @author yuriyz
 */
public class UmaScriptByScope {

    private UmaScopeDescription scope;

    private CustomScriptConfiguration script;

    public UmaScriptByScope() {
    }

    public UmaScriptByScope(UmaScopeDescription scope, CustomScriptConfiguration script) {
        this.scope = scope;
        this.script = script;
    }

    public UmaScopeDescription getScope() {
        return scope;
    }

    public void setScope(UmaScopeDescription scope) {
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
