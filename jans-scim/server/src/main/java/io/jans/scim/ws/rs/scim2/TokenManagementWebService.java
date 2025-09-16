package io.jans.scim.ws.rs.scim2;

import io.jans.scim.model.scim.ScimCustomPerson;
import io.jans.scim.model.scim2.*;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.service.filter.ProtectedApi;
import io.jans.scim.service.scim2.UserTokensService;

import java.util.*;
import java.util.stream.Collectors;
import java.net.URI;

import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import static io.jans.scim.model.scim2.Constants.*;

@Dependent
@Named
@Path("/v2/UserTokens")
public class TokenManagementWebService extends BaseScimWebService implements ITokenWebService {

    private String userResourceType;
    
    @Inject
    private UserTokensService uts;
    
    @Path("{id}")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @ProtectedApi(scopes = { "https://jans.io/scim/tokens" })
    public Response getTokensMetadata(
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. getTokensMetadata");

            ScimCustomPerson person = userPersistenceHelper.getPersonByInum(id);
            if (person == null) return notFoundResponse(id, userResourceType);
            
            List<TokenResource> trs = uts.getTokens(id);
            trs.forEach(tr -> tr.setId(null));  //prevent field serialization in the output

            List<BaseScimResource> tokens = trs.stream().map(BaseScimResource.class::cast)
                    .collect(Collectors.toList());
            int len = tokens.size();

            String json = getListResponseSerialized(len, 1, tokens, attrsList, excludedAttrsList, len == 0);
            return Response.ok(json).location(new URI(endpointUrl)).build();
            
        } catch (Exception e) {
            log.error("Failure at getTokensMetadata method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;
        
    }
      
    @Path("{id}")
    @DELETE
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @ProtectedApi(scopes = { "https://jans.io/scim/tokens" })
    public Response revokeTokens(@PathParam("id") String id, @QueryParam("tokenHash") String hash) {

        Response response;
        try {
            log.debug("Executing web service method. revokeTokens");

            ScimCustomPerson person = userPersistenceHelper.getPersonByInum(id);
            if (person == null) return notFoundResponse(id, userResourceType);
            
            List<TokenResource> tokens = uts.getTokens(id);
            TokenResource tr = tokens.stream().filter(t -> t.getHash().equals(hash)).findFirst().orElse(null); 
            if (tr == null) {
                return getErrorResponse(Response.Status.NOT_FOUND,
                        "Token with given hash does not seem to be associated to user " + id);
            }
            
            if (uts.revoke(tr.getIti())) {                
                response = Response.noContent().build();
            } else {
                response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
                        "Unable to revoke ALL tokens associated to the token with hash " + hash); 
            }
            
        } catch (Exception e) {
            log.error("Failure at getTokensMetadata method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;
        
    }

    @PostConstruct
    public void setup() {
        //Do not use getClass() here...
        init(TokenManagementWebService.class);
        userResourceType = ScimResourceUtil.getType(UserResource.class);
    }
    
}
