/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.scim2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;

import io.jans.model.GluuStatus;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.scim.model.GluuCustomPerson;
import io.jans.scim.model.GluuGroup;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.Meta;
import io.jans.scim.model.scim2.group.GroupResource;
import io.jans.scim.model.scim2.group.Member;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.model.scim2.util.DateUtil;
import io.jans.scim.service.GroupService;
import io.jans.scim.service.OrganizationService;
import io.jans.scim.service.PersonService;
import io.jans.scim.service.antlr.scimFilter.ScimFilterParserService;
import io.jans.scim.service.external.ExternalScimService;

/**
 * @author Val Pecaoco Re-engineered by jgomer on 2017-10-18.
 */
@ApplicationScoped
public class Scim2GroupService implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1555887165477267426L;

	@Inject
	private Logger log;

	@Inject
	private PersonService personService;

	@Inject
	private GroupService groupService;

	@Inject
	private ExternalScimService externalScimService;

	@Inject
	private OrganizationService organizationService;

	@Inject
	private ExtensionService extService;

	@Inject
	private ScimFilterParserService scimFilterParserService;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	private void transferAttributesToGroup(GroupResource res, GluuGroup group, String usersUrl) {

		// externalId (so jansExtId) not part of LDAP schema
		group.setAttribute("jansMetaCreated", res.getMeta().getCreated());
		group.setAttribute("jansMetaLastMod", res.getMeta().getLastModified());
		// When creating group, location will be set again when having an inum
		group.setAttribute("jansMetaLocation", res.getMeta().getLocation());

		group.setDisplayName(res.getDisplayName());
		group.setStatus(GluuStatus.ACTIVE);
		group.setOrganization(organizationService.getDnForOrganization());

		// Add the members, and complement the $refs and users' display names in res
		Set<Member> members = res.getMembers();
		if (members != null && members.size() > 0) {
			List<String> listMembers = new ArrayList<>();
			List<Member> invalidMembers = new ArrayList<>();

			for (Member member : members) {
				String inum = member.getValue(); // it's not null as it is required in GroupResource
				GluuCustomPerson person = personService.getPersonByInum(inum);

				if (person == null) {
					log.info("Member identified by {} does not exist. Ignored", inum);
					invalidMembers.add(member);
				} else {
					member.setDisplay(person.getDisplayName());
					member.setRef(usersUrl + "/" + inum);
					member.setType(ScimResourceUtil.getType(UserResource.class));

					listMembers.add(person.getDn());
				}
			}
			group.setMembers(listMembers);

			members.removeAll(invalidMembers);
			members = members.size() == 0 ? null : members;
			res.setMembers(members);
		} else {
			group.setMembers(new ArrayList<>());
		}
	}

	private void assignComputedAttributesToGroup(GluuGroup gluuGroup) throws Exception {

		String inum = groupService.generateInumForNewGroup();
		String dn = groupService.getDnForGroup(inum);

		gluuGroup.setInum(inum);
		gluuGroup.setDn(dn);
	}

	public void transferAttributesToGroupResource(GluuGroup gluuGroup, GroupResource res, String groupsUrl,
			String usersUrl) {

		res.setId(gluuGroup.getInum());

		Meta meta = new Meta();
		meta.setResourceType(ScimResourceUtil.getType(res.getClass()));
		meta.setCreated(gluuGroup.getAttribute("jansMetaCreated"));
		meta.setLastModified(gluuGroup.getAttribute("jansMetaLastMod"));
		meta.setLocation(gluuGroup.getAttribute("jansMetaLocation"));
		if (meta.getLocation() == null)
			meta.setLocation(groupsUrl + "/" + gluuGroup.getInum());

		res.setMeta(meta);
		res.setDisplayName(gluuGroup.getDisplayName());

		// Transfer members from GluuGroup to GroupResource
		List<String> memberDNs = gluuGroup.getMembers();
		if (memberDNs != null) {
			Set<Member> members = new HashSet<>();

			for (String dn : memberDNs) {
				GluuCustomPerson person = null;
				try {
					person = personService.getPersonByDn(dn);
				} catch (Exception e) {
					log.warn("Wrong member entry {} found in group {}", dn, gluuGroup.getDisplayName());
				}
				if (person != null) {
					Member aMember = new Member();
					aMember.setValue(person.getInum());
					aMember.setRef(usersUrl + "/" + person.getInum());
					aMember.setType(ScimResourceUtil.getType(UserResource.class));
					aMember.setDisplay(person.getDisplayName());

					members.add(aMember);
				}
			}
			res.setMembers(members);
		}
	}

	/**
	 * Inserts a new group in LDAP based on the SCIM Resource passed There is no
	 * need to check attributes mutability in this case as there are no original
	 * attributes (the resource does not exist yet)
	 * 
	 * @param group
	 *            A GroupResource object with all info as received by the web
	 *            service
	 * @param groupsUrl Base URL associated to group resources in SCIM (eg. .../scim/v2/Groups)
	 * @param usersUrl Base URL associated to user resources in SCIM (eg. .../scim/v2/Users)
	 * @throws Exception In case of unexpected error
	 */
	public void createGroup(GroupResource group, String groupsUrl, String usersUrl) throws Exception {

		String groupName = group.getDisplayName();
		log.info("Preparing to create group {}", groupName);

		GluuGroup gluuGroup = new GluuGroup();
		transferAttributesToGroup(group, gluuGroup, usersUrl);
		assignComputedAttributesToGroup(gluuGroup);

		String location = groupsUrl + "/" + gluuGroup.getInum();
		gluuGroup.setAttribute("jansMetaLocation", location);

		log.info("Persisting group {}", groupName);

		if (externalScimService.isEnabled()) {
			boolean result = externalScimService.executeScimCreateGroupMethods(gluuGroup);
			if (!result) {
				throw new WebApplicationException("Failed to execute SCIM script successfully",
						Status.PRECONDITION_FAILED);
			}
			groupService.addGroup(gluuGroup);
			syncMemberAttributeInPerson(gluuGroup.getDn(), null, gluuGroup.getMembers());

			// Copy back to group the info from gluuGroup
			transferAttributesToGroupResource(gluuGroup, group, groupsUrl, usersUrl);
			externalScimService.executeScimPostCreateGroupMethods(gluuGroup);
		} else {
			groupService.addGroup(gluuGroup);
			group.getMeta().setLocation(location);
			// We are ignoring the id value received (group.getId())
			group.setId(gluuGroup.getInum());
			syncMemberAttributeInPerson(gluuGroup.getDn(), null, gluuGroup.getMembers());
		}

	}

	public GroupResource updateGroup(String id, GroupResource group, String groupsUrl, String usersUrl)
			throws Exception {

		GluuGroup gluuGroup = groupService.getGroupByInum(id); // This is never null (see decorator involved)
		GroupResource tmpGroup = new GroupResource();
		transferAttributesToGroupResource(gluuGroup, tmpGroup, groupsUrl, usersUrl);

		tmpGroup.getMeta().setLastModified(DateUtil.millisToISOString(System.currentTimeMillis()));

		tmpGroup = (GroupResource) ScimResourceUtil.transferToResourceReplace(group, tmpGroup,
				extService.getResourceExtensions(group.getClass()));
		replaceGroupInfo(gluuGroup, tmpGroup, groupsUrl, usersUrl);

		return tmpGroup;

	}

	public void deleteGroup(GluuGroup gluuGroup) throws Exception {
		log.info("Removing group and updating user's entries");

		if (externalScimService.isEnabled()) {
			boolean result = externalScimService.executeScimDeleteGroupMethods(gluuGroup);
			if (!result) {
				throw new WebApplicationException("Failed to execute SCIM script successfully",
						Status.PRECONDITION_FAILED);
			}
		}

		groupService.removeGroup(gluuGroup);

		if (externalScimService.isEnabled())
			externalScimService.executeScimPostDeleteGroupMethods(gluuGroup);

	}

	public void replaceGroupInfo(GluuGroup gluuGroup, GroupResource group, String groupsUrl, String usersUrl)
			throws Exception {

		List<String> olderMembers = new ArrayList<>();
		if (gluuGroup.getMembers() != null)
			olderMembers.addAll(gluuGroup.getMembers());

		transferAttributesToGroup(group, gluuGroup, usersUrl);
		log.debug("replaceGroupInfo. Updating group info in LDAP");

		if (externalScimService.isEnabled()) {
			boolean result = externalScimService.executeScimUpdateGroupMethods(gluuGroup);
			if (!result) {
				throw new WebApplicationException("Failed to execute SCIM script successfully",
						Status.PRECONDITION_FAILED);
			}

			groupService.updateGroup(gluuGroup);
			syncMemberAttributeInPerson(gluuGroup.getDn(), olderMembers, gluuGroup.getMembers());

			// Copy back to user the info from gluuGroup
			transferAttributesToGroupResource(gluuGroup, group, groupsUrl, usersUrl);
			externalScimService.executeScimPostUpdateGroupMethods(gluuGroup);
		} else {
			groupService.updateGroup(gluuGroup);
			syncMemberAttributeInPerson(gluuGroup.getDn(), olderMembers, gluuGroup.getMembers());
		}

	}

	public PagedResult<BaseScimResource> searchGroups(String filter, String sortBy, SortOrder sortOrder, int startIndex,
			int count, String groupsUrl, String usersUrl, int maxCount) throws Exception {

		Filter ldapFilter = scimFilterParserService.createFilter(filter, Filter.createPresenceFilter("inum"), GroupResource.class);
		log.info("Executing search for groups using: ldapfilter '{}', sortBy '{}', sortOrder '{}', startIndex '{}', count '{}'",
				ldapFilter.toString(), sortBy, sortOrder.getValue(), startIndex, count);

		PagedResult<GluuGroup> list = ldapEntryManager.findPagedEntries(groupService.getDnForGroup(null),
				GluuGroup.class, ldapFilter, null, sortBy, sortOrder, startIndex - 1, count, maxCount);
		List<BaseScimResource> resources = new ArrayList<>();

		if (externalScimService.isEnabled() && !externalScimService.executeScimPostSearchGroupsMethods(list)) {
			throw new WebApplicationException("Failed to execute SCIM script successfully", Status.PRECONDITION_FAILED);
		}

		for (GluuGroup group : list.getEntries()) {
			GroupResource jsScimGrp = new GroupResource();
			transferAttributesToGroupResource(group, jsScimGrp, groupsUrl, usersUrl);
			resources.add(jsScimGrp);
		}
		log.info("Found {} matching entries - returning {}", list.getTotalEntriesCount(), list.getEntries().size());

		PagedResult<BaseScimResource> result = new PagedResult<>();
		result.setEntries(resources);
		result.setTotalEntriesCount(list.getTotalEntriesCount());

		return result;

	}

	private void syncMemberAttributeInPerson(String groupDn, List<String> beforeMemberDns,
			List<String> afterMemberDns) {

		log.debug("syncMemberAttributeInPerson. Updating memberOf attribute in user LDAP entries");
		log.trace("Before member dns {}; After member dns {}", beforeMemberDns, afterMemberDns);

		// Build 2 sets of DNs
		Set<String> before = new HashSet<>();
		if (beforeMemberDns != null)
			before.addAll(beforeMemberDns);

		Set<String> after = new HashSet<>();
		if (afterMemberDns != null)
			after.addAll(afterMemberDns);

		// Do removals
		for (String dn : before) {
			if (!after.contains(dn)) {
				try {
					GluuCustomPerson gluuPerson = personService.getPersonByDn(dn);

					List<String> memberOf = new ArrayList<>();
					memberOf.addAll(gluuPerson.getMemberOf());
					memberOf.remove(groupDn);

					gluuPerson.setMemberOf(memberOf);
					personService.updatePerson(gluuPerson);
				} catch (Exception e) {
					log.error("An error occurred while removing user {} from group {}", dn, groupDn);
					log.error(e.getMessage(), e);
				}
			}
		}

		// Do insertions
		for (String dn : after) {
			if (!before.contains(dn)) {
				try {
					GluuCustomPerson gluuPerson = personService.getPersonByDn(dn);

					List<String> memberOf = new ArrayList<>();
					memberOf.add(groupDn);

					if (gluuPerson.getMemberOf() != null)
						memberOf.addAll(gluuPerson.getMemberOf());

					gluuPerson.setMemberOf(memberOf);
					personService.updatePerson(gluuPerson);
				} catch (Exception e) {
					log.error("An error occurred while adding user {} to group {}", dn, groupDn);
					log.error(e.getMessage(), e);
				}
			}
		}

	}

}
