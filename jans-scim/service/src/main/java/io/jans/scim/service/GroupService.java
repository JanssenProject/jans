/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service;

import io.jans.as.model.common.IdType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.*;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.orm.search.filter.Filter;
import io.jans.scim.model.JansCustomPerson;
import io.jans.scim.model.JansGroup;
import io.jans.util.ArrayHelper;
import io.jans.util.StringHelper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;

/**
 * Provides operations with groups
 * 
 * @author Yuriy Movchan Date: 11.02.2010
 */
@ApplicationScoped
public class GroupService implements Serializable {

	private static final long serialVersionUID = -9167587377957719152L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private OrganizationService organizationService;

	@Inject
	private PersonService personService;

	@Inject
	private ExternalIdGeneratorService idGeneratorService;

	public void addGroup(JansGroup group) throws Exception {
		JansGroup displayNameGroup = new JansGroup();
		displayNameGroup.setDisplayName(group.getDisplayName());
		List<JansGroup> groups = findGroups(displayNameGroup, 1);
		if (groups == null || groups.size() == 0) {
			persistenceEntryManager.persist(group);
		} else {
			throw new DuplicateEntryException("Duplicate displayName: " + group.getDisplayName());
		}
	}

	public void updateGroup(JansGroup group) {
		persistenceEntryManager.merge(group);
	}

	public void removeGroup(JansGroup group) {
		if (group.getMembers() != null) {
			List<String> memberDNs = group.getMembers();
			for (String memberDN : memberDNs) {
				if (personService.contains(memberDN)) {
					JansCustomPerson person = personService.getPersonByDn(memberDN);
					List<String> groupDNs = person.getMemberOf();
					List<String> updatedGroupDNs = new ArrayList<String>();
					updatedGroupDNs.addAll(groupDNs);
					updatedGroupDNs.remove(group.getDn());
					person.setMemberOf(updatedGroupDNs);
					try {
						personService.updatePerson(person);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		persistenceEntryManager.remove(group);
		// clear references in gluuPerson entries
	}

	public List<JansGroup> getAllGroups() {
		return persistenceEntryManager.findEntries(getDnForGroup(null), JansGroup.class, null);
	}

	public boolean isMemberOrOwner(String groupDN, String personDN) {
		Filter ownerFilter = Filter.createEqualityFilter("owner", personDN);
		Filter memberFilter = Filter.createEqualityFilter("member", personDN);
		Filter searchFilter = Filter.createORFilter(ownerFilter, memberFilter);

		boolean isMemberOrOwner = false;
		try {
			isMemberOrOwner = persistenceEntryManager.findEntries(groupDN, JansGroup.class, searchFilter, 1).size() > 0;
		} catch (EntryPersistenceException ex) {
			log.error("Failed to determine if person '{}' memeber or owner of group '{}'", personDN, groupDN, ex);
		}

		return isMemberOrOwner;
	}

	public JansGroup getGroupByInum(String inum) throws Exception {
		JansGroup result = null;
		try {
			result = persistenceEntryManager.find(JansGroup.class, getDnForGroup(inum));
		} catch (Exception e) {
            if (!SearchException.class.isInstance(e.getCause())) {
                log.error(e.getMessage(), e.getCause());
                throw e;
            }
            log.debug("Failed to find group by Inum {}", inum);
		}
		return result;

	}

	public String getDnForGroup(String inum) {
		String orgDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=groups,%s", orgDn);
		}

		return String.format("inum=%s,ou=groups,%s", inum, orgDn);
	}

	public int countGroups() {
		String dn = getDnForGroup(null);

		Class<?> searchClass = JansGroup.class;
		if (persistenceEntryManager.hasBranchesSupport(dn)) {
			searchClass = SimpleBranch.class;
		}

		return persistenceEntryManager.countEntries(dn, searchClass, null, SearchScope.BASE);
	}

	public boolean contains(String groupDn) {
		return persistenceEntryManager.contains(groupDn, JansCustomPerson.class);
	}

	public String generateInumForNewGroup() throws Exception {
		JansGroup group = new JansGroup();
		String newInum = null;
		String newDn = null;
		do {
			newInum = generateInumForNewGroupImpl();
			newDn = getDnForGroup(newInum);
			group.setDn(newDn);
		} while (persistenceEntryManager.contains(newDn, JansCustomPerson.class));

		return newInum;
	}

	public List<JansGroup> searchGroups(String pattern, int sizeLimit) throws Exception {
		String[] targetArray = new String[] { pattern };
		Filter displayNameFilter = Filter.createSubstringFilter("displayName", null, targetArray, null);
		Filter descriptionFilter = Filter.createSubstringFilter("description", null, targetArray, null);
		Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter);
		return persistenceEntryManager.findEntries(getDnForGroup(null), JansGroup.class, searchFilter, sizeLimit);
	}

	public List<JansGroup> getAllGroups(int sizeLimit) {
		return persistenceEntryManager.findEntries(getDnForGroup(null), JansGroup.class, null, sizeLimit);
	}

	/**
	 * Generate new inum for group
	 * 
	 * @return New inum for group
	 */
	private String generateInumForNewGroupImpl() throws Exception {
	    
	    String id = null;
	    if (idGeneratorService.isEnabled()) {
	        id = idGeneratorService.executeExternalGenerateIdMethod(
	            //Use the first enabled script only
	            idGeneratorService.getCustomScriptConfigurations().stream().findFirst().orElse(null)
	            , ""    //appId 
	            , IdType.GROUP.getType()    //idType
	            , ""    //idPrefix
            );
	    }
        return id == null ? UUID.randomUUID().toString() : id;

	}

	public JansGroup getGroupByDn(String Dn) {
		return persistenceEntryManager.find(JansGroup.class, Dn);
	}

	public JansGroup getGroupByDisplayName(String DisplayName) throws Exception {
		JansGroup group = new JansGroup();
		group.setBaseDn(getDnForGroup(null));
		group.setDisplayName(DisplayName);
		List<JansGroup> groups = persistenceEntryManager.findEntries(group);
		if ((groups != null) && (groups.size() > 0)) {
			return groups.get(0);
		}
		return null;
	}

	/**
	 * Search groups by attributes present in object
	 *
	 * @param group
	 * @param sizeLimit
	 * @return
	 */
	public List<JansGroup> findGroups(JansGroup group, int sizeLimit) {
		group.setBaseDn(getDnForGroup(null));
		return persistenceEntryManager.findEntries(group, sizeLimit);
	}

	public boolean isMemberOrOwner(String[] groupDNs, String personDN) throws Exception {
		boolean result = false;
		if (ArrayHelper.isEmpty(groupDNs)) {
			return result;
		}

		for (String groupDN : groupDNs) {
			if (StringHelper.isEmpty(groupDN)) {
				continue;
			}

			result = isMemberOrOwner(groupDN, personDN);
			if (result) {
				break;
			}
		}

		return result;
	}

}
