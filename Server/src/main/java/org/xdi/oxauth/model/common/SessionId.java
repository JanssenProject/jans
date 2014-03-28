package org.xdi.oxauth.model.common;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version 0.9, 05/28/2013
 */
@Name("sessionUser")
@AutoCreate
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthSessionId"})
public class SessionId implements Serializable {

    @LdapDN
    private String dn;
    @LdapAttribute(name = "uniqueIdentifier")
    private String id;
    @LdapAttribute(name = "lastModifiedTime")
    private Date lastUsedAt;
    @LdapAttribute(name = "oxAuthUserDN")
    private String userDn;
    @LdapAttribute(name = "oxAuthAuthenticationTime")
    private Date authenticationTime;
    @LdapAttribute(name = "oxAuthPermissionGranted")
    private Boolean permissionGranted;

    public SessionId() {
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

    public String getId() {
        return id;
    }

    public void setId(String p_id) {
        id = p_id;
    }

    public Date getLastUsedAt() {
        return lastUsedAt != null ? new Date(lastUsedAt.getTime()) : null;
    }

    public void setLastUsedAt(Date p_lastUsedAt) {
        lastUsedAt = p_lastUsedAt != null ? new Date(p_lastUsedAt.getTime()) : null;
    }

    public String getUserDn() {
        return userDn;
    }

    public void setUserDn(String p_userDn) {
        userDn = p_userDn;
    }

    public Date getAuthenticationTime() {
        return authenticationTime != null ? new Date(authenticationTime.getTime()) : null;
    }

    public void setAuthenticationTime(Date authenticationTime) {
        this.authenticationTime = authenticationTime != null ? new Date(authenticationTime.getTime()) : null;
    }

    public Boolean isPermissionGranted() {
        return permissionGranted == null ? false : permissionGranted;
    }

    public void setPermissionGranted(Boolean permissionGranted) {
        this.permissionGranted = permissionGranted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SessionId id1 = (SessionId) o;

        if (id != null ? !id.equals(id1.id) : id1.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SessionId [dn=").append(dn).append(", id=").append(id).append(", lastUsedAt=").append(lastUsedAt)
				.append(", userDn=").append(userDn).append(", authenticationTime=").append(authenticationTime)
				.append(", permissionGranted=").append(permissionGranted).append("]");
		return builder.toString();
	}

}
