/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.authorize.DeviceAuthorizationResponseParam;
import io.jans.as.model.authorize.DeviceAuthzErrorResponseType;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import jakarta.ws.rs.core.Response;

/**
 * Represents a device authz response received from the authorization server.
 */
public class DeviceAuthzResponse extends BaseResponseWithErrors<DeviceAuthzErrorResponseType> {

    private static final Logger LOG = Logger.getLogger(DeviceAuthzResponse.class);

    private String userCode;
    private String deviceCode;
    private Integer interval;
    private String verificationUri;
    private String verificationUriComplete;
    private Integer expiresIn;

    public DeviceAuthzResponse(Response clientResponse) {
        super(clientResponse);
    }

    @Override
    public DeviceAuthzErrorResponseType fromString(String string) {
        return DeviceAuthzErrorResponseType.fromString(string);
    }

    @Override
    public void injectDataFromJson(String json) {
        if (StringUtils.isBlank(json)) {
            return;
        }

        try {
            JSONObject jsonObj = new JSONObject(json);
            if (jsonObj.has(DeviceAuthorizationResponseParam.USER_CODE)) {
                setUserCode(jsonObj.getString(DeviceAuthorizationResponseParam.USER_CODE));
                jsonObj.remove(DeviceAuthorizationResponseParam.USER_CODE);
            }
            if (jsonObj.has(DeviceAuthorizationResponseParam.DEVICE_CODE)) {
                setDeviceCode(jsonObj.getString(DeviceAuthorizationResponseParam.DEVICE_CODE));
                jsonObj.remove(DeviceAuthorizationResponseParam.DEVICE_CODE);
            }
            if (jsonObj.has(DeviceAuthorizationResponseParam.INTERVAL)) {
                setInterval(jsonObj.getInt(DeviceAuthorizationResponseParam.INTERVAL));
                jsonObj.remove(DeviceAuthorizationResponseParam.INTERVAL);
            }
            if (jsonObj.has(DeviceAuthorizationResponseParam.VERIFICATION_URI)) {
                setVerificationUri(jsonObj.getString(DeviceAuthorizationResponseParam.VERIFICATION_URI));
                jsonObj.remove(DeviceAuthorizationResponseParam.VERIFICATION_URI);
            }
            if (jsonObj.has(DeviceAuthorizationResponseParam.VERIFICATION_URI_COMPLETE)) {
                setVerificationUriComplete(jsonObj.getString(DeviceAuthorizationResponseParam.VERIFICATION_URI_COMPLETE));
                jsonObj.remove(DeviceAuthorizationResponseParam.VERIFICATION_URI_COMPLETE);
            }
            if (jsonObj.has(DeviceAuthorizationResponseParam.EXPIRES_IN)) {
                setExpiresIn(jsonObj.getInt(DeviceAuthorizationResponseParam.EXPIRES_IN));
                jsonObj.remove(DeviceAuthorizationResponseParam.EXPIRES_IN);
            }
        } catch (JSONException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public String getVerificationUri() {
        return verificationUri;
    }

    public void setVerificationUri(String verificationUri) {
        this.verificationUri = verificationUri;
    }

    public String getVerificationUriComplete() {
        return verificationUriComplete;
    }

    public void setVerificationUriComplete(String verificationUriComplete) {
        this.verificationUriComplete = verificationUriComplete;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }
}