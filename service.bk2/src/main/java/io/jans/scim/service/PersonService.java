/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.scim.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.model.GluuAttribute;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.operation.DuplicateEntryException;
import org.gluu.persist.model.AttributeData;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.persist.model.base.SimpleUser;
import org.gluu.search.filter.Filter;
import org.gluu.util.ArrayHelper;
import org.gluu.util.OxConstants;
import org.gluu.util.StringHelper;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;

import io.jans.scim.exception.DuplicateEmailException;
import io.jans.scim.model.GluuCustomAttribute;
import io.jans.scim.model.GluuCustomPerson;
import io.jans.scim.model.User;
import io.jans.scim.util.OxTrustConstants;

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
	private AttributeService attributeService;

	@Inject
	private OrganizationService organizationService;

	private List<GluuCustomAttribute> mandatoryAttributes;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#addCustomObjectClass(org.gluu.
	 * oxtrust.model.GluuCustomPerson)
	 */
	@Override
	public void addCustomObjectClass(GluuCustomPerson person) {
		String customObjectClass = attributeService.getCustomOrigin();
		String[] customObjectClassesArray = person.getCustomObjectClasses();
		if (ArrayHelper.isNotEmpty(customObjectClassesArray)) {
			List<String> customObjectClassesList = Arrays.asList(customObjectClassesArray);
			if (!customObjectClassesList.contains(customObjectClass)) {
				List<String> customObjectClassesListUpdated = new ArrayList<String>();
				customObjectClassesListUpdated.addAll(customObjectClassesList);
				customObjectClassesListUpdated.add(customObjectClass);
				customObjectClassesList = customObjectClassesListUpdated;
			}

			person.setCustomObjectClasses(customObjectClassesList.toArray(new String[0]));

		} else {
			person.setCustomObjectClasses(new String[] { customObjectClass });
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#addPerson(org.gluu.oxtrust.model
	 * .GluuCustomPerson)
	 */
	// TODO: Review this methods. We need to check if uid is unique in outside
	// method
	@Override
	public void addPerson(GluuCustomPerson person) throws Exception {
		try {
			List<GluuCustomPerson> persons = getPersonsByUid(person.getUid());
			if (persons == null || persons.size() == 0) {
				person.setCreationDate(new Date());
				persistenceEntryManager.persist(person);
			} else {
				throw new DuplicateEntryException("Duplicate UID value: " + person.getUid());
			}
		} catch (Exception e) {
			if (e.getCause().getMessage().contains("unique attribute conflict was detected for attribute mail")) {
				throw new DuplicateEmailException("Email Already Registered");
			} else {
				throw new Exception("Duplicate UID value: " + person.getUid());
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#updatePerson(org.gluu.oxtrust.
	 * model.GluuCustomPerson)
	 */
	@Override
	public void updatePerson(GluuCustomPerson person) throws Exception {
		try {
			Date updateDate = new Date();
			person.setUpdatedAt(updateDate);
			if (person.getAttribute("oxTrustMetaLastModified") != null) {
				person.setAttribute("oxTrustMetaLastModified",
						ISODateTimeFormat.dateTime().withZoneUTC().print(updateDate.getTime()));
			}
			persistenceEntryManager.merge(person);
		} catch (Exception e) {
			if (e.getCause().getMessage().contains("unique attribute conflict was detected for attribute mail")) {
				throw new DuplicateEmailException("Email Already Registered");
			} else {
				throw new Exception("Duplicate UID value: " + person.getUid(), e);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#removePerson(org.gluu.oxtrust.
	 * model.GluuCustomPerson)
	 */
	@Override
	public void removePerson(GluuCustomPerson person) {
		persistenceEntryManager.removeRecursively(person.getDn());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#searchPersons(java.lang.String,
	 * int)
	 */
	@Override
	public List<GluuCustomPerson> searchPersons(String pattern, int sizeLimit) {
		Filter searchFilter = buildFilter(pattern);
		return persistenceEntryManager.findEntries(getDnForPerson(null), GluuCustomPerson.class, searchFilter, sizeLimit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#searchPersons(java.lang.String)
	 */
	@Override
	public List<GluuCustomPerson> searchPersons(String pattern) {
		Filter searchFilter = buildFilter(pattern);
		return persistenceEntryManager.findEntries(getDnForPerson(null), GluuCustomPerson.class, searchFilter);
	}

	private Filter buildFilter(String pattern) {
		String[] targetArray = new String[] { pattern };
		Filter uidFilter = Filter.createSubstringFilter(OxConstants.UID, null, targetArray, null);
		Filter mailFilter = Filter.createSubstringFilter(OxTrustConstants.mail, null, targetArray, null);
		Filter nameFilter = Filter.createSubstringFilter(OxTrustConstants.displayName, null, targetArray, null);
		Filter ppidFilter = Filter.createSubstringFilter(OxTrustConstants.ppid, null, targetArray, null);
		Filter inumFilter = Filter.createSubstringFilter(OxTrustConstants.inum, null, targetArray, null);
		Filter snFilter = Filter.createSubstringFilter(OxTrustConstants.sn, null, targetArray, null);
		Filter searchFilter = Filter.createORFilter(uidFilter, mailFilter, nameFilter, ppidFilter, inumFilter,
				snFilter);
		return searchFilter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#findPersons(org.gluu.oxtrust.
	 * model.GluuCustomPerson, int)
	 */
	@Override
	public List<GluuCustomPerson> findPersons(GluuCustomPerson person, int sizeLimit) {
		person.setBaseDn(getDnForPerson(null));
		return persistenceEntryManager.findEntries(person, sizeLimit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#searchPersons(java.lang.String,
	 * int, java.util.List)
	 */
	@Override
	public List<GluuCustomPerson> searchPersons(String pattern, int sizeLimit, List<GluuCustomPerson> excludedPersons)
			throws Exception {
		Filter orFilter = buildFilter(pattern);
		Filter searchFilter = orFilter;
		if (excludedPersons != null && excludedPersons.size() > 0) {
			List<Filter> excludeFilters = new ArrayList<Filter>();
			for (GluuCustomPerson excludedPerson : excludedPersons) {
				Filter eqFilter = Filter.createEqualityFilter(OxConstants.UID, excludedPerson.getUid());
				excludeFilters.add(eqFilter);
			}
			Filter orExcludeFilter = null;
			if (excludedPersons.size() == 1) {
				orExcludeFilter = excludeFilters.get(0);
			} else {
				orExcludeFilter = Filter.createORFilter(excludeFilters);
			}
			Filter notFilter = Filter.createNOTFilter(orExcludeFilter);
			searchFilter = Filter.createANDFilter(orFilter, notFilter);
		}
		return persistenceEntryManager.findEntries(getDnForPerson(null), GluuCustomPerson.class, searchFilter, sizeLimit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#findAllPersons(java.lang.String[
	 * ])
	 */
	@Override
	public List<GluuCustomPerson> findAllPersons(String[] returnAttributes) {
		return persistenceEntryManager.findEntries(getDnForPerson(null), GluuCustomPerson.class, null, returnAttributes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#findPersonsByUids(java.util.
	 * List, java.lang.String[])
	 */
	@Override
	public List<GluuCustomPerson> findPersonsByUids(List<String> uids, String[] returnAttributes) throws Exception {
		List<Filter> uidFilters = new ArrayList<Filter>();
		for (String uid : uids) {
			uidFilters.add(Filter.createEqualityFilter(OxConstants.UID, uid));
		}
		Filter filter = Filter.createORFilter(uidFilters);
		return persistenceEntryManager.findEntries(getDnForPerson(null), GluuCustomPerson.class, filter, returnAttributes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#findPersonsByMailds(java.util.
	 * List, java.lang.String[])
	 */
	@Override
	public List<GluuCustomPerson> findPersonsByMailids(List<String> mailids, String[] returnAttributes)
			throws Exception {
		List<Filter> mailidFilters = new ArrayList<Filter>();
		for (String mailid : mailids) {
			mailidFilters.add(Filter.createEqualityFilter(OxTrustConstants.mail, mailid));
		}
		Filter filter = Filter.createORFilter(mailidFilters);
		return persistenceEntryManager.findEntries(getDnForPerson(null), GluuCustomPerson.class, filter, returnAttributes);
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
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#containsPerson(org.gluu.oxtrust.
	 * model.GluuCustomPerson)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public boolean containsPerson(GluuCustomPerson person) {
		boolean result = false;
		try {
			result = persistenceEntryManager.contains(GluuCustomPerson.class);
		} catch (Exception e) {
			log.debug(e.getMessage(), e);
		}
		return result;
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
	 * org.gluu.oxtrust.ldap.service.IPersonService#getPersonByDn(java.lang.String)
	 */
	@Override
	public GluuCustomPerson getPersonByDn(String dn) {
		GluuCustomPerson result = persistenceEntryManager.find(GluuCustomPerson.class, dn);

		return result;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gluu.oxtrust.ldap.service.IPersonService#getPersonByInum(java.lang.
	 * String)
	 */
	@Override
	public GluuCustomPerson getPersonByInum(String inum) {
		GluuCustomPerson person = null;
		try {
			person = persistenceEntryManager.find(GluuCustomPerson.class, getDnForPerson(inum));
		} catch (Exception e) {
			log.error("Failed to find Person by Inum " + inum, e);
		}
		return person;
	}

	public GluuCustomPerson getPersonByUid(String uid, String... returnAttributes) {
		List<GluuCustomPerson> entries = getPersonsByUid(uid, returnAttributes);

		if (entries.size() > 0) {
			return entries.get(0);
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gluu.oxtrust.ldap.service.IPersonService#countPersons()
	 */
	@Override
	public int countPersons() {
		String dn = getDnForPerson(null);

		Class<?> searchClass = GluuCustomPerson.class;
		if (persistenceEntryManager.hasBranchesSupport(dn)) {
			searchClass = SimpleBranch.class;
		}

		return persistenceEntryManager.countEntries(dn, searchClass, null, SearchScope.BASE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gluu.oxtrust.ldap.service.IPersonService#generateInumForNewPerson()
	 */
	@Override
	public String generateInumForNewPerson() {
		GluuCustomPerson person = null;
		String newInum = null;
		String newDn = null;
		do {
			newInum = generateInumForNewPersonImpl();
			newDn = getDnForPerson(newInum);
			person = new GluuCustomPerson();
			person.setDn(newDn);
		} while (persistenceEntryManager.contains(newDn, GluuCustomPerson.class));
		return newInum;
	}

	/**
	 * Generate new inum for person
	 *
	 * @return New inum for person
	 * @throws Exception
	 */
	private String generateInumForNewPersonImpl() {
		return UUID.randomUUID().toString();
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
			return String.format("ou=people,%s", orgDn);
		}

		return String.format("inum=%s,ou=people,%s", inum, orgDn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#authenticate(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public boolean authenticate(String userName, String password) {
		return persistenceEntryManager.authenticate(userName, password);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gluu.oxtrust.ldap.service.IPersonService#getMandatoryAtributes()
	 */
	@Override
	public List<GluuCustomAttribute> getMandatoryAtributes() {
		if (this.mandatoryAttributes == null) {
			mandatoryAttributes = new ArrayList<GluuCustomAttribute>();
			mandatoryAttributes.add(new GluuCustomAttribute(OxConstants.UID, "", true, true));
			mandatoryAttributes.add(new GluuCustomAttribute("givenName", "", true, true));
			mandatoryAttributes.add(new GluuCustomAttribute("displayName", "", true, true));
			mandatoryAttributes.add(new GluuCustomAttribute("sn", "", true, true));
			mandatoryAttributes.add(new GluuCustomAttribute("mail", "", true, true));
			mandatoryAttributes.add(new GluuCustomAttribute("userPassword", "", true, true));
			mandatoryAttributes.add(new GluuCustomAttribute("gluuStatus", "", true, true));
		}
		return mandatoryAttributes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#getPersonUid(java.util.List)
	 */
	@Override
	public String getPersonUids(List<GluuCustomPerson> persons) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (Iterator<GluuCustomPerson> iterator = persons.iterator(); iterator.hasNext();) {
			GluuCustomPerson call = iterator.next();
			sb.append('\'').append(call.getUid()).append('\'');
			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#getPersonMailid(java.util.List)
	 */
	@Override
	public String getPersonMailids(List<GluuCustomPerson> persons) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (Iterator<GluuCustomPerson> iterator = persons.iterator(); iterator.hasNext();) {
			GluuCustomPerson call = iterator.next();
			sb.append('\'').append(call.getMail()).append('\'');
			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#createEntities(java.util.Map)
	 */
	@Override
	public List<GluuCustomPerson> createEntities(Map<String, List<AttributeData>> entriesAttributes) throws Exception {
		return persistenceEntryManager.createEntities(GluuCustomPerson.class, entriesAttributes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gluu.oxtrust.ldap.service.IPersonService#getPersonByEmail(java.lang.
	 * String)
	 */
	@Override
	public GluuCustomPerson getPersonByEmail(String mail, String... returnAttributes) {
		List<GluuCustomPerson> persons = getPersonsByEmail(mail, returnAttributes);
		if ((persons != null) && (persons.size() > 0)) {
			return persons.get(0);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gluu.oxtrust.ldap.service.IPersonService#getPersonsByUid(java.lang.
	 * String)
	 */
	@Override
	public List<GluuCustomPerson> getPersonsByUid(String uid, String... returnAttributes) {
		log.debug("Getting user information from DB: userId = {}", uid);

		if (StringHelper.isEmpty(uid)) {
			return null;
		}

		Filter userUidFilter = Filter.createEqualityFilter(Filter.createLowercaseFilter(OxConstants.UID), StringHelper.toLowerCase(uid));

		List<GluuCustomPerson> entries = persistenceEntryManager.findEntries(getDnForPerson(null), GluuCustomPerson.class, userUidFilter, returnAttributes);
		log.debug("Found {} entries for userId = {}", entries.size(), uid);

		return entries;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#getPersonsByEmail(java.lang.
	 * String)
	 */
	@Override
	public List<GluuCustomPerson> getPersonsByEmail(String mail, String... returnAttributes) {
		log.debug("Getting user information from DB: mail = {}", mail);

		if (StringHelper.isEmpty(mail)) {
			return null;
		}

		Filter userMailFilter = Filter.createEqualityFilter(Filter.createLowercaseFilter("mail"), StringHelper.toLowerCase(mail));
		
		boolean multiValued = false;
		GluuAttribute mailAttribute = attributeService.getAttributeByName("mail");
		if ((mailAttribute != null) && (mailAttribute.getOxMultiValuedAttribute() != null) && mailAttribute.getOxMultiValuedAttribute()) {
			multiValued = true;
		}
		userMailFilter.multiValued(multiValued);

		List<GluuCustomPerson> entries = persistenceEntryManager.findEntries(getDnForPerson(null), GluuCustomPerson.class, userMailFilter, returnAttributes);
		log.debug("Found {} entries for mail = {}", entries.size(), mail);

		return entries;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#getPersonByAttribute(java.lang.
	 * String, java.lang.String)
	 */
	@Override
	public GluuCustomPerson getPersonByAttribute(String attribute, String value) throws Exception {
		GluuCustomPerson person = new GluuCustomPerson();
		person.setBaseDn(getDnForPerson(null));
		person.setAttribute(attribute, value);
		List<GluuCustomPerson> persons = persistenceEntryManager.findEntries(person);
		if ((persons != null) && (persons.size() > 0)) {
			return persons.get(0);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#getUserByUid(java.lang.String)
	 */
	@Override
	public User getUserByUid(String uid) {
		Filter userUidFilter = Filter.createEqualityFilter(Filter.createLowercaseFilter(OxConstants.UID), StringHelper.toLowerCase(uid));
		Filter userObjectClassFilter = Filter.createEqualityFilter(OxConstants.OBJECT_CLASS, "gluuPerson");
		Filter filter = Filter.createANDFilter(userObjectClassFilter, userUidFilter);

		List<SimpleUser> users = persistenceEntryManager.findEntries(getDnForPerson(null), SimpleUser.class, filter, 1);
		if ((users != null) && (users.size() > 0)) {
			return persistenceEntryManager.find(User.class, users.get(0).getDn());
		}
		return null;
	}

	/**
	 * Get list of persons by attribute
	 *
	 * @param attribute
	 *            attribute
	 * @param value
	 *            value
	 * @return List <Person>
	 */
	@Override
	public List<GluuCustomPerson> getPersonsByAttribute(String attribute, String value) throws Exception {
		GluuCustomPerson person = new GluuCustomPerson();
		person.setBaseDn(getDnForPerson(null));
		person.setAttribute(attribute, value);
		List<GluuCustomPerson> persons = persistenceEntryManager.findEntries(person);
		if ((persons != null) && (persons.size() > 0)) {
			return persons;
		}
		return null;
	}

}