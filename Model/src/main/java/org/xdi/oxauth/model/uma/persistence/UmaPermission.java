/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma.persistence;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

import java.util.Date;
import java.util.List;

/**
 * UMA permission
 *
 * @author Yuriy Zabrovarnyy
 * @version 2.0, date: 17/05/2017
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthUmaResourceSetPermission"})
public class UmaPermission {

    @LdapDN
    private String dn;
    @LdapAttribute(name = "oxAmHost")
	private String amHost;
    @LdapAttribute(name = "oxHost")
	private String host;
    @LdapAttribute(name = "oxTicket")
	private String ticket;
    @LdapAttribute(name = "oxConfigurationCode")
	private String configurationCode;
    @LdapAttribute(name = "oxAuthExpiration")
	private Date expirationDate;

    // resource set permission request
    @LdapAttribute(name = "oxResourceSetId")
    private String resourceId;
    @LdapAttribute(name = "oxAuthUmaScope")
    private List<String> scopeDns;

    private boolean expired;

    public UmaPermission() {
    }

    public UmaPermission(String resourceId, List<String> scopes, String amHost, String host, String ticket,
                         String configurationCode, Date expirationDate) {
		this.resourceId = resourceId;
        this.scopeDns = scopes;
		this.amHost = amHost;
		this.host = host;
		this.ticket = ticket;
		this.configurationCode = configurationCode;
		this.expirationDate = expirationDate;

		checkExpired();
	}

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

    public void checkExpired() {
		checkExpired(new Date());
	}

	public void checkExpired(Date now) {
        if (now.after(expirationDate)) {
            expired = true;
        }
	}

	public boolean isValid() {
		return !expired;
	}

	public String getAmHost() {
		return amHost;
	}

	public void setAmHost(String amHost) {
		this.amHost = amHost;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getConfigurationCode() {
		return configurationCode;
	}

	public void setConfigurationCode(String configurationCode) {
		this.configurationCode = configurationCode;
	}

	public String getTicket() {
		return ticket;
	}

	public void setTicket(String ticket) {
		this.ticket = ticket;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public List<String> getScopeDns() {
        return scopeDns;
    }

    public void setScopeDns(List<String> p_scopeDns) {
        scopeDns = p_scopeDns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UmaPermission that = (UmaPermission) o;

        return !(ticket != null ? !ticket.equals(that.ticket) : that.ticket != null);

    }

    @Override
    public int hashCode() {
        return ticket != null ? ticket.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ResourceSetPermission");
        sb.append("{amHost='").append(amHost).append('\'');
        sb.append(", host='").append(host).append('\'');
        sb.append(", ticket='").append(ticket).append('\'');
        sb.append(", configurationCode='").append(configurationCode).append('\'');
        sb.append(", expirationDate=").append(expirationDate);
        sb.append(", expired=").append(expired);
        sb.append(", resourceId='").append(resourceId).append('\'');
        sb.append(", scopes=").append(scopeDns);
        sb.append('}');
        return sb.toString();
    }
}
