/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.ws.rs.scim2;

import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.fido.Fido2DeviceResource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static io.jans.scim.model.scim2.Constants.*;

import java.util.List;

/*
 * Shared interface of SCIM service methods to manipulate Fido 2 Devices.
 */
public interface IFido2DeviceWebService {

    /**
     * Service method that retrieves a Fido 2 device resource using GET (as per section 3.4.1 of RFC 7644).
     * @param id The "id" attribute of the resource to retrieve
     * @param userId The identifier of the user that owns the device ("id" attribute, not "userName" attribute)
     * @param attrsList See notes about attributes query param in this interface description
     * @param excludedAttrsList See notes about excludedAttributes query param in this interface description
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 200 and a {@link Fido2DeviceResource}
     * in the entity body (the resource retrieved)
     */
    @Path("/v2/Fido2Devices/{id}")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response getF2DeviceById(@PathParam("id") String id,
                           @QueryParam("userId") String userId,
                           @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
                           @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList);

    /**
     * Service method that allows updating a Fido 2 device resource via PUT (as per section 3.5.1 of RFC 7644).
     * <p>This operation is not suitable to delete/remove/nullify attributes. For this purpose you can use the PATCH
     * operation instead. PUT is intended to do replacements using the (not-null) values supplied in <code>fidoDeviceResource</code>
     * parameter.</p>
     * <p>To learn more about how the update works, read the replacement rules found at {@link io.jans.scim.model.scim2.util.ScimResourceUtil#transferToResourceReplace(BaseScimResource, BaseScimResource, List)
     * ScimResourceUtil#transferToResourceReplace}.</p>
     * @param fidoDeviceResource An object that contains the data to update on a destination resource. There is no need
     *                           to supply a full resource, just provide one with the attributes which are intended to
     *                           be replaced in the destination
     * @param id The "id" attribute of the resource to update (destination)
     * @param attrsList See notes about attributes query param in this interface description
     * @param excludedAttrsList See notes about excludedAttributes query param in this interface description
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 200 and a {@link Fido2DeviceResource}
     * in the entity body (the resource after the update took place)
     */
    @Path("/v2/Fido2Devices/{id}")
    @PUT
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response updateF2Device(
            Fido2DeviceResource fidoDeviceResource,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList);

    /**
     * Removes a Fido 2 device via DELETE HTTP method (see section 3.6 of RFC 7644).
     * @param id The "id" attribute of the resource to be removed
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 204 (no content)
     */
    @Path("/v2/Fido2Devices/{id}")
    @DELETE
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response deleteF2Device(@PathParam("id") String id);

    /**
     * Sends a search query for Fido 2 devices using GET (see section 3.4.2 of RFC 7644).
     * @param userId An (optional) user identifier in order to focus the search on a specific person. 
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
    @Path("/v2/Fido2Devices")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response searchF2Devices(
            @QueryParam("userId") String userId,
            @QueryParam(QUERY_PARAM_FILTER) String filter,
            @QueryParam(QUERY_PARAM_START_INDEX) Integer startIndex,
            @QueryParam(QUERY_PARAM_COUNT) Integer count,
            @QueryParam(QUERY_PARAM_SORT_BY) String sortBy,
            @QueryParam(QUERY_PARAM_SORT_ORDER) String sortOrder,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList);

    /**
     * Sends a search query for Fido 2 devices using POST (see section 3.4.3 of RFC 7644).
     * @param searchRequest An object containing the parameters for the query to execute. These are the same parameters
     *                      passed in the URL for searches, for example in
     *                      {@link #searchF2Devices(String, String, Integer, Integer, String, String, String, String) searchDevices}
     * @param userId An (optional) user identifier in order to focus the search on a specific person.
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this request should contain a status code of 200 and a {@link io.jans.scim.model.scim2.ListResponse
     * ListResponse} in the entity body (holding a collection of SCIM resources)
     */
    @Path("/v2/Fido2Devices/.search")
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response searchF2DevicesPost(SearchRequest searchRequest, @QueryParam("userId") String userId);

    /**
     * Service method that allows to modify a Fido 2 device resource via PATCH (see section 3.5.2 of RFC 7644).
     * <p>Note that patching offers a fine-grained control over the attributes to modify. While PUT is more intended to
     * replace attribute values, PATCH allows to perform localized updates, removals and additions in certain portions
     * of the target resource.</p>
     * @param request A <code>PatchRequest</code> that contains the operations to apply upon the resource being updated
     * @param id The id of the resource to update
     * @param attrsList See notes about attributes query param in this interface description
     * @param excludedAttrsList See notes about excludedAttributes query param in this interface description
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 200 and a {@link Fido2DeviceResource}
     * in the entity body (the resource after modifications took place)
     */
    @Path("/v2/Fido2Devices/{id}")
    @PATCH
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response patchF2Device(
            PatchRequest request,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList);

}
