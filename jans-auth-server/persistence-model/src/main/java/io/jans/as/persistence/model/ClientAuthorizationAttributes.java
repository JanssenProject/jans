package io.jans.as.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @author Yuriy Z
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientAuthorizationAttributes implements Serializable {

    @JsonProperty("authorizationDetails")
    private String authorizationDetails;

    public String getAuthorizationDetails() {
        return authorizationDetails;
    }

    public void setAuthorizationDetails(String authorizationDetails) {
        this.authorizationDetails = authorizationDetails;
    }
}
