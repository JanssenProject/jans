package io.jans.configapi.plugin.scim.rest;

import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.plugin.scim.service.ScimService;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.user.UserResource;
import org.slf4j.Logger;

import static io.jans.scim.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_ATTRIBUTES;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_COUNT;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_EXCLUDED_ATTRS;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_FILTER;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_SORT_BY;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_SORT_ORDER;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_START_INDEX;
import static io.jans.scim.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/user")
public class ScimResource {

    public static final String SEARCH_SUFFIX = ".search";
    @Inject
    Logger log;

    @Inject
    ScimService scimService;

    @GET
    @Produces({ MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT })
    @HeaderParam("Accept")
    @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = { "https://jans.io/scim/users.read" })
    public Response searchUsers(@QueryParam(QUERY_PARAM_FILTER) String filter,
            @QueryParam(QUERY_PARAM_START_INDEX) Integer startIndex, @QueryParam(QUERY_PARAM_COUNT) Integer count,
            @QueryParam(QUERY_PARAM_SORT_BY) String sortBy, @QueryParam(QUERY_PARAM_SORT_ORDER) String sortOrder,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) throws Exception {
        log.info(" Request to search User with filter:{}, startIndex:{}, sortBy:{}, sortOrder:{}, attrsList{},  excludedAttrsList:{}", filter, startIndex, sortBy, sortOrder, attrsList,excludedAttrsList);
        return scimService.serachScimUser(filter, startIndex, count, sortBy, sortOrder, attrsList, excludedAttrsList);
    }

    @Path(SEARCH_SUFFIX)
    @POST
    @Consumes({ MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON })
    @Produces({ MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT })
    @HeaderParam("Accept")
    @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = { "https://jans.io/scim/users.read" })
    public Response searchUsersPost(SearchRequest searchRequest) throws Exception {

        log.info(" Request to search User with SearchRequest object  searchRequest:{}", searchRequest);
        return scimService.serachScimUserPost(searchRequest);
    }

    @POST
    @Consumes({ MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON })
    @Produces({ MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT })
    @HeaderParam("Accept")
    @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = { "https://jans.io/scim/users.write" })
    public Response createUser(UserResource user, @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) throws Exception {
        log.info(" Request to create User with user:{}, attrsList:{}, excludedAttrsList:{}", user , attrsList, excludedAttrsList);
        return scimService.createScimUser(user, attrsList, excludedAttrsList);
    }

    @Path("{id}")
    @GET
    @Produces({ MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT })
    @HeaderParam("Accept")
    @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = { "https://jans.io/scim/users.read" })
    public Response getUserById(@PathParam("id") String id, @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) throws Exception {

        log.info(" Request to search User with id:{}, attrsList:{}, excludedAttrsList:{}",id, attrsList, excludedAttrsList);
        return scimService.getScimUserById(id, attrsList, excludedAttrsList);
    }

    @Path("{id}")
    @PUT
    @Consumes({ MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON })
    @Produces({ MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT })
    @HeaderParam("Accept")
    @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = { "https://jans.io/scim/users.write" })
    public Response updateUser(UserResource user, @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) throws Exception {

        log.info(" Request to update User with user:{}, id:{}, attrsList:{}, excludedAttrsList:{} ", user, id, attrsList, excludedAttrsList);
        return scimService.updateScimUser(user, id, attrsList, excludedAttrsList);

    }

    @Path("{id}")
    @DELETE
    @Produces({ MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT })
    @HeaderParam("Accept")
    @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = { "https://jans.io/scim/users.write" })
    public Response deleteUser(@PathParam("id") String id) throws Exception {
        log.info(" Request to delete User with id:{} ", id );
        return scimService.deleteScimUser(id);
    }


    @Path("{id}")
    @PATCH
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON,MediaType.APPLICATION_JSON_PATCH_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/users.write"})
    public Response patchUser(
            PatchRequest patchRequest,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) throws Exception {
        log.info(" Request to patch User with patchRequest:{}, id:{}, attrsList:{}, excludedAttrsList:{}", patchRequest, id , attrsList, excludedAttrsList);
        return scimService.patchScimUser(patchRequest, id, attrsList, excludedAttrsList);

    }

}
