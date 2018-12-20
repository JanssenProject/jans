package org.xdi.oxauth.service.external.context;

import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.service.AttributeService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ExternalIntrospectionContext extends ExternalScriptContext {

    private final AuthorizationGrant tokenGrant;
    private final AppConfiguration appConfiguration;
    private final AttributeService attributeService;

    private CustomScriptConfiguration script;

    public ExternalIntrospectionContext(AuthorizationGrant tokenGrant, HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                        AppConfiguration appConfiguration, AttributeService attributeService) {
        super(httpRequest, httpResponse);
        this.tokenGrant = tokenGrant;
        this.appConfiguration = appConfiguration;
        this.attributeService = attributeService;
    }

    public AuthorizationGrant getTokenGrant() {
        return tokenGrant;
    }

    public AppConfiguration getAppConfiguration() {
        return appConfiguration;
    }

    public AttributeService getAttributeService() {
        return attributeService;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }
}
