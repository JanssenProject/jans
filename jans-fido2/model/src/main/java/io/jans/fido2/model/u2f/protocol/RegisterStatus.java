/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jans.fido2.model.u2f.exception.BadInputException;

import java.io.Serializable;

/**
 * FIDO U2F device registration status response
 *
 * @author Yuriy Movchan Date: 05/20/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterStatus implements Serializable {

    private static final long serialVersionUID = -1113719742487477953L;

    @JsonProperty
    private final String status;

    @JsonProperty
    private final String challenge;

    public RegisterStatus(@JsonProperty("status") String status, @JsonProperty("challenge") String challenge) throws BadInputException {
        this.status = status;
        this.challenge = challenge;
    }

    public String getStatus() {
        return status;
    }

    public String getChallenge() {
        return challenge;
    }

    @JsonIgnore
    public String getRequestId() {
        return getChallenge();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RegisterStatus [status=").append(status).append(", challenge=").append(challenge).append("]");
        return builder.toString();
    }

}
