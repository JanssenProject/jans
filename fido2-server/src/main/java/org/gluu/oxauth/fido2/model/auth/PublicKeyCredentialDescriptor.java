/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package org.gluu.oxauth.fido2.model.auth;

/**
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
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
