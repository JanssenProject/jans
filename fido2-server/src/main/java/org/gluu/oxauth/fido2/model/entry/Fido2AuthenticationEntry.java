/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.oxauth.fido2.model.entry;

import java.io.Serializable;
import java.util.Date;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapJsonObject;

/**
 * Fido2 registration entry
 *
 * @author Yuriy Movchan
 * @version 11/02/2018
 */
public class Fido2AuthenticationEntry extends Fido2Entry implements Serializable {

    private static final long serialVersionUID = -2242931562244920584L;

    @LdapJsonObject
    @LdapAttribute(name = "oxAuthenticationData")
    private Fido2AuthenticationData authenticationData;

    public Fido2AuthenticationEntry() {
    }

    public Fido2AuthenticationEntry(Fido2AuthenticationData authenticationData) {
        this.authenticationData = authenticationData;
        // TODO: Fix
//        this.requestId = authenticationData.getRequestId();
    }

    public Fido2AuthenticationEntry(String dn, String id, Date creationDate, String sessionId, String userInum,
            Fido2AuthenticationData authenticationData) {
        // TODO: Fix
//        super(dn, id, authenticationData.getRequestId(), creationDate, sessionId, userInum);
        this.authenticationData = authenticationData;
    }

    public Fido2AuthenticationData getAuthenticationData() {
        return authenticationData;
    }

    public void setAuthenticationData(Fido2AuthenticationData authenticationData) {
        this.authenticationData = authenticationData;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Fido2AuthenticationEntry [authenticationData=").append(authenticationData).append("]");
        return builder.toString();
    }

}
