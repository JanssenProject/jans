package io.jans.scim.ws.rs.scim2;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import static io.jans.scim.model.scim2.Constants.*;

/**
 * SCIM service interface with available methods to list and revoke tokens issued to users by the IDP.
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
public interface ITokenWebService {
    
    /**
     * Service method that retrieves token metadata associated to a given user 
     * @param id The "id" attribute of the user in question
     * @param attrsList See notes about attributes query param in this interface description
     * @param excludedAttrsList See notes about excludedAttributes query param in this interface description
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 200 and a {@link io.jans.scim.model.scim2.ListResponse
     * ListResponse} in the entity body (holding a collection of SCIM token resources, see 
     * {@link io.jans.scim.model.scim2.TokenResource TokenResource})
     */
    @Path("/v2/UserTokens/{id}")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    Response getTokensMetadata(
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList);

    /**
     * Revokes one or more tokens associated to a given user. All tokens associated to the token that matches
     * the hash passed as input are removed (as long as they are tied to the user identified by the value passed)  
     * @param id The "id" attribute of the user in question
     * @param hash The token hash. This is a value found in the response of method getTokensMetadata
     * @return An object abstracting the response obtained from the server to this request.
     * A succesful response for this operation should contain a status code of 204 (no content)
     */
    @Path("/v2/UserTokens/{id}")
    @DELETE
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    Response revokeTokens(@PathParam("id") String id, @QueryParam("tokenHash") String hash);

}
