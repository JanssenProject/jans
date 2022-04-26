/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.rest;

import io.jans.scim.ws.rs.scim2.IGroupWebService;
import io.jans.scim.ws.rs.scim2.PATCH;

import static io.jans.scim.model.scim2.Constants.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * An interface that exhibits operations to manipulate Group SCIM resources.
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
 * Created by jgomer on 2017-09-14.
 */
public interface ClientSideGroupService extends IGroupWebService, CloseableClient {

    /**
     * Invokes a service method that allows creating a Group resource via POST (as per section 3.3 of RFC 7644).
     * @param jsonGroup A String with the payload for the operation. It represents the Group resource to create
     * @param attrsList See notes about attributes query param in this interface description
     * @param excludedAttrsList See notes about excludedAttributes query param in this interface description
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 201 (created) and a GroupResource
     * in the entity body (the resource just created)
     */
    @Path("/v2/Groups")
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response createGroup(
            String jsonGroup,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList);

    /**
     * Invokes a service method that allows updating a Group resource via PUT (as per section 3.5.1 of RFC 7644).
     * <p>This operation is not suitable to delete/remove/nullify attributes. For this purpose you can use the PATCH
     * operation instead. PUT is intended to do replacements using the (not-null) values supplied in <code>fidoDeviceResource</code>
     * parameter.</p>
     * <p>To learn more about how the update works, read the replacement rules found at method
     * <code>io.jans.scim.model.scim2.util.ScimResourceUtil#transferToResourceReplace.</code></p>
     * @param jsonGroup A String with the payload for the operation. It represents an object that contains the data to
     *                  update on a destination resource. There is no need to supply a full resource, just provide one
     *                  with the attributes which are intended to be replaced in the destination.
     * @param id The "id" attribute of the resource to update (destination)
     * @param attrsList See notes about attributes query param in this interface description
     * @param excludedAttrsList See notes about excludedAttributes query param in this interface description
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 200 and a GroupResource
     * in the entity body (the resource after the update took place)
     */
    @Path("/v2/Groups/{id}")
    @PUT
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response updateGroup(
            String jsonGroup,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList);

    /**
     * Invokes a service method that allows to modify a Group resource via PATCH (see section 3.5.2 of RFC 7644).
     * <p>Note that patching offers a fine-grained control over the attributes to modify. While PUT is more intended to
     * replace attribute values, PATCH allows to perform localized updates, removals and additions in certain portions
     * of the target resource.</p>
     * @param jsonPatch A String with the payload for the operation. It represents a <code>PatchRequest</code> that
     *                  contains the operations to apply upon the resource being updated
     * @param id The id of the resource to update
     * @param attrsList See notes about attributes query param in this interface description
     * @param excludedAttrsList See notes about excludedAttributes query param in this interface description
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 200 and a GroupResource
     * in the entity body (the resource after modifications took place)
     */
    @Path("/v2/Groups/{id}")
    @PATCH
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response patchGroup(
            String jsonPatch,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList);

}
