/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.link.service;

import java.io.Serializable;
import java.util.Date;
import java.time.Instant;
//import javax.inject.Inject;

import io.jans.link.model.*;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

/**
 * Provides operations with persons
 *
 * @author Yuriy Movchan Date: 10.13.2010
 */
@ApplicationScoped
public class PersonService implements Serializable, IPersonService {

	private static final long serialVersionUID = 6685720517520443399L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private OrganizationService organizationService;

	public void addPersonWithoutCheck(GluuCustomPerson person) {
		person.setCreationDate(new Date());
		persistenceEntryManager.persist(person);
	}

	public void updatePersonWithoutCheck(GluuCustomPerson person) {
		Date updateDate = new Date();
		person.setUpdatedAt(updateDate);
		if (person.getAttribute("oxTrustMetaLastModified") != null) {
			person.setAttribute("oxTrustMetaLastModified", Instant.ofEpochMilli(updateDate.getTime()).toString());
		}
		persistenceEntryManager.merge(person);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#findPersonByDn(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public GluuCustomPerson findPersonByDn(String dn, String... returnAttributes) {
		return persistenceEntryManager.find(dn, GluuCustomPerson.class, returnAttributes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gluu.oxtrust.ldap.service.IPersonService#contains(java.lang.String)
	 */
	@Override
	public boolean contains(String dn) {
		return persistenceEntryManager.contains(dn, GluuCustomPerson.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#getDnForPerson(java.lang.String)
	 */
	@Override
	public String getDnForPerson(String inum) {
		String orgDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=person,%s", orgDn);
		}

		return String.format("inum=%s,ou=people,%s", inum, orgDn);
	}

}