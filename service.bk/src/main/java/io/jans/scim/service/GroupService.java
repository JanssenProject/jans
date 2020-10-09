/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.scim.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.EntryPersistenceException;
import org.gluu.persist.exception.operation.DuplicateEntryException;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.search.filter.Filter;
import org.gluu.util.ArrayHelper;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import io.jans.scim.model.GluuCustomPerson;
import io.jans.scim.model.GluuGroup;
import io.jans.scim.model.GluuGroupVisibility;
import io.jans.scim.util.OxTrustConstants;

/**
 * Provides operations with groups
 * 
 * @author Yuriy Movchan Date: 11.02.2010
 */
@ApplicationScoped
public class GroupService implements Serializable, IGroupService {

	private static final long serialVersionUID = -9167587377957719152L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private OrganizationService organizationService;

	@Inject
	private PersonService personService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IGroupService#addGroup(org.gluu.oxtrust.model.
	 * GluuGroup)
	 */
	@Override
	public void addGroup(GluuGroup group) throws Exception {
		GluuGroup displayNameGroup = new GluuGroup();
		displayNameGroup.setDisplayName(group.getDisplayName());
		List<GluuGroup> groups = findGroups(displayNameGroup, 1);
		if (groups == null || groups.size() == 0) {
			persistenceEntryManager.persist(group);
		} else {
			throw new DuplicateEntryException("Duplicate displayName: " + group.getDisplayName());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IGroupService#updateGroup(org.gluu.oxtrust.
	 * model.GluuGroup)
	 */
	@Override
	public void updateGroup(GluuGroup group) {
		persistenceEntryManager.merge(group);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IGroupService#removeGroup(org.gluu.oxtrust.
	 * model.GluuGroup)
	 */
	@Override
	public void removeGroup(GluuGroup group) {
		if (group.getMembers() != null) {
			List<String> memberDNs = group.getMembers();
			for (String memberDN : memberDNs) {
				if (personService.contains(memberDN)) {
					GluuCustomPerson person = personService.getPersonByDn(memberDN);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gluu.oxtrust.ldap.service.IGroupService#getAllGroups()
	 */
	@Override
	public List<GluuGroup> getAllGroups() {
		return persistenceEntryManager.findEntries(getDnForGroup(null), GluuGroup.class, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IGroupService#isMemberOrOwner(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public boolean isMemberOrOwner(String groupDN, String personDN) {
		Filter ownerFilter = Filter.createEqualityFilter(OxTrustConstants.owner, personDN);
		Filter memberFilter = Filter.createEqualityFilter(OxTrustConstants.member, personDN);
		Filter searchFilter = Filter.createORFilter(ownerFilter, memberFilter);

		boolean isMemberOrOwner = false;
		try {
			isMemberOrOwner = persistenceEntryManager.findEntries(groupDN, GluuGroup.class, searchFilter, 1).size() > 0;
		} catch (EntryPersistenceException ex) {
			log.error("Failed to determine if person '{}' memeber or owner of group '{}'", personDN, groupDN, ex);
		}

		return isMemberOrOwner;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IGroupService#getGroupByInum(java.lang.String)
	 */
	@Override
	public GluuGroup getGroupByInum(String inum) {
		GluuGroup result = null;
		try {
			result = persistenceEntryManager.find(GluuGroup.class, getDnForGroup(inum));
		} catch (Exception e) {
			log.error("Failed to find group by Inum " + inum, e);
		}
		return result;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IGroupService#getDnForGroup(java.lang.String)
	 */
	@Override
	public String getDnForGroup(String inum) {
		String orgDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=groups,%s", orgDn);
		}

		return String.format("inum=%s,ou=groups,%s", inum, orgDn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gluu.oxtrust.ldap.service.IGroupService#countGroups()
	 */
	@Override
	public int countGroups() {
		String dn = getDnForGroup(null);

		Class<?> searchClass = GluuGroup.class;
		if (persistenceEntryManager.hasBranchesSupport(dn)) {
			searchClass = SimpleBranch.class;
		}

		return persistenceEntryManager.countEntries(dn, searchClass, null, SearchScope.BASE);
	}

	public boolean contains(String groupDn) {
		return persistenceEntryManager.contains(groupDn, GluuCustomPerson.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gluu.oxtrust.ldap.service.IGroupService#generateInumForNewGroup()
	 */
	@Override
	public String generateInumForNewGroup() throws Exception {
		GluuGroup group = new GluuGroup();
		String newInum = null;
		String newDn = null;
		do {
			newInum = generateInumForNewGroupImpl();
			newDn = getDnForGroup(newInum);
			group.setDn(newDn);
		} while (persistenceEntryManager.contains(newDn, GluuCustomPerson.class));

		return newInum;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IGroupService#searchGroups(java.lang.String,
	 * int)
	 */
	@Override
	public List<GluuGroup> searchGroups(String pattern, int sizeLimit) throws Exception {
		String[] targetArray = new String[] { pattern };
		Filter displayNameFilter = Filter.createSubstringFilter(OxTrustConstants.displayName, null, targetArray, null);
		Filter descriptionFilter = Filter.createSubstringFilter(OxTrustConstants.description, null, targetArray, null);
		Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter);
		return persistenceEntryManager.findEntries(getDnForGroup(null), GluuGroup.class, searchFilter, sizeLimit);
	}

	@Override
	public List<GluuGroup> getAllGroups(int sizeLimit) {
		return persistenceEntryManager.findEntries(getDnForGroup(null), GluuGroup.class, null, sizeLimit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gluu.oxtrust.ldap.service.IGroupService#getVisibilityTypes()
	 */
	@Override
	public GluuGroupVisibility[] getVisibilityTypes() {
		return GluuGroupVisibility.values();
	}

	/**
	 * Generate new inum for group
	 * 
	 * @return New inum for group
	 */
	private String generateInumForNewGroupImpl() throws Exception {
		return UUID.randomUUID().toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IGroupService#getGroupByDn(java.lang.String)
	 */

	@Override
	public GluuGroup getGroupByDn(String Dn) {
		return persistenceEntryManager.find(GluuGroup.class, Dn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IGroupService#getGroupByDisplayName(java.lang.
	 * String)
	 */
	@Override
	public GluuGroup getGroupByDisplayName(String DisplayName) throws Exception {
		GluuGroup group = new GluuGroup();
		group.setBaseDn(getDnForGroup(null));
		group.setDisplayName(DisplayName);
		List<GluuGroup> groups = persistenceEntryManager.findEntries(group);
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
	@Override
	public List<GluuGroup> findGroups(GluuGroup group, int sizeLimit) {
		group.setBaseDn(getDnForGroup(null));
		return persistenceEntryManager.findEntries(group, sizeLimit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gluu.oxtrust.ldap.service.IPersonService#isMemberOrOwner(java.lang.String
	 * [], java.lang.String)
	 */
	@Override
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
