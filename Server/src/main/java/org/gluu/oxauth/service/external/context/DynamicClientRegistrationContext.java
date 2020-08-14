package org.gluu.oxauth.service.external.context;

import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.oxauth.model.jwt.Jwt;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Yuriy Zabrovarnyy
 */
public class DynamicClientRegistrationContext extends ExternalScriptContext {

    private CustomScriptConfiguration script;
    private JSONObject registerRequest;
    private Jwt softwareStatement;

    public DynamicClientRegistrationContext(HttpServletRequest httpRequest, JSONObject registerRequest, CustomScriptConfiguration script) {
        super(httpRequest);
        this.script = script;
        this.registerRequest = registerRequest;
    }

    public Jwt getSoftwareStatement() {
        return softwareStatement;
    }

    public void setSoftwareStatement(Jwt softwareStatement) {
        this.softwareStatement = softwareStatement;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

    public JSONObject getRegisterRequest() {
        return registerRequest;
    }

    public void setRegisterRequest(JSONObject registerRequest) {
        this.registerRequest = registerRequest;
    }

    @Override
    public String toString() {
        return "DynamicClientRegistrationContext{" +
                "softwareStatement=" + softwareStatement +
                "registerRequest=" + registerRequest +
                "script=" + script +
                "} " + super.toString();
    }
}
