package io.jans.scim.ws.rs.scim2;

import static io.jans.scim.model.scim2.Constants.GROUP_OVERHEAD_BYPASS_PARAM;
import static io.jans.scim.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_ATTRIBUTES;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_COUNT;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_EXCLUDED_ATTRS;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_FILTER;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_SORT_BY;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_SORT_ORDER;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_START_INDEX;
import static io.jans.scim.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Predicate;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.management.InvalidAttributeValueException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.scim.model.GluuGroup;
import io.jans.scim.model.exception.SCIMException;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ErrorScimType;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.group.GroupResource;
import io.jans.scim.model.scim2.patch.PatchOperation;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.util.DateUtil;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.service.GroupService;
import io.jans.scim.service.filter.ProtectedApi;
import io.jans.scim.service.scim2.Scim2GroupService;
import io.jans.scim.service.scim2.Scim2PatchService;
import io.jans.scim.service.scim2.interceptor.RefAdjusted;

/**
 * Implementation of /Groups endpoint. Methods here are intercepted.
 * Filter io.jans.scim.service.filter.AuthorizationProcessingFilter secures invocations
 */
@Named("scim2GroupEndpoint")
@Path("/v2/Groups")
public class GroupWebService extends BaseScimWebService implements IGroupWebService {

    @Inject
    private UserWebService userWebService;

    @Inject
    private Scim2GroupService scim2GroupService;

    @Inject
    private GroupService groupService;

    @Inject
    private Scim2PatchService scim2PatchService;
    
    private String usersUrl;
    
    private String groupResourceType;

