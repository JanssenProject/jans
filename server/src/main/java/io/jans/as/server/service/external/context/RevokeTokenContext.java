package io.jans.as.server.service.external.context;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import io.jans.as.common.model.registration.Client;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;

/**
 * @author Yuriy Zabrovarnyy
 */
public class RevokeTokenContext extends ExternalScriptContext {

    private final Client client;
    private final AuthorizationGrant grant;
    private final Response.ResponseBuilder responseBuilder;
    private CustomScriptConfiguration script;

    public RevokeTokenContext(HttpServletRequest httpRequest, Client client, AuthorizationGrant grant, Response.ResponseBuilder responseBuilder) {
        super(httpRequest);
        this.client = client;
        this.grant = grant;
        this.responseBuilder = responseBuilder;
    }

    public Client getClient() {
        return client;
    }

    public AuthorizationGrant getGrant() {
        return grant;
    }

    public Response.ResponseBuilder getResponseBuilder() {
        return responseBuilder;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

    @Override
    public String toString() {
        return "RevokeTokenContext{" +
                "clientId=" + (client != null ? client.getClientId() : "") +
                ", script=" + (script != null ? script.getName() : "") +
                "} " + super.toString();
    }
}