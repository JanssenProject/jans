/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.json.JSONObject;

import javax.ws.rs.HttpMethod;

import static io.jans.as.model.authorize.DeviceAuthorizationRequestParam.CLIENT_ID;
import static io.jans.as.model.authorize.DeviceAuthorizationRequestParam.SCOPE;
import static io.jans.as.model.authorize.DeviceAuthorizationResponseParam.*;

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
        initClientRequest();
        return _exec();
    }

    @Deprecated
    public DeviceAuthzResponse exec(ClientExecutor clientExecutor) {
        this.clientRequest = new ClientRequest(getUrl(), clientExecutor);
        return _exec();
    }

    private DeviceAuthzResponse _exec() {
        try {
            clientRequest.setHttpMethod(getHttpMethod());
            clientRequest.header("Content-Type", request.getContentType());
            new ClientAuthnEnabler(clientRequest).exec(getRequest());

            final String scopesAsString = Util.listAsString(getRequest().getScopes());

            if (StringUtils.isNotBlank(scopesAsString)) {
                clientRequest.formParameter(SCOPE, scopesAsString);
            }
            if (StringUtils.isNotBlank(getRequest().getClientId())) {
                clientRequest.formParameter(CLIENT_ID, getRequest().getClientId());
            }

            // Call REST Service and handle response
            clientResponse = clientRequest.post(String.class);

            setResponse(new DeviceAuthzResponse(clientResponse));
            String entity = clientResponse.getEntity(String.class);
            getResponse().setEntity(entity);
            getResponse().setHeaders(clientResponse.getMetadata());
            if (StringUtils.isNotBlank(entity)) {
                JSONObject jsonObj = new JSONObject(entity);

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