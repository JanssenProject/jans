package io.jans.scim.ws.rs.scim2;

import static io.jans.scim.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_ATTRIBUTES;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_COUNT;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_EXCLUDED_ATTRS;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_FILTER;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_SORT_BY;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_SORT_ORDER;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_START_INDEX;
import static io.jans.scim.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;
import static io.jans.scim.model.scim2.patch.PatchOperationType.REMOVE;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

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

import io.jans.scim.model.GluuCustomPerson;
import io.jans.scim.model.exception.SCIMException;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ErrorScimType;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.patch.PatchOperation;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.model.scim2.util.DateUtil;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.service.filter.ProtectedApi;
import io.jans.scim.model.scim.ScimCustomPerson;
import io.jans.scim.service.scim2.Scim2PatchService;
import io.jans.scim.service.scim2.Scim2UserService;
import io.jans.scim.service.scim2.interceptor.RefAdjusted;
import io.jans.scim.ws.rs.scim2.IUserWebService;
import io.jans.scim.ws.rs.scim2.PATCH;

/**
 * Implementation of /Users endpoint. Methods here are intercepted.
 * Filter io.jans.scim.service.filter.AuthorizationProcessingFilter secures invocations
 */
@Named
@Path("/v2/Users")
public class UserWebService extends BaseScimWebService implements IUserWebService {

    @Inject
    private Scim2UserService scim2UserService;

    @Inject
    private Scim2PatchService scim2PatchService;

    private String userResourceType;

    private void checkUidExistence(String uid) throws DuplicateEntryException {
        if (personService.getPersonByUid(uid) != null) {
            throw new DuplicateEntryException("Duplicate UID value: " + uid);
        }
    }
    
