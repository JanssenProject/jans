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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.management.InvalidAttributeValueException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.jans.scim.model.exception.SCIMException;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ErrorScimType;
import io.jans.scim.model.scim2.ListResponse;
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
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;

/**
 * Implementation of /Users endpoint. Methods here are intercepted and/or decorated.
 * Class org.gluu.oxtrust.service.scim2.interceptor.UserWebServiceDecorator is used to apply pre-validations on data.
 * Filter org.gluu.oxtrust.filter.AuthorizationProcessingFilter secures invocations
 *
 * @author Rahat Ali Date: 05.08.2015
 * Updated by jgomer on 2017-09-12.
 */
@Named
@Path("/v2/Users")
@Api(value = "/v2/Users", description = "SCIM 2.0 User Endpoint (https://tools.ietf.org/html/rfc7644#section-3.2)",
        authorizations = {@Authorization(value = "Authorization", type = "uma")})
public class UserWebService extends BaseScimWebService implements IUserWebService {

    @Inject
    private Scim2UserService scim2UserService;

    @Inject
    private Scim2PatchService scim2PatchService;

    /**
     *
     */
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/users.write"})
    @RefAdjusted
    @ApiOperation(value = "Create user", notes = "https://tools.ietf.org/html/rfc7644#section-3.3", response = UserResource.class)
    public Response createUser(
            @ApiParam(value = "User", required = true) UserResource user,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList){

        Response response;
        try {
            log.debug("Executing web service method. createUser");
            scim2UserService.createUser(user, endpointUrl);
            String json=resourceSerializer.serialize(user, attrsList, excludedAttrsList);
            response=Response.created(new URI(user.getMeta().getLocation())).entity(json).build();
        }
        catch (Exception e){
            log.error("Failure at createUser method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/users.read"})
    @RefAdjusted
    @ApiOperation(value = "Find user by id", notes = "Returns a user by id as path param (https://tools.ietf.org/html/rfc7644#section-3.4.1)",
            response = UserResource.class)
    public Response getUserById(
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. getUserById");
            UserResource user = new UserResource();
            ScimCustomPerson person = userPersistenceHelper.getPersonByInum(id);  //person is not null (check associated decorator method)

            if (externalScimService.isEnabled() && !externalScimService.executeScimGetUserMethods(person)) {
                throw new WebApplicationException("Failed to execute SCIM script successfully",
                        Response.Status.PRECONDITION_FAILED);
            }
            scim2UserService.transferAttributesToUserResource(person, user, endpointUrl);

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
    @ApiOperation(value = "Update user", notes = "Update user (https://tools.ietf.org/html/rfc7644#section-3.5.1)", response = UserResource.class)
    public Response updateUser(
            @ApiParam(value = "User", required = true) UserResource user,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. updateUser");
            UserResource updatedResource=scim2UserService.updateUser(id, user, endpointUrl);
            String json=resourceSerializer.serialize(updatedResource, attrsList, excludedAttrsList);
            response=Response.ok(new URI(updatedResource.getMeta().getLocation())).entity(json).build();
        }
        catch (InvalidAttributeValueException e){
            log.error(e.getMessage());
            response=getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.MUTABILITY, e.getMessage());
        }
        catch (Exception e){
            log.error("Failure at updateUser method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @DELETE
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/users.write"})
    @ApiOperation(value = "Delete User", notes = "Delete User (https://tools.ietf.org/html/rfc7644#section-3.6)")
    public Response deleteUser(@PathParam("id") String id){

        Response response;
        try {
            log.debug("Executing web service method. deleteUser");
            ScimCustomPerson person=userPersistenceHelper.getPersonByInum(id);  //person cannot be null (check associated decorator method)
            scim2UserService.deleteUser(person);
            response=Response.noContent().build();
        }
        catch (Exception e){
            log.error("Failure at deleteUser method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/users.read"})
    @RefAdjusted
    @ApiOperation(value = "Search users", notes = "Returns a list of users (https://tools.ietf.org/html/rfc7644#section-3.4.2.2)", response = ListResponse.class)
    public Response searchUsers(
            @QueryParam(QUERY_PARAM_FILTER) String filter,
            @QueryParam(QUERY_PARAM_START_INDEX) Integer startIndex,
            @QueryParam(QUERY_PARAM_COUNT) Integer count,
            @QueryParam(QUERY_PARAM_SORT_BY) String sortBy,
            @QueryParam(QUERY_PARAM_SORT_ORDER) String sortOrder,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList){

        Response response;
        try {
            log.debug("Executing web service method. searchUsers");
            sortBy=translateSortByAttribute(UserResource.class, sortBy);
            PagedResult<BaseScimResource> resources = scim2UserService.searchUsers(filter, sortBy, SortOrder.getByValue(sortOrder),
                    startIndex, count, endpointUrl, getMaxCount());

            String json = getListResponseSerialized(resources.getTotalEntriesCount(), startIndex, resources.getEntries(), attrsList, excludedAttrsList, count==0);
            response=Response.ok(json).location(new URI(endpointUrl)).build();
        }
        catch (SCIMException e){
            log.error(e.getMessage(), e);
            response=getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_FILTER, e.getMessage());
        }
        catch (Exception e){
            log.error("Failure at searchUsers method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path(SEARCH_SUFFIX)
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/users.read"})
    @RefAdjusted
    @ApiOperation(value = "Search users POST /.search", notes = "Returns a list of users (https://tools.ietf.org/html/rfc7644#section-3.4.3)", response = ListResponse.class)
    public Response searchUsersPost(@ApiParam(value = "SearchRequest", required = true) SearchRequest searchRequest){

        log.debug("Executing web service method. searchUsersPost");

        //Calling searchUsers here does not provoke that method's interceptor/decorator being called (only this one's)
        URI uri=null;
        Response response = searchUsers(searchRequest.getFilter(),searchRequest.getStartIndex(), searchRequest.getCount(),
                searchRequest.getSortBy(), searchRequest.getSortOrder(), searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr());

        try {
            uri = new URI(endpointUrl + "/" + SEARCH_SUFFIX);
        }
        catch (Exception e){
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
    @ApiOperation(value = "PATCH operation", notes = "https://tools.ietf.org/html/rfc7644#section-3.5.2", response = UserResource.class)
    public Response patchUser(
            PatchRequest request,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList){

        Response response;
        try{
            log.debug("Executing web service method. patchUser");
            UserResource user=new UserResource();
            ScimCustomPerson person=userPersistenceHelper.getPersonByInum(id);  //person is not null (check associated decorator method)

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
                }
                else
                    user = (UserResource) scim2PatchService.applyPatchOperation(user, po);
            }

            //Throws exception if final representation does not pass overall validation
            log.debug("patchUser. Revising final resource representation still passes validations");
            executeDefaultValidation(user);
            ScimResourceUtil.adjustPrimarySubAttributes(user);

            //Update timestamp
            user.getMeta().setLastModified(DateUtil.millisToISOString(System.currentTimeMillis()));

            //Replaces the information found in person with the contents of user
            scim2UserService.replacePersonInfo(person, user, endpointUrl);

            String json=resourceSerializer.serialize(user, attrsList, excludedAttrsList);
            response=Response.ok(new URI(user.getMeta().getLocation())).entity(json).build();
        }
        catch (InvalidAttributeValueException e){
            log.error(e.getMessage(), e);
            response=getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.MUTABILITY, e.getMessage());
        }
        catch (SCIMException e){
            response=getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, e.getMessage());
        }
        catch (Exception e){
            log.error("Failure at patchUser method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @PostConstruct
    public void setup(){
        //Do not use getClass() here... a typical weld issue...
        endpointUrl=appConfiguration.getBaseEndpoint() + UserWebService.class.getAnnotation(Path.class).value();
    }

}
