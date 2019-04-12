/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.oxauth.fido2.model.entry;

import java.io.Serializable;
import java.util.Date;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.JsonObject;
import org.gluu.persist.annotation.ObjectClass;

/**
 * Fido2 registration entry
 *
 * @author Yuriy Movchan
 * @version 11/02/2018
 */
@ObjectClass(values = { "top", "oxFido2AuthenticationEntry" })
public class Fido2AuthenticationEntry extends Fido2Entry implements Serializable {

    private static final long serialVersionUID = -2242931562244920584L;

    @JsonObject
    @AttributeName(name = "oxAuthenticationData")
    private Fido2AuthenticationData authenticationData;

    @JsonObject
    @AttributeName(name = "oxStatus")
    private Fido2AuthenticationStatus authenticationStatus;

    public Fido2AuthenticationEntry() {
    }

    public Fido2AuthenticationEntry(String dn, String id, Date creationDate, String sessionId, String userInum,
            Fido2AuthenticationData authenticationData) {
        super(dn, id, creationDate, sessionId, userInum, authenticationData.getChallenge());
        this.authenticationData = authenticationData;
    }

    public Fido2AuthenticationData getAuthenticationData() {
        return authenticationData;
    }

    public void setAuthenticationData(Fido2AuthenticationData authenticationData) {
        this.authenticationData = authenticationData;
    }

    public Fido2AuthenticationStatus getAuthenticationStatus() {
        return authenticationStatus;
    }

    public void setAuthenticationStatus(Fido2AuthenticationStatus authenticationStatus) {
        this.authenticationStatus = authenticationStatus;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Fido2AuthenticationEntry [authenticationData=").append(authenticationData).append("]");
        return builder.toString();
    }
}
