/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package io.jans.keycloak.link.service;

import io.jans.link.model.GluuCustomPerson;

public interface IPersonService {

	public abstract GluuCustomPerson findPersonByDn(String dn, String... returnAttributes);

	public abstract boolean contains(String dn);

	/**
	 * Build DN string for person
	 * 
	 * @param inum
	 *            Inum
	 * @return DN string for specified person or DN for persons branch if inum is
	 *         null
	 * @throws Exception
	 */
	public abstract String getDnForPerson(String inum);

}