/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.ciba.CibaEncryptionService;

/**
 * @author Milton BO
 */
public class ExternalCibaEndUserNotificationContext {

    private final AppConfiguration appConfiguration;
    private final CibaEncryptionService encryptionService;
    private final String scope;
    private final String acrValues;
    private final String authReqId;
    private final String deviceRegistrationToken;

    public ExternalCibaEndUserNotificationContext(String scope, String acrValues, String authReqId,
                                                  String deviceRegistrationToken, AppConfiguration appConfiguration,
                                                  CibaEncryptionService encryptionService) {
        this.appConfiguration = appConfiguration;
        this.scope = scope;
        this.acrValues = acrValues;
        this.authReqId = authReqId;
        this.deviceRegistrationToken = deviceRegistrationToken;
        this.encryptionService = encryptionService;
    }

    public AppConfiguration getAppConfiguration() {
        return appConfiguration;
    }

    public String getScope() {
        return scope;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public String getAuthReqId() {
        return authReqId;
    }

    public String getDeviceRegistrationToken() {
        return deviceRegistrationToken;
    }

    public CibaEncryptionService getEncryptionService() {
        return encryptionService;
    }

    @Override
    public String toString() {
        return "ExternalCibaEndUserNotificationContext{" +
                ", scope='" + scope + '\'' +
                ", acrValues='" + acrValues + '\'' +
                ", authReqId='" + authReqId + '\'' +
                ", deviceRegistrationToken='" + deviceRegistrationToken + '\'' +
                '}';
    }

}
