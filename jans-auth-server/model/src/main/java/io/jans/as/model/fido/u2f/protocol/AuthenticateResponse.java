/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.fido.u2f.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jans.as.model.fido.u2f.exception.BadInputException;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import java.io.Serializable;

/**
 * FIDO U2F device authentication response
 *
 * @author Yuriy Movchan Date: 05/13/2015
 */
@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here:
// http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateResponse implements Serializable {

    private static final long serialVersionUID = -4854326288654670000L;

    /**
     * base64(UTF8(client data))
     */
    @JsonProperty
    private final String clientData;

    @JsonIgnore
    private final transient ClientData clientDataRef;

    /* base64(raw response from U2F device) */
    @JsonProperty
    private final String signatureData;

    /* keyHandle originally passed */
    @JsonProperty
    private final String keyHandle;
    
	/**
	 * base64(UTF8(device data))
	 */
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final String deviceData;

	public String getDeviceData() {
		return deviceData;
	}

    public AuthenticateResponse(@JsonProperty("clientData") String clientData, @JsonProperty("signatureData") String signatureData,
                                @JsonProperty("keyHandle") String keyHandle, @JsonProperty("deviceData") String deviceData) throws BadInputException {
        this.clientData = clientData;
        this.signatureData = signatureData;
        this.keyHandle = keyHandle;
        this.clientDataRef = new ClientData(clientData);
        this.deviceData = deviceData;
    }

    public ClientData getClientData() {
        return clientDataRef;
    }

    public String getClientDataRaw() {
        return clientData;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public String getKeyHandle() {
        return keyHandle;
    }

    @JsonIgnore
    public String getRequestId() {
        return getClientData().getChallenge();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AuthenticateResponse [clientData=").append(clientData).append(", signatureData=").append(signatureData).append(" , deviceData=").append(deviceData).append(", keyHandle=")
                .append(keyHandle).append("]");
        return builder.toString();
    }

}
