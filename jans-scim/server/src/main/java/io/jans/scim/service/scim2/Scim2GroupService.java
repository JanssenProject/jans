/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.scim2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

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

@ApplicationScoped
public class Scim2GroupService implements Serializable {

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

    @Inject
    private UserPersistenceHelper userPersistenceHelper;

    /**
     * Takes two GroupResource objects and attempts to fill the members' display names
     * in the second object when missing based on the data existing in the first object.
     * In practice the first object represents an already stored group while the
     * second is the result of modifications applied upon the first. In the course 
     * of modifications some display names may have removed. This method tries to
     * recover some of this lost data
     * @param trusted Object containing valid group data
     * @param altered Modified object 
     */
    public void restoreMembersDisplay(GroupResource trusted, GroupResource altered) {

        int aSize = membersCount(altered);
        int tSize = membersCount(trusted);
        if (aSize > 0 && tSize > 0) {

            Map<String, String> map = trusted.getMembers().stream().filter(m -> m.getDisplay() != null)
                    .collect(Collectors.toMap(Member::getValue, Member::getDisplay));

            for (Member member : altered.getMembers()) {
                String inum = member.getValue();
                if (member.getDisplay() == null) {
                    member.setDisplay(map.get(inum));
                }
            }
        }
        
    }

    private void transferAttributesToGroup(GroupResource res, GluuGroup group,
            boolean skipMembersValidation, boolean fillMembersDisplay, String usersUrl) {

        // externalId (so jansExtId) not part of LDAP schema
        group.setAttribute("jansMetaCreated", res.getMeta().getCreated());
        group.setAttribute("jansMetaLastMod", res.getMeta().getLastModified());
        // When creating group, location will be set again when having an inum
        group.setAttribute("jansMetaLocation", res.getMeta().getLocation());

        group.setDisplayName(res.getDisplayName());
        group.setStatus(GluuStatus.ACTIVE);
        group.setOrganization(organizationService.getDnForOrganization());

        Set<Member> members = res.getMembers();
        if (members != null && members.size() > 0) {

            Set<String> groupMembers = group.getMembers().stream()
                    .map(userPersistenceHelper::getUserInumFromDN).collect(
                            Collectors.toCollection(HashSet::new));

            List<String> listMembers = new ArrayList<>();
            List<Member> invalidMembers = new ArrayList<>();

            // Add the members, and complement the $refs and users' display names in res
            for (Member member : members) {
                GluuCustomPerson person;
                // it's not null as it is required in GroupResource
                String inum = member.getValue();

                //Added users via POST/PUT/PATCH might not exist
                //so data is not considered trusty. In this case
                //we make database lookups
                if (!skipMembersValidation && !groupMembers.contains(inum)) {
                    person = personService.getPersonByInum(inum);
                    
                    if (person != null && fillMembersDisplay) {
                        member.setDisplay(person.getDisplayName());
                    }
                } else {
                    person = new GluuCustomPerson();
                    person.setDn(personService.getDnForPerson(inum));
                }

                if (person == null) {
                    log.info("Member identified by {} does not exist. Ignored", inum);
                    invalidMembers.add(member);
                } else {
                    member.setRef(usersUrl + "/" + inum);
                    member.setType(ScimResourceUtil.getType(UserResource.class));
                    
                    if (skipMembersValidation) {
                        //In overhead bypass mode, display names must not be returned
                        member.setDisplay(null);
                    }

                    listMembers.add(person.getDn());
                }
            }
            group.setMembers(listMembers);

            members.removeAll(invalidMembers);
            if (members.isEmpty()) {
                res.setMembers(null);
            }                        
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

    public void transferAttributesToGroupResource(GluuGroup gluuGroup, GroupResource res,
            boolean fillMembersDisplay, String groupsUrl, String usersUrl) {

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

                if (fillMembersDisplay) {
                    try {                                    
                        person = personService.getPersonByDn(dn);
                    } catch (Exception e) {
                        log.warn("Wrong member entry {} found in group {}",
                                dn, gluuGroup.getDisplayName());
                    }
                }
                
                if (person == null) {
                    person = new GluuCustomPerson();
                    person.setInum(userPersistenceHelper.getUserInumFromDN(dn));
                }
                
                Member aMember = new Member();
                aMember.setValue(person.getInum());
                aMember.setRef(usersUrl + "/" + person.getInum());
                aMember.setType(ScimResourceUtil.getType(UserResource.class));
                aMember.setDisplay(person.getDisplayName());

                members.add(aMember);
            }
            res.setMembers(members);
        }

    }

