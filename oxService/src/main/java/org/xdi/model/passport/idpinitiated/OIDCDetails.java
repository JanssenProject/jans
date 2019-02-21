package org.xdi.model.passport.idpinitiated;

/**
 * Created by jgomer on 2019-02-21.
 */
public class OIDCDetails {

    private String authorizationEndpoint;
    private String clientId;
    private String acrValues;

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

}
