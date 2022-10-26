/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.json.JSONObject;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;

import static io.jans.as.model.authorize.DeviceAuthorizationRequestParam.CLIENT_ID;
import static io.jans.as.model.authorize.DeviceAuthorizationRequestParam.SCOPE;
import static io.jans.as.model.authorize.DeviceAuthorizationResponseParam.DEVICE_CODE;
import static io.jans.as.model.authorize.DeviceAuthorizationResponseParam.EXPIRES_IN;
import static io.jans.as.model.authorize.DeviceAuthorizationResponseParam.INTERVAL;
import static io.jans.as.model.authorize.DeviceAuthorizationResponseParam.USER_CODE;
import static io.jans.as.model.authorize.DeviceAuthorizationResponseParam.VERIFICATION_URI;
import static io.jans.as.model.authorize.DeviceAuthorizationResponseParam.VERIFICATION_URI_COMPLETE;

/**
 * Encapsulates functionality to make Device Authz request calls to an authorization server via REST Services.
 */
public class DeviceAuthzClient extends BaseClient<DeviceAuthzRequest, DeviceAuthzResponse> {

    private static final Logger LOG = Logger.getLogger(DeviceAuthzClient.class);

    /**
     * Construct a device authz client by providing an URL where the REST service is located.
     *
     * @param url The REST service location.
     */
    public DeviceAuthzClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public DeviceAuthzResponse exec() {
        initClient();

        return _exec();
    }

    /**
     * @deprecated Engine should be shared between clients
     */
    @SuppressWarnings("java:S1133")
    @Deprecated
    public DeviceAuthzResponse exec(ClientHttpEngine engine) {
        resteasyClient = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
        webTarget = resteasyClient.target(getUrl());

        return _exec();
    }

    private DeviceAuthzResponse _exec() {
        try {
            Builder clientRequest = webTarget.request();
            applyCookies(clientRequest);

            clientRequest.header("Content-Type", request.getContentType());
            new ClientAuthnEnabler(clientRequest, requestForm).exec(getRequest());

            final String scopesAsString = Util.listAsString(getRequest().getScopes());

            if (StringUtils.isNotBlank(scopesAsString)) {
                requestForm.param(SCOPE, scopesAsString);
            }
            if (StringUtils.isNotBlank(getRequest().getClientId())) {
                requestForm.param(CLIENT_ID, getRequest().getClientId());
            }

            // Call REST Service and handle response
            clientResponse = clientRequest.buildPost(Entity.form(requestForm)).invoke();

            setResponse(new DeviceAuthzResponse(clientResponse));
            if (StringUtils.isNotBlank(response.getEntity())) {
                JSONObject jsonObj = new JSONObject(response.getEntity());

                if (jsonObj.has(USER_CODE)) {
                    getResponse().setUserCode(jsonObj.getString(USER_CODE));
                }
                if (jsonObj.has(DEVICE_CODE)) {
                    getResponse().setDeviceCode(jsonObj.getString(DEVICE_CODE));
                }
                if (jsonObj.has(INTERVAL)) {
                    getResponse().setInterval(jsonObj.getInt(INTERVAL));
                }
                if (jsonObj.has(VERIFICATION_URI)) {
                    getResponse().setVerificationUri(jsonObj.getString(VERIFICATION_URI));
                }
                if (jsonObj.has(VERIFICATION_URI_COMPLETE)) {
                    getResponse().setVerificationUriComplete(jsonObj.getString(VERIFICATION_URI_COMPLETE));
                }
                if (jsonObj.has(EXPIRES_IN)) {
                    getResponse().setExpiresIn(jsonObj.getInt(EXPIRES_IN));
                }
            }

            return getResponse();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            closeConnection();
        }
    }
}