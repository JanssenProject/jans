package org.gluu.oxauth.fido2.model.entry;

import java.util.Date;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.ldap.model.BaseEntry;

/**
 * Fido2 base persistence entry
 *
 * @author Yuriy Movchan
 * @version 11/02/2018
 */
@LdapEntry(sortBy = "creationDate")
@LdapObjectClass(values = { "top", "oxFido2Entry" })
public class Fido2Entry extends BaseEntry {

    @LdapAttribute(ignoreDuringUpdate = true, name = "oxId")
    protected String id;

    @LdapAttribute(name = "oxCodeChallenge")
    protected String challange;

    @LdapAttribute(name = "creationDate")
    protected Date creationDate;

    @LdapAttribute(name = "oxSessionStateId")
    protected String sessionId;

    @LdapAttribute(name = "personInum")
    protected String userInum;

    public Fido2Entry() {
    }

    public Fido2Entry(String dn) {
        super(dn);
    }

    public Fido2Entry(String dn, String id, Date creationDate, String sessionId, String userInum, String challange) {
        super(dn);
        this.id = id;
        this.creationDate = creationDate;
        this.sessionId = sessionId;
        this.userInum = userInum;
        this.challange = challange;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChallange() {
        return challange;
    }

    public void setChallange(String challange) {
        this.challange = challange;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserInum() {
        return userInum;
    }

    public void setUserInum(String userInum) {
        this.userInum = userInum;
    }
}
