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

import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import java.io.Serializable;

/**
 * FIDO U2F device registration response
 *
 * @author Yuriy Movchan Date: 05/13/2015
 */
@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here:
// http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterResponse implements Serializable {

    private static final long serialVersionUID = -4192863815075074953L;

    /**
     * base64 (raw registration response message)
     */
    @JsonProperty
    private final String registrationData;

    /**
     * base64(UTF8(client data))
     */
    @JsonProperty
    private final String clientData;

    /**
     * base64(UTF8(device data))
     */
    @JsonProperty
    private final String deviceData;

    @JsonIgnore
    private final transient ClientData clientDataRef;

    public RegisterResponse(@JsonProperty("registrationData") String registrationData, @JsonProperty("clientData") String clientData, @JsonProperty("deviceData") String deviceData) throws BadInputException {
        this.registrationData = registrationData;
        this.clientData = clientData;
        this.clientDataRef = new ClientData(clientData);
        this.deviceData = deviceData;
    }

    public String getRegistrationData() {
        return registrationData;
    }

    public ClientData getClientData() {
        return clientDataRef;
    }

    public String getDeviceData() {
        return deviceData;
    }

    @JsonIgnore
    public String getRequestId() {
        return getClientData().getChallenge();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RegisterResponse [registrationData=").append(registrationData).append(", clientData=").append(clientData).append(", deviceData=")
                .append(deviceData).append("]");
        return builder.toString();
    }

}
