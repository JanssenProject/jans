/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package io.jans.cacherefresh.service;

import java.util.List;
import java.util.Map;

import io.jans.cacherefresh.model.JansCustomAttribute;
import io.jans.cacherefresh.model.GluuCustomPerson;
import io.jans.cacherefresh.model.User;
import io.jans.orm.model.AttributeData;

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