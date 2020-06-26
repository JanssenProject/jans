package org.gluu.oxauth.service.external.context;

import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.oxauth.model.common.ExecutionContext;
import org.gluu.oxauth.model.registration.Client;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ExternalUmaRptClaimsContext extends ExternalScriptContext {

    private final Client client;
    private CustomScriptConfiguration script;
    private boolean isTranferPropertiesIntoJwtClaims = true;

    public ExternalUmaRptClaimsContext(Client client, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        super(httpRequest, httpResponse);
        this.client = client;
    }

    public ExternalUmaRptClaimsContext(ExecutionContext executionContext) {
        this(executionContext.getClient(), executionContext.getHttpRequest(), executionContext.getHttpResponse());
    }

    public Client getClient() {
        return client;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

    public boolean isTranferPropertiesIntoJwtClaims() {
        return isTranferPropertiesIntoJwtClaims;
    }

    public void setTranferPropertiesIntoJwtClaims(boolean tranferPropertiesIntoJwtClaims) {
        isTranferPropertiesIntoJwtClaims = tranferPropertiesIntoJwtClaims;
    }

    @Override
    public String toString() {
        return "ExternalUmaRptClaimsContext{" +
                "client=" + client +
                ", script=" + script +
                ", isTranferPropertiesIntoJwtClaims=" + isTranferPropertiesIntoJwtClaims +
                "} " + super.toString();
    }
}
