/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jans.fido2.model.u2f.U2fConstants;

import java.io.Serializable;

/**
 * FIDO U2F device registration request
 *
 * @author Yuriy Movchan Date: 05/13/2015
 */
public class RegisterRequest implements Serializable {

    private static final long serialVersionUID = -7804531602792040593L;

    /**
     * Version of the protocol that the to-be-registered U2F token must speak.
     * For the version of the protocol described herein, must be "U2F_V2"
     */
    @JsonProperty
    private static final String VERSION = U2fConstants.U2F_PROTOCOL_VERSION;

    /**
     * The websafe-base64-encoded challenge.
     */
    @JsonProperty
    private final String challenge;

    /**
     * The application id that the RP would like to assert. The U2F token will
     * enforce that the key handle provided above is associated with this
     * application id. The browser enforces that the calling origin belongs to
     * the application identified by the application id.
     */
    @JsonProperty
    private final String appId;

    public RegisterRequest(@JsonProperty("challenge") String challenge, @JsonProperty("appId") String appId) {
        this.challenge = challenge;
        this.appId = appId;
    }

    public String getChallenge() {
        return challenge;
    }

    public String getAppId() {
        return appId;
    }

    @JsonIgnore
    public String getRequestId() {
        return getChallenge();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RegisterRequest [version=").append(VERSION).append(", challenge=").append(challenge).append(", appId=").append(appId).append("]");
        return builder.toString();
    }

}
