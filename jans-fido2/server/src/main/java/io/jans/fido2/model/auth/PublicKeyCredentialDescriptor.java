/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * PublicKeyCredentialDescriptor - https://www.w3.org/TR/webauthn-2/#enum-credentialType
 * @author Yuriy Movchan
 * @version May 08, 2020
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicKeyCredentialDescriptor {

    private String type;
    private String transports[];
    private String id;

    public PublicKeyCredentialDescriptor(String type, String id) {
        this.type = type;
        this.id = id;
    }

    public PublicKeyCredentialDescriptor(String type, String transports[], String id) {
        this.type = type;
        this.transports = transports;
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public String[] getTransports() {
		return transports;
	}

	public String getId() {
        return id;
    }

}
