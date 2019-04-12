/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.oxauth.persistence.model.configuration;

import org.gluu.persist.annotation.DN;

/**
 * Provides DN attribute
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
public class Entry {

	@DN
	private String dn;

	public Entry() {}

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
