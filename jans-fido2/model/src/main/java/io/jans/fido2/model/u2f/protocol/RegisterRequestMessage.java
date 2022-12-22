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
 * FIDO U2F registration request message
 *
 * @author Yuriy Movchan Date: 05/15/2015
 */
public class RegisterRequestMessage implements Serializable {

    private static final long serialVersionUID = -5554834606247337007L;

    @JsonProperty
    private final List<AuthenticateRequest> authenticateRequests;

    @JsonProperty
    private final List<RegisterRequest> registerRequests;

    public RegisterRequestMessage(@JsonProperty("authenticateRequests") List<AuthenticateRequest> authenticateRequests,
                                  @JsonProperty("registerRequests") List<RegisterRequest> registerRequests) {
        this.authenticateRequests = authenticateRequests;
        this.registerRequests = registerRequests;
    }

    public List<AuthenticateRequest> getAuthenticateRequests() {
        return Collections.unmodifiableList(authenticateRequests);
    }

    public List<RegisterRequest> getRegisterRequests() {
        return Collections.unmodifiableList(registerRequests);
    }

    @JsonIgnore
    public RegisterRequest getRegisterRequest() {
        return Util.firstItem(registerRequests);
    }

    @JsonIgnore
    public String getRequestId() {
        return Util.firstItem(registerRequests).getChallenge();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RegisterRequestMessage [authenticateRequests=").append(authenticateRequests).append(", registerRequests=").append(registerRequests)
                .append("]");
        return builder.toString();
    }

}
