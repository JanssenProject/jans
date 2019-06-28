package org.gluu.oxauth.service.external.context;

import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.oxauth.model.common.AuthorizationGrant;
import org.gluu.oxauth.model.common.User;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.service.AttributeService;

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
    private Jwt accessTokenAsJwt;
    private boolean tranferIntrospectionPropertiesIntoJwtClaims = true;
    private AuthorizationGrant grantOfIntrospectionToken;

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

    public Jwt getAccessTokenAsJwt() {
        return accessTokenAsJwt;
    }

    public void setAccessTokenAsJwt(Jwt accessTokenAsJwt) {
        this.accessTokenAsJwt = accessTokenAsJwt;
    }

    public boolean isTranferIntrospectionPropertiesIntoJwtClaims() {
        return tranferIntrospectionPropertiesIntoJwtClaims;
    }

    public void setTranferIntrospectionPropertiesIntoJwtClaims(boolean tranferIntrospectionPropertiesIntoJwtClaims) {
        this.tranferIntrospectionPropertiesIntoJwtClaims = tranferIntrospectionPropertiesIntoJwtClaims;
    }

    public AuthorizationGrant getGrantOfIntrospectionToken() {
        return grantOfIntrospectionToken;
    }

    public void setGrantOfIntrospectionToken(AuthorizationGrant grantOfIntrospectionToken) {
        this.grantOfIntrospectionToken = grantOfIntrospectionToken;
    }

    public User getUser() {
        return grantOfIntrospectionToken != null ? grantOfIntrospectionToken.getUser() : null;
    }
}
