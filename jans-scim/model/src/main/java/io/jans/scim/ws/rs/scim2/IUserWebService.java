/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.ws.rs.scim2;

import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.user.UserResource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static io.jans.scim.model.scim2.Constants.*;

import java.util.List;

/**
 * SCIM service interface with available methods to manipulate User resources.
 *
 * <p>The parameters <i>attrsList</i> and <i>excludedAttrsList</i> found in methods of this interface are aimed at
 * specifying the "attributes" and "excludedAttributes" query params regarded in section 3.9 of SCIM spec protocol document
 * (RFC 7644).</p>
 * <p>Every SCIM service operation that returns resources (e.g Users, Groups, etc.) offers the possibility to specify
 * which attributes can be included for every resource part of the response. The default behavior is returning those attributes
 * that according to the resource Schema have returnability = "always" in addition to those with returnability = "default".</p>
 * <p><i><b>attrsList</b></i> is used to override the default attribute set, so when supplying a not-null or not empty String,
 * the attributes included in the resource(s) of the response will be those with returnability = "always" as well as those
 * specified by <i>attrsList</i>.</p>
 * <p>This parameter consists of  a comma-separated list of attribute names. An example of a valid value for <i>attrsList</i>
 * when the resource of interest is User, could be: <code>userName, active, name.familyName, addresses,  emails.value, emails.type,
 * urn:ietf:params:scim:schemas:extension:gluu:2.0:User:myCustomAttribute</code></p>
 * <p>Note that attributes marked with returnability = "never" (such as a User password) will always be filtered out from
 * the output, so including such attributes in <i>attrsList</i> has no effect.</p>
 * <p><i><b>excludedAttrsList</b></i> is used to specify the set of attributes that should be excluded from the default
 * attribute set. In this sense, the resources found in the response will include the attributes whose returnability = "always"
 * in addition to those with returnability = "default" except for those included in <i>excludedAttrsList</i>. As with
 * <i>attrsList</i>, this parameter must be in the form of a comma-separated list of attribute names.</p>
 * <p><i>attrsList</i> and <i>excludedAttrsList</i> are mutually exclusive: if both are provided only <i>attrsList</i>
 * will be taken into account to compute the output attribute set.</p>
 */
/*
 * Created by jgomer on 2017-09-01.
 *
 * Shared (rest-easy) interface of the SCIM service.
 */
public interface IUserWebService {

    /**
     * Service method that allows creating a User resource via POST (as per section 3.3 of RFC 7644).
     * @param user An object that represents the User to create
     * @param attrsList See notes about attributes query param in this interface description
     * @param excludedAttrsList See notes about excludedAttributes query param in this interface description
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 201 (created) and a {@link UserResource}
     * in the entity body (the resource just created)
     */
    @Path("/v2/Users")
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response createUser(
            UserResource user,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList);

    /**
     * Service method that retrieves a User resource using GET (as per section 3.4.1 of RFC 7644).
     * @param id The "id" attribute of the resource to retrieve
     * @param attrsList See notes about attributes query param in this interface description
     * @param excludedAttrsList See notes about excludedAttributes query param in this interface description
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 200 and a {@link UserResource}
     * in the entity body (the resource retrieved)
     */
    @Path("/v2/Users/{id}")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response getUserById(
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList);

    /**
     * Service method that allows updating a User resource via PUT (as per section 3.5.1 of RFC 7644).
     * <p>This operation is not suitable to delete/remove/nullify attributes. For this purpose you can use the PATCH
     * operation instead. PUT is intended to do replacements using the (not-null) values supplied in <code>user</code>
     * parameter.</p>
     * <p>To learn more about how the update works, read the replacement rules found at {@link io.jans.scim.model.scim2.util.ScimResourceUtil#transferToResourceReplace(BaseScimResource, BaseScimResource, List)
     * ScimResourceUtil#transferToResourceReplace}.</p>
     * @param user An object that contains the data to update on a destination resource. There is no need to supply a full
     *             resource, just provide one with the attributes which are intended to be replaced in the destination
     * @param id The "id" attribute of the resource to update (destination)
     * @param attrsList See notes about attributes query param in this interface description
     * @param excludedAttrsList See notes about excludedAttributes query param in this interface description
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 200 and a {@link UserResource}
     * in the entity body (the resource after the update took place)
     */
    @Path("/v2/Users/{id}")
    @PUT
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response updateUser(
            UserResource user,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList);

