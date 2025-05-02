/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class TokenAttributes implements Serializable {

	private static final long serialVersionUID = -3069575637747538483L;
	@JsonProperty("x5cs256")
    private String x5cs256;
    @JsonProperty("online_access")
    private boolean onlineAccess;
    @JsonProperty("authorization_challenge")
    private boolean authorizationChallenge;
    @JsonProperty("attributes")
    private Map<String, String> attributes;
    @JsonProperty("dpopJkt")
    private String dpopJkt;
    @JsonProperty("authorizationDetails")
    private String authorizationDetails;
    @JsonProperty("statusListIndex")
    private Integer statusListIndex;

    public boolean isAuthorizationChallenge() {
        return authorizationChallenge;
    }

    public TokenAttributes setAuthorizationChallenge(boolean authorizationChallenge) {
        this.authorizationChallenge = authorizationChallenge;
        return this;
    }

    public Integer getStatusListIndex() {
        return statusListIndex;
    }

    public void setStatusListIndex(Integer statusListIndex) {
        this.statusListIndex = statusListIndex;
    }

    public String getAuthorizationDetails() {
        return authorizationDetails;
    }

    public void setAuthorizationDetails(String authorizationDetails) {
        this.authorizationDetails = authorizationDetails;
    }

    public String getDpopJkt() {
        return dpopJkt;
    }

    public void setDpopJkt(String dpopJkt) {
        this.dpopJkt = dpopJkt;
    }

    public Map<String, String> getAttributes() {
        if (attributes == null) attributes = new HashMap<>();
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getX5cs256() {
        return x5cs256;
    }

    public void setX5cs256(String x5cs256) {
        this.x5cs256 = x5cs256;
    }

    public boolean isOnlineAccess() {
        return onlineAccess;
    }

    public void setOnlineAccess(boolean onlineAccess) {
        this.onlineAccess = onlineAccess;
    }

    @Override
    public String toString() {
        return "TokenAttributes{" +
                "attributes='" + attributes + '\'' +
                "x5cs256='" + x5cs256 + '\'' +
                "onlineAccess='" + onlineAccess + '\'' +
                "dpopJkt='" + dpopJkt + '\'' +
                "authorizationDetails='" + authorizationDetails + '\'' +
                "authorizationChallenge='" + authorizationChallenge + '\'' +
                '}';
    }
}
