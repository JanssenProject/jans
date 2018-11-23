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
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * Fido2 registration entry
 *
 * @author Yuriy Movchan
 * @version 11/02/2018
 */
@LdapObjectClass(values = { "top", "oxFido2RegistrationEntry" })
public class Fido2RegistrationEntry extends Fido2Entry implements Serializable {

    private static final long serialVersionUID = -2242931562244920584L;

    @LdapJsonObject
    @LdapAttribute(name = "oxRegistrationData")
    private Fido2RegistrationData registrationData;

    public Fido2RegistrationEntry() {
    }

    public Fido2RegistrationEntry(String dn, String id, Date creationDate, String sessionId, String userInum,
            Fido2RegistrationData registrationData) {
        super(dn, id, creationDate, sessionId, userInum, registrationData.getChallenge());
        this.registrationData = registrationData;
    }

    public Fido2RegistrationData getRegistrationData() {
        return registrationData;
    }

    public void setRegistrationData(Fido2RegistrationData registrationData) {
        this.registrationData = registrationData;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Fido2RegistrationEntry [registrationData=").append(registrationData).append("]");
        return builder.toString();
    }
}