    /**
     * Removes a User via DELETE HTTP method (see section 3.6 of RFC 7644).
     * @param id The "id" attribute of the resource to be removed
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 204 (no content)
     */
    @Path("/v2/Users/{id}")
    @DELETE
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response deleteUser(@PathParam("id") String id);

    /**
     * Sends a search query for User resources using GET (see section 3.4.2 of RFC 7644).
     * @param filter A filter expression so that the search will return only those resources matching the expression.
     *               To learn more about SCIM filter expressions and operators, see section 3.4.2.2 of RFC 7644.
     * @param startIndex The 1-based index of the first query result. If a negative integer or null is provided, the
     *                   search is performed as if 1 was provided as value.
     * @param count Specifies the desired maximum number of query results per page the response must include. If null is
     *              provided, the maximum supported by the server is used. If count is zero, this is interpreted as
     *              no results should be included (only the total amount is). If a negative number is supplied, the search
     *              is performed as if zero was provided as value.
     * @param sortBy Specifies the attribute whose value will be used to order the returned resources. If sortBy is null
     *               the results will be sorted by userName attribute.
     * @param sortOrder The order in which the <code>sortBy</code> parameter is applied. Allowed values are "ascending"
     *                  or "descending", being "ascending" the default if null or an unknown value is passed.
     * @param attrsList See notes about attributes query param in this interface description
     * @param excludedAttrsList See notes about excludedAttributes query param in this interface description
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 200 and a {@link io.jans.scim.model.scim2.ListResponse
     * ListResponse} in the entity body (holding a collection of SCIM resources)
     */
    @Path("/v2/Users")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response searchUsers(
            @QueryParam(QUERY_PARAM_FILTER) String filter,
            @QueryParam(QUERY_PARAM_START_INDEX) Integer startIndex,
            @QueryParam(QUERY_PARAM_COUNT) Integer count,
            @QueryParam(QUERY_PARAM_SORT_BY) String sortBy,
            @QueryParam(QUERY_PARAM_SORT_ORDER) String sortOrder,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList);

    /**
     * Sends a search query for User resources using POST (see section 3.4.3 of RFC 7644).
     * @param searchRequest An object containing the parameters for the query to execute. These are the same parameters
     *                      passed in the URL for searches, for example in
     *                      {@link #searchUsers(String, Integer, Integer, String, String, String, String) searchDevices}
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this request should contain a status code of 200 and a {@link io.jans.scim.model.scim2.ListResponse
     * ListResponse} in the entity body (holding a collection of SCIM resources)
     */
    @Path("/v2/Users/.search")
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response searchUsersPost(SearchRequest searchRequest);

    /**
     * Service method that allows to modify a User resource via PATCH (see section 3.5.2 of RFC 7644).
     * <p>Note that patching offers a fine-grained control over the attributes to modify. While PUT is more intended to
     * replace attribute values, PATCH allows to perform localized updates, removals and additions in certain portions
     * of the target resource.</p>
     * @param request A <code>PatchRequest</code> that contains the operations to apply upon the resource being updated
     * @param id The id of the resource to update
     * @param attrsList See notes about attributes query param in this interface description
     * @param excludedAttrsList See notes about excludedAttributes query param in this interface description
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 200 and a {@link UserResource}
     * in the entity body (the resource after modifications took place)
     */
    @Path("/v2/Users/{id}")
    @PATCH
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response patchUser(
                PatchRequest request,
                @PathParam("id") String id,
                @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
                @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList);

}