    private Predicate<String> selectionFilterSkipPredicate;

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
        if (list != null &&
            list.stream().anyMatch(g -> !g.getInum().equals(id))) {
            throw new DuplicateEntryException("Duplicate group displayName value: " + displayName);
        }

    }

    private Response doSearchGroups(String filter, Integer startIndex, Integer count,
            String sortBy, String sortOrder, String attrsList, String excludedAttrsList,
            String method, boolean fillMembersDisplay) {
        
        Response response;
        try {
            SearchRequest searchReq = new SearchRequest();
            response = prepareSearchRequest(searchReq.getSchemas(), filter, sortBy,
                    sortOrder, startIndex, count, attrsList, excludedAttrsList, searchReq);
            if (response != null) return response;

            response = externalConstraintsService.applySearchCheck(searchReq,
                    httpHeaders, uriInfo, method, groupResourceType);
            if (response != null) return response;

            PagedResult<BaseScimResource> resources = scim2GroupService.searchGroups(
                    searchReq.getFilter(), translateSortByAttribute(GroupResource.class, searchReq.getSortBy()), 
                    SortOrder.getByValue(searchReq.getSortOrder()), searchReq.getStartIndex(),
                    searchReq.getCount(), endpointUrl, usersUrl, getMaxCount(), fillMembersDisplay);

            String json = getListResponseSerialized(resources.getTotalEntriesCount(), 
                    searchReq.getStartIndex(), resources.getEntries(), searchReq.getAttributesStr(), 
                    searchReq.getExcludedAttributesStr(), searchReq.getCount() == 0);
            response = Response.ok(json).location(new URI(endpointUrl)).build();
        } catch (SCIMException e){
            log.error(e.getMessage(), e);
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_FILTER,
                    e.getMessage());
        } catch (Exception e){
            log.error("Failure at searchGroups method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
                    "Unexpected error: " + e.getMessage());
        }
        return response;
   
    }

    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/groups.write"})
    @RefAdjusted
    public Response createGroup(
            GroupResource group,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. createGroup");
            
            // empty externalId, no place to store it in LDAP
            group.setExternalId(null);
            executeValidation(group);
            checkDisplayNameExistence(group.getDisplayName());
            assignMetaInformation(group);

            boolean skipValidation = isMembersValidationSkipped();
            boolean displayExcluded = isDisplayExcluded(skipValidation, attrsList, excludedAttrsList);
            GluuGroup gluuGroup = scim2GroupService.preCreateGroup(group, skipValidation,
                    !displayExcluded, usersUrl);

            response = externalConstraintsService.applyEntityCheck(gluuGroup, group,
                    httpHeaders, uriInfo, HttpMethod.POST, groupResourceType);
            if (response != null) return response;

            scim2GroupService.createGroup(gluuGroup, group, !displayExcluded, endpointUrl, usersUrl);            
            String json = resourceSerializer.serialize(group, attrsList, excludedAttrsList);
            response = Response.created(new URI(group.getMeta().getLocation())).entity(json).build();
        } catch (DuplicateEntryException e) {
            log.error(e.getMessage());
            response = getErrorResponse(Response.Status.CONFLICT, ErrorScimType.UNIQUENESS, e.getMessage());
        } catch (SCIMException e) {
            log.error("Validation check at createGroup returned: {}", e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
        } catch (Exception e){
            log.error("Failure at createGroup method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/groups.read"})
    @RefAdjusted
    public Response getGroupById(
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. getGroupById");

            GluuGroup gluuGroup = groupService.getGroupByInum(id);
            if (gluuGroup == null) return notFoundResponse(id, groupResourceType);
            
            response = externalConstraintsService.applyEntityCheck(gluuGroup, null,
                    httpHeaders, uriInfo, HttpMethod.GET, groupResourceType);
            if (response != null) return response;

            boolean displayExcluded = isDisplayExcluded(false, attrsList, excludedAttrsList);
            GroupResource group = scim2GroupService.buildGroupResource(gluuGroup,
                    !displayExcluded, endpointUrl, usersUrl);

            String json = resourceSerializer.serialize(group, attrsList, excludedAttrsList);
            response = Response.ok(new URI(group.getMeta().getLocation())).entity(json).build();
        } catch (Exception e) {
            log.error("Failure at getGroupById method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    /**
     * This implementation differs from spec in the following aspects:
     * - Passing a null value for an attribute, does not modify the attribute in the destination, however passing an
     * empty array for a multivalued attribute does clear the attribute. Thus, to clear single-valued attribute, PATCH
     * operation should be used
     */
    @Path("{id}")
    @PUT
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/groups.write"})
    @RefAdjusted
    public Response updateGroup(
            GroupResource group,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. updateGroup");

            // empty externalId, no place to store it in LDAP
            group.setExternalId(null);

            // Check if the ids match in case the group coming has one
            if (group.getId() != null && !group.getId().equals(id))
                throw new SCIMException("Parameter id does not match with id attribute of Group");

            GluuGroup gluuGroup = groupService.getGroupByInum(id);
            if (gluuGroup == null) return notFoundResponse(id, groupResourceType);

            response = externalConstraintsService.applyEntityCheck(gluuGroup, group,
                    httpHeaders, uriInfo, HttpMethod.PUT, groupResourceType);
            if (response != null) return response;

            executeValidation(group, true);
            if (StringUtils.isNotEmpty(group.getDisplayName())) {
                checkDisplayNameExistence(group.getDisplayName(), id);
            }

            boolean skipValidation = isMembersValidationSkipped();
            boolean displayExcluded = isDisplayExcluded(skipValidation, attrsList, excludedAttrsList);
            GroupResource updatedResource = scim2GroupService.updateGroup(gluuGroup,
                    group, skipValidation, !displayExcluded, endpointUrl, usersUrl);

            String json = resourceSerializer.serialize(updatedResource, attrsList, excludedAttrsList);
            response = Response.ok(new URI(updatedResource.getMeta().getLocation())).entity(json).build();
        } catch (DuplicateEntryException e) {
            log.error(e.getMessage());
            response = getErrorResponse(Response.Status.CONFLICT, ErrorScimType.UNIQUENESS, e.getMessage());
        } catch (SCIMException e) {
            log.error("Validation check at updateGroup returned: {}", e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
        } catch (InvalidAttributeValueException e) {
            log.error(e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.MUTABILITY, e.getMessage());
        } catch (Exception e) {
            log.error("Failure at updateGroup method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @DELETE
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/groups.write"})
    public Response deleteGroup(@PathParam("id") String id) {

        Response response;
        try {
            log.debug("Executing web service method. deleteGroup");

            GluuGroup gluuGroup = groupService.getGroupByInum(id);
            if (gluuGroup == null) return notFoundResponse(id, groupResourceType);

            response = externalConstraintsService.applyEntityCheck(gluuGroup, null,
                    httpHeaders, uriInfo, HttpMethod.DELETE, groupResourceType);
            if (response != null) return response;
            
            scim2GroupService.deleteGroup(gluuGroup);
            response = Response.noContent().build();
        } catch (Exception e){
            log.error("Failure at deleteGroup method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/groups.read"})
    @RefAdjusted
    public Response searchGroups(
            @QueryParam(QUERY_PARAM_FILTER) String filter,
            @QueryParam(QUERY_PARAM_START_INDEX) Integer startIndex,
            @QueryParam(QUERY_PARAM_COUNT) Integer count,
            @QueryParam(QUERY_PARAM_SORT_BY) String sortBy,
            @QueryParam(QUERY_PARAM_SORT_ORDER) String sortOrder,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        log.debug("Executing web service method. searchGroups");
        boolean displayExcluded = isDisplayExcluded(false, attrsList, excludedAttrsList);
        return doSearchGroups(filter, startIndex, count, sortBy, sortOrder, attrsList, 
                excludedAttrsList, HttpMethod.GET, !displayExcluded);

    }

    @Path(SEARCH_SUFFIX)
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/groups.read"})
    @RefAdjusted
    public Response searchGroupsPost(SearchRequest searchRequest) {

        log.debug("Executing web service method. searchGroupsPost");
        boolean displayExcluded = isDisplayExcluded(false, searchRequest.getAttributesStr(),
                searchRequest.getExcludedAttributesStr());

        Response response = doSearchGroups(searchRequest.getFilter(), searchRequest.getStartIndex(), 
                searchRequest.getCount(), searchRequest.getSortBy(), searchRequest.getSortOrder(), 
                searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr(),
                HttpMethod.POST, !displayExcluded);

        URI uri = null;
        try {
            uri = new URI(endpointUrl + "/" + SEARCH_SUFFIX);
        } catch (URISyntaxException e) {
            log.error(e.getMessage(), e);
        }
        return Response.fromResponse(response).location(uri).build();

    }

    @Path("{id}")
    @PATCH
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/groups.write"})
    @RefAdjusted
    public Response patchGroup(
            PatchRequest request,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. patchGroup");

            response = inspectPatchRequest(request, GroupResource.class);
            if (response != null) return response;
            
            GluuGroup gluuGroup = groupService.getGroupByInum(id);			
            if (gluuGroup == null) return notFoundResponse(id, groupResourceType);

            response = externalConstraintsService.applyEntityCheck(gluuGroup, request,
                    httpHeaders, uriInfo, HttpMethod.PATCH, groupResourceType);
            if (response != null) return response;

            boolean skipValidation = isMembersValidationSkipped();
            boolean displayExcluded = isDisplayExcluded(skipValidation, attrsList, excludedAttrsList);

            GroupResource group = new GroupResource();
            //Fill group instance with all info from gluuGroup
            scim2GroupService.transferAttributesToGroupResource(gluuGroup, group,
                !skipValidation, endpointUrl, usersUrl);
            GroupResource original = (GroupResource) ScimResourceUtil.clone(group);

            Predicate<String> p = skipValidation ? selectionFilterSkipPredicate : (filter -> false);
            //Apply patches one by one in sequence
            for (PatchOperation po : request.getOperations()) {
                group = (GroupResource) scim2PatchService.applyPatchOperation(group, po, p);
            }

            log.debug("patchGroup. Revising final resource representation still passes validations");
            //Throws exception if final representation does not pass overall validation
            executeValidation(group);
            checkDisplayNameExistence(group.getDisplayName(), id);

            //Update timestamp
            group.getMeta().setLastModified(DateUtil.millisToISOString(System.currentTimeMillis()));

            if (!displayExcluded) {
                scim2GroupService.restoreMembersDisplay(original, group);
            }

            //Replaces the information found in gluuGroup with the contents of group
            scim2GroupService.replaceGroupInfo(gluuGroup, group, skipValidation, !displayExcluded,
                    endpointUrl, usersUrl);

            String json = resourceSerializer.serialize(group, attrsList, excludedAttrsList);
            response = Response.ok(new URI(group.getMeta().getLocation())).entity(json).build();
        } catch (DuplicateEntryException e) {
            log.error(e.getMessage());
            response = getErrorResponse(Response.Status.CONFLICT, ErrorScimType.UNIQUENESS, e.getMessage());
        } catch (InvalidAttributeValueException e) {
            log.error(e.getMessage(), e);
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.MUTABILITY, e.getMessage());
        } catch (SCIMException e) {
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, e.getMessage());
        } catch (Exception e) {
            log.error("Failure at patchGroup method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }


    private boolean isDisplayExcluded(boolean skipMembersValidation, String include, String exclude) {

        boolean excluded = false;
        if (skipMembersValidation) {
            //In overhead bypass mode, regardless of other params, display is excluded from outputs
            excluded = true;
        } else if (include != null) {
            excluded = !scim2GroupService.membersDisplayInPath(include);
        } else if (exclude != null) {
            excluded = scim2GroupService.membersDisplayInPath(exclude);
        }
        
        if (excluded) {
            log.info("Members display will be ignored");
        }
        return excluded;

    }
    
    private boolean isMembersValidationSkipped() {
        
        boolean overheadBypass = uriInfo.getQueryParameters().getFirst(GROUP_OVERHEAD_BYPASS_PARAM) != null
                || httpHeaders.getHeaderString(GROUP_OVERHEAD_BYPASS_PARAM) != null;
        
        if (overheadBypass) {
            log.info("{} param found", GROUP_OVERHEAD_BYPASS_PARAM);
        }
        return overheadBypass;
        
    }

    @PostConstruct
    public void setup(){
        //Do not use getClass() here...
        init(GroupWebService.class);
        usersUrl = userWebService.getEndpointUrl();
        groupResourceType = ScimResourceUtil.getType(GroupResource.class);

        //An approximate predicate that tries to guess if a SCIM filter contains
        //an expression involving display attribute
        selectionFilterSkipPredicate = filter -> {
            String filth = filter.replaceAll(ScimResourceUtil.getDefaultSchemaUrn(GroupResource.class) + ":", "");
            return filth.matches(".*display\\s++(eq|ne|co|sw|ew|gt|lt|ge|le|pr).*");
        };
    }

}
