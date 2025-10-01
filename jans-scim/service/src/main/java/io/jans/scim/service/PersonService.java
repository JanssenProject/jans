/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;

import io.jans.as.model.common.IdType;
import io.jans.model.JansAttribute;
import io.jans.model.user.SimpleUser;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.scim.exception.DuplicateEmailException;
import io.jans.scim.model.JansCustomAttribute;
import io.jans.scim.model.JansCustomPerson;
import io.jans.scim.model.User;
import io.jans.util.ArrayHelper;
import io.jans.util.OxConstants;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provides operations with persons
 *
 * @author Yuriy Movchan Date: 10.13.2010
 */
@ApplicationScoped
public class PersonService implements Serializable {

	private static final long serialVersionUID = 6685720517520443399L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private AttributeService attributeService;

	@Inject
	private OrganizationService organizationService;

	@Inject
	private ExternalIdGeneratorService idGeneratorService;
	
	private List<JansCustomAttribute> mandatoryAttributes;

	public void addCustomObjectClass(JansCustomPerson person) {
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

	// TODO: Review this methods. We need to check if uid is unique in outside method
	public void addPerson(JansCustomPerson person) throws Exception {
		try {
			List<JansCustomPerson> persons = getPersonsByUid(person.getUid());
			if (persons == null || persons.size() == 0) {
				person.setCreationDate(new Date());
				attributeService.applyMetaData(person.getCustomAttributes());

				ignoreCustomObjectClassesForNonLDAP(person);
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

    private JansCustomPerson ignoreCustomObjectClassesForNonLDAP(JansCustomPerson person) {
        String persistenceType = persistenceEntryManager.getPersistenceType();
        log.debug("persistenceType: {}", persistenceType);
        if (!PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(persistenceType)) {
        	log.debug(
                    "Setting CustomObjectClasses :{} to null as it's used only for LDAP and current persistenceType is {} ",
                    person.getCustomObjectClasses(), persistenceType);
        	person.setCustomObjectClasses(null);
        }

        return person;
    }

	public void updatePerson(JansCustomPerson person) throws Exception {
		try {
			Date updateDate = new Date();
			person.setUpdatedAt(updateDate);
			if (person.getAttribute("jansMetaLastMod") != null) {
				person.setAttribute("jansMetaLastMod", Instant.ofEpochMilli(updateDate.getTime()).toString());
			}
			attributeService.applyMetaData(person.getCustomAttributes());
			persistenceEntryManager.merge(person);
		} catch (Exception e) {
			if (e.getCause().getMessage().contains("unique attribute conflict was detected for attribute mail")) {
				throw new DuplicateEmailException("Email Already Registered");
			} else {
				throw new Exception("Duplicate UID value: " + person.getUid(), e);
			}
		}

	}

	public void removePerson(JansCustomPerson person) {
		persistenceEntryManager.removeRecursively(person.getDn(), JansCustomPerson.class);
	}

	public List<JansCustomPerson> searchPersons(String pattern, int sizeLimit) {
		Filter searchFilter = buildFilter(pattern);
		return persistenceEntryManager.findEntries(getDnForPerson(null), JansCustomPerson.class, searchFilter, sizeLimit);
	}

	public List<JansCustomPerson> searchPersons(String pattern) {
		Filter searchFilter = buildFilter(pattern);
		return persistenceEntryManager.findEntries(getDnForPerson(null), JansCustomPerson.class, searchFilter);
	}

	private Filter buildFilter(String pattern) {
		String[] targetArray = new String[] { pattern };
		Filter uidFilter = Filter.createSubstringFilter(OxConstants.UID, null, targetArray, null);
		Filter mailFilter = Filter.createSubstringFilter("mail", null, targetArray, null);
		Filter nameFilter = Filter.createSubstringFilter("displayName", null, targetArray, null);
		Filter ppidFilter = Filter.createSubstringFilter("jansPPID", null, targetArray, null);
		Filter inumFilter = Filter.createSubstringFilter("inum", null, targetArray, null);
		Filter snFilter = Filter.createSubstringFilter("sn", null, targetArray, null);
		Filter searchFilter = Filter.createORFilter(uidFilter, mailFilter, nameFilter, ppidFilter, inumFilter,
				snFilter);
		return searchFilter;
	}

	public List<JansCustomPerson> findPersons(JansCustomPerson person, int sizeLimit) {
		person.setBaseDn(getDnForPerson(null));
		return persistenceEntryManager.findEntries(person, sizeLimit);
	}

	public List<JansCustomPerson> searchPersons(String pattern, int sizeLimit, List<JansCustomPerson> excludedPersons)
			throws Exception {
		Filter orFilter = buildFilter(pattern);
		Filter searchFilter = orFilter;
		if (excludedPersons != null && excludedPersons.size() > 0) {
			List<Filter> excludeFilters = new ArrayList<Filter>();
			for (JansCustomPerson excludedPerson : excludedPersons) {
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
		return persistenceEntryManager.findEntries(getDnForPerson(null), JansCustomPerson.class, searchFilter, sizeLimit);
	}

	public List<JansCustomPerson> findAllPersons(String[] returnAttributes) {
		return persistenceEntryManager.findEntries(getDnForPerson(null), JansCustomPerson.class, null, returnAttributes);
	}

	public List<JansCustomPerson> findPersonsByUids(List<String> uids, String[] returnAttributes) throws Exception {
		List<Filter> uidFilters = new ArrayList<Filter>();
		for (String uid : uids) {
			uidFilters.add(Filter.createEqualityFilter(OxConstants.UID, uid));
		}
		Filter filter = Filter.createORFilter(uidFilters);
		return persistenceEntryManager.findEntries(getDnForPerson(null), JansCustomPerson.class, filter, returnAttributes);
	}

	public List<JansCustomPerson> findPersonsByMailids(List<String> mailids, String[] returnAttributes)
			throws Exception {
		List<Filter> mailidFilters = new ArrayList<Filter>();
		for (String mailid : mailids) {
			mailidFilters.add(Filter.createEqualityFilter("mail", mailid));
		}
		Filter filter = Filter.createORFilter(mailidFilters);
		return persistenceEntryManager.findEntries(getDnForPerson(null), JansCustomPerson.class, filter, returnAttributes);
	}

	public JansCustomPerson findPersonByDn(String dn, String... returnAttributes) {
		return persistenceEntryManager.find(dn, JansCustomPerson.class, returnAttributes);
	}

	public boolean containsPerson(JansCustomPerson person) {
		boolean result = false;
		try {
			result = persistenceEntryManager.contains(JansCustomPerson.class);
		} catch (Exception e) {
			log.debug(e.getMessage(), e);
		}
		return result;
	}

	public boolean contains(String dn) {
		return persistenceEntryManager.contains(dn, JansCustomPerson.class);
	}

	public JansCustomPerson getPersonByDn(String dn) {
		JansCustomPerson result = persistenceEntryManager.find(JansCustomPerson.class, dn);

		return result;

	}

	public JansCustomPerson getPersonByInum(String inum) {
		JansCustomPerson person = null;
		try {
			person = persistenceEntryManager.find(JansCustomPerson.class, getDnForPerson(inum));
		} catch (Exception e) {
			log.error("Failed to find Person by Inum " + inum, e);
		}
		return person;
	}

	public JansCustomPerson getPersonByUid(String uid, String... returnAttributes) {
		List<JansCustomPerson> entries = getPersonsByUid(uid, returnAttributes);

		if (entries.size() > 0) {
			return entries.get(0);
		} else {
			return null;
		}
	}

	public int countPersons() {
		String dn = getDnForPerson(null);

		Class<?> searchClass = JansCustomPerson.class;
		if (persistenceEntryManager.hasBranchesSupport(dn)) {
			searchClass = SimpleBranch.class;
		}

		return persistenceEntryManager.countEntries(dn, searchClass, null, SearchScope.BASE);
	}

	public String generateInumForNewPerson() {
		JansCustomPerson person = null;
		String newInum = null;
		String newDn = null;
		do {
			newInum = generateInumForNewPersonImpl();
			newDn = getDnForPerson(newInum);
			person = new JansCustomPerson();
			person.setDn(newDn);
		} while (persistenceEntryManager.contains(newDn, JansCustomPerson.class));
		return newInum;
	}

	/**
	 * Generate new inum for person
	 *
	 * @return New inum for person
	 * @throws Exception
	 */
	private String generateInumForNewPersonImpl() {

	    String id = null;
	    if (idGeneratorService.isEnabled()) {
	        id = idGeneratorService.executeExternalGenerateIdMethod(
	            //Use the first enabled script only
	            idGeneratorService.getCustomScriptConfigurations().stream().findFirst().orElse(null)
	            , ""    //appId 
	            , IdType.PEOPLE.getType()    //idType
	            , ""    //idPrefix
            );
	    }
        return id == null ? UUID.randomUUID().toString() : id;

	}

	public String getDnForPerson(String inum) {
		String orgDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=people,%s", orgDn);
		}

		return String.format("inum=%s,ou=people,%s", inum, orgDn);
	}

	public boolean authenticate(String userName, String password) {
		return persistenceEntryManager.authenticate(userName, password);
	}

	public List<JansCustomAttribute> getMandatoryAtributes() {
		if (this.mandatoryAttributes == null) {
			mandatoryAttributes = new ArrayList<JansCustomAttribute>();
			mandatoryAttributes.add(new JansCustomAttribute(OxConstants.UID, "", true, true));
			mandatoryAttributes.add(new JansCustomAttribute("givenName", "", true, true));
			mandatoryAttributes.add(new JansCustomAttribute("displayName", "", true, true));
			mandatoryAttributes.add(new JansCustomAttribute("sn", "", true, true));
			mandatoryAttributes.add(new JansCustomAttribute("mail", "", true, true));
			mandatoryAttributes.add(new JansCustomAttribute("userPassword", "", true, true));
			mandatoryAttributes.add(new JansCustomAttribute("jansStatus", "", true, true));
		}
		return mandatoryAttributes;
	}

	public String getPersonUids(List<JansCustomPerson> persons) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (Iterator<JansCustomPerson> iterator = persons.iterator(); iterator.hasNext();) {
			JansCustomPerson call = iterator.next();
			sb.append('\'').append(call.getUid()).append('\'');
			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	public String getPersonMailids(List<JansCustomPerson> persons) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (Iterator<JansCustomPerson> iterator = persons.iterator(); iterator.hasNext();) {
			JansCustomPerson call = iterator.next();
			sb.append('\'').append(call.getMail()).append('\'');
			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	public List<JansCustomPerson> createEntities(Map<String, List<AttributeData>> entriesAttributes) throws Exception {
		return persistenceEntryManager.createEntities(JansCustomPerson.class, entriesAttributes);
	}

	public JansCustomPerson getPersonByEmail(String mail, String... returnAttributes) {
		List<JansCustomPerson> persons = getPersonsByEmail(mail, returnAttributes);
		if ((persons != null) && (persons.size() > 0)) {
			return persons.get(0);
		}
		return null;
	}

	public List<JansCustomPerson> getPersonsByUid(String uid, String... returnAttributes) {
		log.debug("Getting user information from DB: userId = {}", uid);

		if (StringHelper.isEmpty(uid)) {
			return null;
		}

		Filter userUidFilter = Filter.createEqualityFilter(Filter.createLowercaseFilter(OxConstants.UID), StringHelper.toLowerCase(uid));

		List<JansCustomPerson> entries = persistenceEntryManager.findEntries(getDnForPerson(null), JansCustomPerson.class, userUidFilter, returnAttributes);
		log.debug("Found {} entries for userId = {}", entries.size(), uid);

		return entries;
	}

	public List<JansCustomPerson> getPersonsByEmail(String mail, String... returnAttributes) {
		log.debug("Getting user information from DB: mail = {}", mail);

		if (StringHelper.isEmpty(mail)) {
			return null;
		}

		Filter userMailFilter = Filter.createEqualityFilter(Filter.createLowercaseFilter("mail"), StringHelper.toLowerCase(mail));
		
		boolean multiValued = false;
		JansAttribute mailAttribute = attributeService.getAttributeByName("mail");
		if ((mailAttribute != null) && (mailAttribute.getOxMultiValuedAttribute() != null) && mailAttribute.getOxMultiValuedAttribute()) {
			multiValued = true;
		}
		userMailFilter.multiValued(multiValued);

		List<JansCustomPerson> entries = persistenceEntryManager.findEntries(getDnForPerson(null), JansCustomPerson.class, userMailFilter, returnAttributes);
		log.debug("Found {} entries for mail = {}", entries.size(), mail);

		return entries;
	}

	public JansCustomPerson getPersonByAttribute(String attribute, String value) throws Exception {
		JansCustomPerson person = new JansCustomPerson();
		person.setBaseDn(getDnForPerson(null));
		person.setAttribute(attribute, value);
		List<JansCustomPerson> persons = persistenceEntryManager.findEntries(person);
		if ((persons != null) && (persons.size() > 0)) {
			return persons.get(0);
		}
		return null;
	}

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
	public List<JansCustomPerson> getPersonsByAttribute(String attribute, String value) throws Exception {
		JansCustomPerson person = new JansCustomPerson();
		person.setBaseDn(getDnForPerson(null));
		person.setAttribute(attribute, value);
		List<JansCustomPerson> persons = persistenceEntryManager.findEntries(person);
		if ((persons != null) && (persons.size() > 0)) {
			return persons;
		}
		return null;
	}

}