package io.jans.fido2.service.external.context;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.service.external.context.ExternalScriptContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ExternalFido2InterceptionContext extends ExternalScriptContext {

    private final JsonNode jsonNode;
    private CustomScriptConfiguration script;

    public ExternalFido2InterceptionContext(JsonNode jsonNode, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        super(httpRequest, httpResponse);
        this.jsonNode = jsonNode;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

    public JsonNode getJsonNode() {
        return jsonNode;
    }

    @Override
    public String toString() {
        return "ExternalFido2InterceptionContext{" +
                "jsonNode=" + jsonNode != null ? jsonNode.toString() : "" +
                ", script=" + script +
                "} " + super.toString();
    }
}