    private void checkUidExistence(String uid, String id) throws DuplicateEntryException {

        // Validate if there is an attempt to supply a userName already in use by a user other than current
        List<GluuCustomPerson> list = null;
        try {
            list = personService.findPersonsByUids(Collections.singletonList(uid), new String[]{"inum"});
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        if (list != null &&
            list.stream().filter(p -> !p.getInum().equals(id)).findAny().isPresent()) {
            throw new DuplicateEntryException("Duplicate UID value: " + uid);
        }

    }

    private Response doSearch(String filter, Integer startIndex, Integer count, String sortBy,
           String sortOrder, String attrsList, String excludedAttrsList, String method) {

        Response response;
        try {            
            SearchRequest searchReq = new SearchRequest();
            response = prepareSearchRequest(searchReq.getSchemas(), filter, sortBy,
                    sortOrder, startIndex, count, attrsList, excludedAttrsList, searchReq);
            if (response != null) return response;

            response = externalConstraintsService.applySearchCheck(searchReq,
                    httpHeaders, uriInfo, method, userResourceType);
            if (response != null) return response;

            PagedResult<BaseScimResource> resources = scim2UserService.searchUsers(
                    searchReq.getFilter(), translateSortByAttribute(UserResource.class, searchReq.getSortBy()),
                    SortOrder.getByValue(searchReq.getSortOrder()), searchReq.getStartIndex(), 
                    searchReq.getCount(), endpointUrl, getMaxCount());

            String json = getListResponseSerialized(resources.getTotalEntriesCount(), 
                    searchReq.getStartIndex(), resources.getEntries(), searchReq.getAttributesStr(),
                    searchReq.getExcludedAttributesStr(), searchReq.getCount() == 0);
            response = Response.ok(json).location(new URI(endpointUrl)).build();
        } catch (SCIMException e) {
            log.error(e.getMessage(), e);
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_FILTER, 
                    e.getMessage());
        } catch (Exception e) {
            log.error("Failure at searchUsers method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                    "Unexpected error: " + e.getMessage());
        }
        return response;
        
    }

    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/users.write"})
    @RefAdjusted
    public Response createUser(
            UserResource user,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. createUser");

            executeValidation(user);
            if (StringUtils.isEmpty(user.getUserName())) throw new SCIMException("Empty username not allowed");

            checkUidExistence(user.getUserName());
            assignMetaInformation(user);
            ScimResourceUtil.adjustPrimarySubAttributes(user);

            ScimCustomPerson person = scim2UserService.preCreateUser(user);
            response = externalConstraintsService.applyEntityCheck(person, user,
                    httpHeaders, uriInfo, HttpMethod.POST, userResourceType);
            if (response != null) return response;

            scim2UserService.createUser(person, user, endpointUrl);
            String json = resourceSerializer.serialize(user, attrsList, excludedAttrsList);
            response = Response.created(new URI(user.getMeta().getLocation())).entity(json).build();
        } catch (DuplicateEntryException e) {
            log.error(e.getMessage());
            response = getErrorResponse(Response.Status.CONFLICT, ErrorScimType.UNIQUENESS, e.getMessage());
        } catch (SCIMException e) {
            log.error("Validation check at createUser returned: {}", e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
        } catch (Exception e) {
            log.error("Failure at createUser method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/users.read"})
    @RefAdjusted
    public Response getUserById(
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. getUserById");

            ScimCustomPerson person = userPersistenceHelper.getPersonByInum(id);
            if (person == null) return notFoundResponse(id, userResourceType);

            response = externalConstraintsService.applyEntityCheck(person, null,
                    httpHeaders, uriInfo, HttpMethod.GET, userResourceType);
            if (response != null) return response;

            UserResource user = scim2UserService.buildUserResource(person, endpointUrl);
            String json = resourceSerializer.serialize(user, attrsList, excludedAttrsList);
            response = Response.ok(new URI(user.getMeta().getLocation())).entity(json).build();
        } catch (Exception e) {
            log.error("Failure at getUserById method", e);
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
    @ProtectedApi(scopes = {"https://jans.io/scim/users.write"})
    @RefAdjusted
    public Response updateUser(
            UserResource user,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. updateUser");

            //Check if the ids match in case the user coming has one
            if (user.getId() != null && !user.getId().equals(id))
                throw new SCIMException("Parameter id does not match with id attribute of User");

            ScimCustomPerson person = userPersistenceHelper.getPersonByInum(id);
            if (person == null) return notFoundResponse(id, userResourceType);

            response = externalConstraintsService.applyEntityCheck(person, user,
                    httpHeaders, uriInfo, HttpMethod.PUT, userResourceType);
            if (response != null) return response;

            executeValidation(user, true);
            if (StringUtils.isNotEmpty(user.getUserName())) {
                checkUidExistence(user.getUserName(), id);
            }

            ScimResourceUtil.adjustPrimarySubAttributes(user);
            UserResource updatedResource = scim2UserService.updateUser(person, user, endpointUrl);
            String json = resourceSerializer.serialize(updatedResource, attrsList, excludedAttrsList);
            response = Response.ok(new URI(updatedResource.getMeta().getLocation())).entity(json).build();

        } catch (DuplicateEntryException e) {
            log.error(e.getMessage());
            response = getErrorResponse(Response.Status.CONFLICT, ErrorScimType.UNIQUENESS, e.getMessage());
        } catch (SCIMException e) {
            log.error("Validation check at updateUser returned: {}", e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
        } catch (InvalidAttributeValueException e) {
            log.error(e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.MUTABILITY, e.getMessage());
        } catch (Exception e) {
            log.error("Failure at updateUser method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @DELETE
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/users.write"})
    public Response deleteUser(@PathParam("id") String id) {

        Response response;
        try {
            log.debug("Executing web service method. deleteUser");

            ScimCustomPerson person = userPersistenceHelper.getPersonByInum(id);
            if (person == null) return notFoundResponse(id, userResourceType);

            response = externalConstraintsService.applyEntityCheck(person, null,
                    httpHeaders, uriInfo, HttpMethod.DELETE, userResourceType);
            if (response != null) return response;
            
            scim2UserService.deleteUser(person);
            response = Response.noContent().build();
        } catch (Exception e) {
            log.error("Failure at deleteUser method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/users.read"})
    @RefAdjusted
    public Response searchUsers(
            @QueryParam(QUERY_PARAM_FILTER) String filter,
            @QueryParam(QUERY_PARAM_START_INDEX) Integer startIndex,
            @QueryParam(QUERY_PARAM_COUNT) Integer count,
            @QueryParam(QUERY_PARAM_SORT_BY) String sortBy,
            @QueryParam(QUERY_PARAM_SORT_ORDER) String sortOrder,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        log.debug("Executing web service method. searchUsers");
        return doSearch(filter, startIndex, count, sortBy, sortOrder, attrsList, 
                excludedAttrsList, HttpMethod.GET);

    }

    @Path(SEARCH_SUFFIX)
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/users.read"})
    @RefAdjusted
    public Response searchUsersPost(SearchRequest searchRequest){

        log.debug("Executing web service method. searchUsersPost");
        Response response = doSearch(searchRequest.getFilter(), searchRequest.getStartIndex(), 
                searchRequest.getCount(), searchRequest.getSortBy(), searchRequest.getSortOrder(), 
                searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr(), HttpMethod.POST);

        URI uri = null;
        try {
            uri = new URI(endpointUrl + "/" + SEARCH_SUFFIX);
        } catch (URISyntaxException e){
            log.error(e.getMessage(), e);
        }
        return Response.fromResponse(response).location(uri).build();

    }

    @Path("{id}")
    @PATCH
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/users.write"})
    @RefAdjusted
    public Response patchUser(
            PatchRequest request,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try{
            log.debug("Executing web service method. patchUser");
            
            response = inspectPatchRequest(request, UserResource.class);
            if (response != null) return response;
            
            ScimCustomPerson person = userPersistenceHelper.getPersonByInum(id);
            if (person == null) return notFoundResponse(id, userResourceType);

            response = externalConstraintsService.applyEntityCheck(person, request,
                    httpHeaders, uriInfo, HttpMethod.PATCH, userResourceType);
            if (response != null) return response;

            UserResource user=new UserResource();
            //Fill user instance with all info from person
            scim2UserService.transferAttributesToUserResource(person, user, endpointUrl);

            //Apply patches one by one in sequence
            for (PatchOperation po : request.getOperations()) {
                //Handle special case: https://github.com/GluuFederation/oxTrust/issues/800
                if (po.getType().equals(REMOVE) && po.getPath().equals("pairwiseIdentifiers")){
                    //If this block weren't here, the implementation will throw error because read-only attribute cannot be altered
                    person.setPpid(null);
                    user.setPairwiseIdentifiers(null);
                    scim2UserService.removePPIDsBranch(person.getDn());
                } else {
                    user = (UserResource) scim2PatchService.applyPatchOperation(user, po);
                }
            }

            //Throws exception if final representation does not pass overall validation
            log.debug("patchUser. Revising final resource representation still passes validations");
            executeValidation(user);
            ScimResourceUtil.adjustPrimarySubAttributes(user);

            //Update timestamp
            user.getMeta().setLastModified(DateUtil.millisToISOString(System.currentTimeMillis()));

            //Replaces the information found in person with the contents of user
            scim2UserService.replacePersonInfo(person, user, endpointUrl);

            String json = resourceSerializer.serialize(user, attrsList, excludedAttrsList);
            response = Response.ok(new URI(user.getMeta().getLocation())).entity(json).build();
        } catch (InvalidAttributeValueException e) {
            log.error(e.getMessage(), e);
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.MUTABILITY, e.getMessage());
        } catch (SCIMException e) {
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, e.getMessage());
        } catch (Exception e) {
            log.error("Failure at patchUser method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @PostConstruct
    public void setup() {
        //Do not use getClass() here...
        init(UserWebService.class);
        userResourceType = ScimResourceUtil.getType(UserResource.class);
    }

}
