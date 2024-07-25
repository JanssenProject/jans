/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Strings;

import java.util.Arrays;

/**
 * PublicKeyCredentialDescriptor - https://www.w3.org/TR/webauthn-2/#enum-credentialType
 * @author Yuriy Movchan
 * @version May 08, 2020
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicKeyCredentialDescriptor {

    private String type;
    private String[] transports;
    private String id;

    public PublicKeyCredentialDescriptor(String id) {
        this.type = PublicKeyCredentialType.PUBLIC_KEY.getKeyName();
        this.id = id;
    }

    public PublicKeyCredentialDescriptor(String[] transports, String id) {
        this.type = PublicKeyCredentialType.PUBLIC_KEY.getKeyName();
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

    @Override
    public String toString() {
        return "PublicKeyCredentialDescriptor{" +
                "type='" + type + '\'' +
                ", transports=" + Arrays.toString(transports) +
                ", id='" + id + '\'' +
                '}';
    }
}
