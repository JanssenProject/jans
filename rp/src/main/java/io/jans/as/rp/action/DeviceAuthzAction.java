/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.rp.action;

import io.jans.as.client.DeviceAuthzClient;
import io.jans.as.client.DeviceAuthzRequest;
import io.jans.as.client.DeviceAuthzResponse;
import org.slf4j.Logger;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named
@SessionScoped
public class DeviceAuthzAction implements Serializable {

    private static final long serialVersionUID = -5920839613190688979L;

    @Inject
    private Logger log;

    @Inject
    private TokenAction tokenAction;

    // Request data
    private String deviceAuthzEndpoint;
    private List<String> scope;
    private String clientId;
    private String clientSecret;

    private String deviceAuthzPageUrl;
    private boolean showResults;
    private String requestString;
    private String responseString;
    private String jsonResponse;

    public void exec() {
        DeviceAuthzRequest deviceAuthzRequest = new DeviceAuthzRequest(clientId, scope);
        deviceAuthzRequest.setAuthUsername(clientId);
        deviceAuthzRequest.setAuthPassword(clientSecret);

        DeviceAuthzClient deviceAuthzlient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzlient.setRequest(deviceAuthzRequest);
        DeviceAuthzResponse deviceAuthzResponse = deviceAuthzlient.exec();

        requestString = deviceAuthzlient.getRequestAsString();
        responseString = deviceAuthzlient.getResponseAsString();
        if (deviceAuthzlient.getResponse().getStatus() >= 200
                && deviceAuthzlient.getResponse().getStatus() < 300) {
            jsonResponse = deviceAuthzlient.getResponse().getEntity();
        }

        tokenAction.setDeviceCode(deviceAuthzResponse.getDeviceCode());

        showResults = true;
        deviceAuthzPageUrl = deviceAuthzEndpoint.replace("/restv1/device_authorization", "/device_authorization.htm");
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getDeviceAuthzEndpoint() {
        return deviceAuthzEndpoint;
    }

    public void setDeviceAuthzEndpoint(String deviceAuthzEndpoint) {
        this.deviceAuthzEndpoint = deviceAuthzEndpoint;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public boolean isShowResults() {
        return showResults;
    }

    public void setShowResults(boolean showResults) {
        this.showResults = showResults;
    }

    public String getRequestString() {
        return requestString;
    }

    public void setRequestString(String requestString) {
        this.requestString = requestString;
    }

    public String getResponseString() {
        return responseString;
    }

    public void setResponseString(String responseString) {
        this.responseString = responseString;
    }

    public String getJsonResponse() {
        return jsonResponse;
    }

    public void setJsonResponse(String jsonResponse) {
        this.jsonResponse = jsonResponse;
    }

    public String getDeviceAuthzPageUrl() {
        return deviceAuthzPageUrl;
    }

    public void setDeviceAuthzPageUrl(String deviceAuthzPageUrl) {
        this.deviceAuthzPageUrl = deviceAuthzPageUrl;
    }
}
