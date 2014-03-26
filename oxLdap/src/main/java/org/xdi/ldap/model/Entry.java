package org.xdi.ldap.model;

import java.io.Serializable;

import org.gluu.site.ldap.persistence.annotation.LdapDN;

/**
 * Provides DN attribute
 * 
 * @author Yuriy Movchan Date: 07/10/2010
 */
public class Entry implements Serializable {

	private static final long serialVersionUID = 6602706707181973761L;

	@LdapDN
	private String dn;

	public Entry() {
	}

	public Entry(String dn) {
		super();
		this.dn = dn;
	}

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public String getBaseDn() {
		return dn;
	}

	public void setBaseDn(String dn) {
		this.dn = dn;
	}

	@Override
	public String toString() {
		return String.format("Entry [dn=%s]", dn);
	}

}
