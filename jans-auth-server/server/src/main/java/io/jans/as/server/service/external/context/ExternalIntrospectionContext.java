/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
