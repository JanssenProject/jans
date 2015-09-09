/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/06/2014
 */

public class ClientReadParams implements IParams {

    @JsonProperty(value = "registration_client_uri")
    private String registrationClientUri;
    @JsonProperty(value = "registration_access_token")
    private String registrationAccessToken;

    public String getRegistrationAccessToken() {
        return registrationAccessToken;
    }

    public void setRegistrationAccessToken(String registrationAccessToken) {
        this.registrationAccessToken = registrationAccessToken;
    }

    public String getRegistrationClientUri() {
        return registrationClientUri;
    }

    public void setRegistrationClientUri(String registrationClientUri) {
        this.registrationClientUri = registrationClientUri;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ClientReadParams");
        sb.append("{registrationAccessToken='").append(registrationAccessToken).append('\'');
        sb.append(", registrationClientUri='").append(registrationClientUri).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
