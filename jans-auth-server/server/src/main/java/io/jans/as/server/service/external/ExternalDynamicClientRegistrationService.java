/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external;

import io.jans.as.client.RegisterRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.context.DynamicClientRegistrationContext;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.service.custom.script.ExternalScriptService;
import org.json.JSONObject;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;

/**
 * Provides factory methods needed to create external dynamic client registration extension
 *
 * @author Yuriy Movchan Date: 01/08/2015
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
@SuppressWarnings("java:S1172")
public class ExternalDynamicClientRegistrationService extends ExternalScriptService {

    private static final long serialVersionUID = 1416361273036208688L;

    public ExternalDynamicClientRegistrationService() {
        super(CustomScriptType.CLIENT_REGISTRATION);
    }

    public boolean executeExternalCreateClientMethod(CustomScriptConfiguration customScriptConfiguration, RegisterRequest registerRequest, Client client, HttpServletRequest httpRequest) {
        return false;
    }

    public boolean executeExternalCreateClientMethods(RegisterRequest registerRequest, Client client, HttpServletRequest httpRequest) {
        return false;
    }

    public boolean executeExternalUpdateClientMethod(HttpServletRequest httpRequest, CustomScriptConfiguration script, RegisterRequest registerRequest, Client client) {
        return false;
    }

    public boolean executeExternalUpdateClientMethods(HttpServletRequest httpRequest, RegisterRequest registerRequest, Client client) {
        return false;
    }

    public JSONObject getSoftwareStatementJwks(HttpServletRequest httpRequest, JSONObject registerRequest, Jwt softwareStatement) {
        return null;
    }

    public String getSoftwareStatementHmacSecret(HttpServletRequest httpRequest, JSONObject registerRequest, Jwt softwareStatement) {
        return null;
    }

    public JSONObject getDcrJwks(HttpServletRequest httpRequest, Jwt dcr) {
        return null;
    }

    public String getDcrHmacSecret(HttpServletRequest httpRequest, Jwt dcr) {
        return null;
    }

    public boolean isCertValidForClient(X509Certificate cert, DynamicClientRegistrationContext context) {
        return false;
    }

    public boolean modifyPostResponse(JSONObject responseAsJsonObject, ExecutionContext context) {
        return false;
    }

    public boolean modifyPutResponse(JSONObject responseAsJsonObject, ExecutionContext context) {
        return false;
    }

    public boolean modifyReadResponse(JSONObject responseAsJsonObject, ExecutionContext context) {
        return false;
    }
}
