package org.gluu.oxauth.fido2.model.entry;

import java.util.Date;

import org.gluu.persist.model.base.BaseEntry;
import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

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
    private String id;

    @LdapAttribute(name = "oxCodeChallenge")
    private String challange;

    @LdapAttribute(name = "oxCodeChallengeHash")
    private String challangeHash;

    @LdapAttribute(name = "creationDate")
    private Date creationDate;

    @LdapAttribute(name = "oxSessionStateId")
    private String sessionId;

    @LdapAttribute(name = "personInum")
    private String userInum;

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

    public String getChallangeHash() {
        return challangeHash;
    }

    public void setChallangeHash(String challangeHash) {
        this.challangeHash = challangeHash;
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
