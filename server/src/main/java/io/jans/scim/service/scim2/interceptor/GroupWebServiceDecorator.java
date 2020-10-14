/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.scim2.interceptor;

import java.util.List;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.scim.model.GluuGroup;
import io.jans.scim.model.exception.SCIMException;
import io.jans.scim.model.scim2.ErrorScimType;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.group.GroupResource;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.service.GroupService;
import io.jans.scim.ws.rs.scim2.BaseScimWebService;
import io.jans.scim.ws.rs.scim2.IGroupWebService;

/**
 * Aims at decorating SCIM group service methods. Currently applies validations
 * via ResourceValidator class or other custom validation logic
 *
 * Created by jgomer on 2017-10-18.
 */
@Priority(Interceptor.Priority.APPLICATION)
@Decorator
public class GroupWebServiceDecorator extends BaseScimWebService implements IGroupWebService {

	@Inject
	private Logger log;

	@Inject
	@Delegate
	@Any
	IGroupWebService service;

	@Inject
	private GroupService groupService;

	private Response validateExistenceOfGroup(String id) {

		Response response = null;
		GluuGroup group = StringUtils.isEmpty(id) ? null : groupService.getGroupByInum(id);

		if (group == null) {
			log.info("Group with inum {} not found", id);
			response = getErrorResponse(Response.Status.NOT_FOUND, "Resource " + id + " not found");
		}
		return response;

	}

	private void checkDisplayNameExistence(String displayName) throws DuplicateEntryException {

		boolean flag = false;
		try {
			flag = groupService.getGroupByDisplayName(displayName) != null;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		if (flag)
			throw new DuplicateEntryException("Duplicate group displayName value: " + displayName);

	}

	private void checkDisplayNameExistence(String displayName, String id) throws DuplicateEntryException {
		// Validate if there is an attempt to supply a displayName already in use by a
		// group other than current

		GluuGroup groupToFind = new GluuGroup();
		groupToFind.setDisplayName(displayName);

		List<GluuGroup> list = groupService.findGroups(groupToFind, 2);
		if (list != null) {
			for (GluuGroup g : list)
				if (!g.getInum().equals(id))
					throw new DuplicateEntryException("Duplicate group displayName value: " + displayName);
		}

	}

	public Response createGroup(GroupResource group, String attrsList, String excludedAttrsList) {

		Response response;
		try {
			// empty externalId, no place to store it in LDAP
			group.setExternalId(null);

			executeDefaultValidation(group);
			checkDisplayNameExistence(group.getDisplayName());
			assignMetaInformation(group);
			// Proceed with actual implementation of createGroup method
			response = service.createGroup(group, attrsList, excludedAttrsList);
		} catch (DuplicateEntryException e) {
			log.error(e.getMessage());
			response = getErrorResponse(Response.Status.CONFLICT, ErrorScimType.UNIQUENESS, e.getMessage());
		} catch (SCIMException e) {
			log.error("Validation check at createGroup returned: {}", e.getMessage());
			response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
		}
		return response;

	}

	public Response getGroupById(String id, String attrsList, String excludedAttrsList) {

		Response response = validateExistenceOfGroup(id);
		if (response == null)
			// Proceed with actual implementation of getGroupById method
			response = service.getGroupById(id, attrsList, excludedAttrsList);

		return response;

	}

	public Response updateGroup(GroupResource group, String id, String attrsList, String excludedAttrsList) {

		Response response;
		try {
			// empty externalId, no place to store it in LDAP
			group.setExternalId(null);

			// Check if the ids match in case the group coming has one
			if (group.getId() != null && !group.getId().equals(id))
				throw new SCIMException("Parameter id does not match with id attribute of Group");

			response = validateExistenceOfGroup(id);
			if (response == null) {

				executeValidation(group, true);
				if (StringUtils.isNotEmpty(group.getDisplayName()))
					checkDisplayNameExistence(group.getDisplayName(), id);

				// Proceed with actual implementation of updateGroup method
				response = service.updateGroup(group, id, attrsList, excludedAttrsList);
			}
		} catch (DuplicateEntryException e) {
			log.error(e.getMessage());
			response = getErrorResponse(Response.Status.CONFLICT, ErrorScimType.UNIQUENESS, e.getMessage());
		} catch (SCIMException e) {
			log.error("Validation check at updateGroup returned: {}", e.getMessage());
			response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
		}
		return response;
	}

	public Response deleteGroup(String id) {

		Response response = validateExistenceOfGroup(id);
		if (response == null)
			// Proceed with actual implementation of deleteGroup method
			response = service.deleteGroup(id);

		return response;

	}

	public Response searchGroups(String filter, Integer startIndex, Integer count, String sortBy, String sortOrder,
			String attrsList, String excludedAttrsList) {

		SearchRequest searchReq = new SearchRequest();
		Response response = prepareSearchRequest(searchReq.getSchemas(), filter, sortBy, sortOrder, startIndex, count,
				attrsList, excludedAttrsList, searchReq);

		if (response == null) {
			response = service.searchGroups(searchReq.getFilter(), searchReq.getStartIndex(), searchReq.getCount(),
					searchReq.getSortBy(), searchReq.getSortOrder(), searchReq.getAttributesStr(),
					searchReq.getExcludedAttributesStr());
		}
		return response;

	}

	public Response searchGroupsPost(SearchRequest searchRequest) {

		SearchRequest searchReq = new SearchRequest();
		Response response = prepareSearchRequest(searchRequest.getSchemas(), searchRequest.getFilter(),
				searchRequest.getSortBy(), searchRequest.getSortOrder(), searchRequest.getStartIndex(),
				searchRequest.getCount(), searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr(),
				searchReq);

		if (response == null) {
			response = service.searchGroupsPost(searchReq);
		}
		return response;

	}

	public Response patchGroup(PatchRequest request, String id, String attrsList, String excludedAttrsList) {

		Response response = inspectPatchRequest(request, GroupResource.class);
		if (response == null) {
			response = validateExistenceOfGroup(id);

			if (response == null)
				response = service.patchGroup(request, id, attrsList, excludedAttrsList);
		}
		return response;

	}

}
