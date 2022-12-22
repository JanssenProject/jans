/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jans.as.model.util.Util;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * FIDO U2F authentication request message
 *
 * @author Yuriy Movchan Date: 05/15/2015
 */
public class AuthenticateRequestMessage implements Serializable {

    private static final long serialVersionUID = 5492097239884163697L;

    @JsonProperty
    private List<AuthenticateRequest> authenticateRequests;

    public AuthenticateRequestMessage() {
    }

    public AuthenticateRequestMessage(@JsonProperty("authenticateRequests") List<AuthenticateRequest> authenticateRequests) {
        this.authenticateRequests = authenticateRequests;
    }

    public List<AuthenticateRequest> getAuthenticateRequests() {
        return Collections.unmodifiableList(authenticateRequests);
    }

    public void setAuthenticateRequests(List<AuthenticateRequest> authenticateRequests) {
        this.authenticateRequests = authenticateRequests;
    }

    @JsonIgnore
    public String getRequestId() {
        return Util.firstItem(authenticateRequests).getChallenge();
    }

    @JsonIgnore
    public String getAppId() {
        return Util.firstItem(authenticateRequests).getAppId();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AuthenticateRequestMessage [authenticateRequests=").append(authenticateRequests).append("]");
        return builder.toString();
    }

}
