package org.gluu.oxauth.ciba.service.external.context;

import org.gluu.oxauth.model.configuration.AppConfiguration;

/**
 * @author Milton BO
 */
public class ExternalCibaEndUserNotificationContext {

    private final AppConfiguration appConfiguration;
    private final String scope;
    private final String acrValues;
    private final String authReqId;
    private final String deviceRegistrationToken;

    public ExternalCibaEndUserNotificationContext(String scope, String acrValues, String authReqId,
                                                  String deviceRegistrationToken, AppConfiguration appConfiguration) {
        this.appConfiguration = appConfiguration;
        this.scope = scope;
        this.acrValues = acrValues;
        this.authReqId = authReqId;
        this.deviceRegistrationToken = deviceRegistrationToken;
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
