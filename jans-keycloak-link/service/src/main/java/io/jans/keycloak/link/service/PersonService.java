/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.keycloak.link.service;

import io.jans.link.model.GluuCustomPerson;
import io.jans.link.service.OrganizationService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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

	/**
	 * Generate new inum for person
	 *
	 * @return New inum for person
	 * @throws Exception
	 */
	public String generateInumForNewPersonImpl() {

		String id = null;
		return id == null ? UUID.randomUUID().toString() : id;

	}

	public List<GluuCustomPerson> getPersonsByUid(String uid, String... returnAttributes) {
		log.debug("Getting user information from DB: userId = {}", uid);

		if (StringHelper.isEmpty(uid)) {
			return null;
		}

		String personDn = getDnForPerson(null);
		Filter userUidFilter;

			userUidFilter = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"),
					StringHelper.toLowerCase(uid));


		List<GluuCustomPerson> entries = persistenceEntryManager.findEntries(personDn,
				GluuCustomPerson.class, userUidFilter, returnAttributes);
		log.debug("Found {} entries for userId = {}", entries.size(), uid);

		return entries;
	}

}