    public GluuGroup preCreateGroup(GroupResource group, boolean skipMembersValidation,
            boolean fillDisplay, String usersUrl) throws Exception {

        log.info("Preparing to create group {}", group.getDisplayName());

        GluuGroup gluuGroup = new GluuGroup();
        transferAttributesToGroup(group, gluuGroup, skipMembersValidation, fillDisplay,
                usersUrl);
        assignComputedAttributesToGroup(gluuGroup);

        return gluuGroup;
        
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
	public void createGroup(GluuGroup gluuGroup, GroupResource group, boolean fillMembersDisplay,
                String groupsUrl, String usersUrl) throws Exception {

		String location = groupsUrl + "/" + gluuGroup.getInum();
		gluuGroup.setAttribute("jansMetaLocation", location);

		log.info("Persisting group {}", group.getDisplayName());

		if (externalScimService.isEnabled()) {
			boolean result = externalScimService.executeScimCreateGroupMethods(gluuGroup);
			if (!result) {
				throw new WebApplicationException("Failed to execute SCIM script successfully",
						Status.PRECONDITION_FAILED);
			}
			groupService.addGroup(gluuGroup);
			syncMemberAttributeInPerson(gluuGroup.getDn(), Collections.emptySet(),
                                memberIDsSet(gluuGroup));

			// Copy back to group the info from gluuGroup
			transferAttributesToGroupResource(gluuGroup, group, fillMembersDisplay,
                            groupsUrl, usersUrl);
			externalScimService.executeScimPostCreateGroupMethods(gluuGroup);
		} else {
			groupService.addGroup(gluuGroup);
			group.getMeta().setLocation(location);
			// We are ignoring the id value received (group.getId())
			group.setId(gluuGroup.getInum());
			syncMemberAttributeInPerson(gluuGroup.getDn(), Collections.emptySet(),
                                memberIDsSet(gluuGroup));
		}

	}

    public GroupResource buildGroupResource(GluuGroup gluuGroup, boolean fillMembersDisplay,
            String endpointUrl, String usersUrl) {

        GroupResource group = new GroupResource();
        if (externalScimService.isEnabled() && !externalScimService.executeScimGetGroupMethods(gluuGroup)) {
            throw new WebApplicationException("Failed to execute SCIM script successfully",
                    Status.PRECONDITION_FAILED);
        }
        transferAttributesToGroupResource(gluuGroup, group, fillMembersDisplay, endpointUrl, usersUrl);
        
        return group;

    }

    public GroupResource updateGroup(GluuGroup gluuGroup, GroupResource group,
                boolean skipMembersValidation, boolean fillMembersDisplay, String groupsUrl,
                String usersUrl) throws Exception {

        GroupResource tmpGroup = new GroupResource();
        transferAttributesToGroupResource(gluuGroup, tmpGroup, !skipMembersValidation,
                        groupsUrl, usersUrl);

        GroupResource res = (GroupResource) ScimResourceUtil.transferToResourceReplace(
                        group, tmpGroup, extService.getResourceExtensions(group.getClass()));
        res.getMeta().setLastModified(DateUtil.millisToISOString(System.currentTimeMillis()));
                
        if (fillMembersDisplay) {
            restoreMembersDisplay(tmpGroup, res);
        }

        replaceGroupInfo(gluuGroup, res, skipMembersValidation, fillMembersDisplay,
                        groupsUrl, usersUrl);

        return res;

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

    public void replaceGroupInfo(GluuGroup gluuGroup, GroupResource group,
                boolean skipMembersValidation, boolean fillMembersDisplay, String groupsUrl,
                String usersUrl) throws Exception {

        Set<String> olderMembers = memberIDsSet(gluuGroup);
        transferAttributesToGroup(group, gluuGroup, skipMembersValidation,
                fillMembersDisplay, usersUrl);
        log.debug("replaceGroupInfo. Updating group info in LDAP");

        if (externalScimService.isEnabled()) {
            boolean result = externalScimService.executeScimUpdateGroupMethods(gluuGroup);
            if (!result) {
                throw new WebApplicationException("Failed to execute SCIM script successfully",
                        Status.PRECONDITION_FAILED);
            }

            groupService.updateGroup(gluuGroup);
            syncMemberAttributeInPerson(gluuGroup.getDn(), olderMembers,
                    memberIDsSet(gluuGroup));

            // Copy back to user the info from gluuGroup
            transferAttributesToGroupResource(gluuGroup, group, fillMembersDisplay,
                    groupsUrl, usersUrl);
            externalScimService.executeScimPostUpdateGroupMethods(gluuGroup);
        } else {
            groupService.updateGroup(gluuGroup);
            syncMemberAttributeInPerson(gluuGroup.getDn(), olderMembers,
                    memberIDsSet(gluuGroup));
        }

    }

	public PagedResult<BaseScimResource> searchGroups(String filter, String sortBy, SortOrder sortOrder, int startIndex,
			int count, String groupsUrl, String usersUrl, int maxCount, boolean fillMembersDisplay) throws Exception {

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
			GroupResource scimGroup = new GroupResource();
			transferAttributesToGroupResource(group, scimGroup, fillMembersDisplay,
                    groupsUrl, usersUrl);
			resources.add(scimGroup);
		}
		log.info("Found {} matching entries - returning {}", list.getTotalEntriesCount(), list.getEntries().size());

		PagedResult<BaseScimResource> result = new PagedResult<>();
		result.setEntries(resources);
		result.setTotalEntriesCount(list.getTotalEntriesCount());

		return result;

	}

    public boolean membersDisplayInPath(String strPath) {
        
        List<String> paths = Arrays.asList(strPath.replaceAll("\\s", "").split(","));
        String prefix = ScimResourceUtil.getDefaultSchemaUrn(GroupResource.class) + ":";
        String parent = "members";
        String path = parent + ".display";
        
        return Stream.of(parent, path, prefix + parent, prefix + path).anyMatch(paths::contains);

    }

	private void syncMemberAttributeInPerson(String groupDn, Set<String> before,
			Set<String> after) {

		log.debug("syncMemberAttributeInPerson. Updating memberOf attribute in user LDAP entries");
		log.trace("Before member dns {}; After member dns {}", before, after);

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
					log.error("An error occurred while removing group {} from user {}", groupDn, dn);
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
					log.error("An error occurred while adding group {} to user {}", groupDn, dn);
					log.error(e.getMessage(), e);
				}
			}
		}

	}

    private static Set<String> memberIDsSet(GluuGroup gluuGroup) {
        return Optional.ofNullable(gluuGroup.getMembers()).orElse(Collections.emptyList())
                .stream().collect(Collectors.toCollection(HashSet::new));
    }

    private int membersCount(GroupResource res) {
        return Optional.ofNullable(res.getMembers()).map(Set::size).orElse(0);
    }

}
