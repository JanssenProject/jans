/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.PermissionTicket;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.service.uma.ResourceSetPermissionManager;
import org.xdi.oxauth.service.uma.UmaValidationService;
import org.xdi.oxauth.service.uma.resourceserver.PermissionService;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * The endpoint at which the host registers permissions that it anticipates a
 * requester will shortly be asking for from the AM. This AM's endpoint is part
 * of resource set registration API.
 * <p/>
 * In response to receiving an access request accompanied by an RPT that is
 * invalid or has insufficient authorization data, the host SHOULD register a
 * permission with the AM that would be sufficient for the type of access
 * sought. The AM returns a permission ticket for the host to give to the
 * requester in its response.
 *
 * @author Yuriy Zabrovarnyy
 */
@Name("resourceSetPermissionRegistrationRestWebService")
@Path("/host/rsrc_pr")
@Api(value = "/host/rsrc_pr", description = "The resource server uses the protection API's permission registration endpoint to register a requested permission with the authorization server that would suffice for the client's access attempt. The authorization server returns a permission ticket for the resource server to give to the client in its response. The PAT provided in the API request implicitly identifies the resource owner (\"subject\") to which the permission applies.\n" +
        "\n" +
        "Note: The resource server is free to choose the extent of the requested permission that it registers, as long as it minimally suffices for the access attempted by the client. For example, it can choose to register a permission that covers several scopes or a resource set that is greater in extent than the specific resource that the client attempted to access. Likewise, the authorization server is ultimately free to choose to partially fulfill the elements of a permission request based on incomplete satisfaction of policy criteria, or not to fulfill the request.\n" +
        "\n" +
        "The resource server uses the POST method at the endpoint. The body of the HTTP request message contains a JSON object providing the requested permission, using a format derived from the scope description format specified in [OAuth-resource-reg], as follows. The object has the following properties:")
public class PermissionRegistrationWS {

    @Logger
    private Log log;
    @In
    private TokenService tokenService;
    @In
    private ResourceSetPermissionManager resourceSetPermissionManager;
    @In
    private ErrorResponseFactory errorResponseFactory;
    @In
    private UmaValidationService umaValidationService;
    
    @In
    private PermissionService umaRsPermissionService;

    @In
	private AppConfiguration appConfiguration;

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    @ApiOperation(value = "Registers permission using the POST method",
            consumes = UmaConstants.JSON_MEDIA_TYPE,
            produces = UmaConstants.JSON_MEDIA_TYPE,
            notes = "The resource server uses the POST method at the endpoint. The body of the HTTP request message contains a JSON object providing the requested permission, using a format derived from the scope description format specified in [OAuth-resource-reg], as follows. The object has the following properties:")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 400, message = "Bad Request")
    })
    public Response registerResourceSetPermission(@Context HttpServletRequest request,
                                                  @HeaderParam("Authorization") String authorization,
                                                  @HeaderParam("Host") String amHost,
                                                  @ApiParam(value = "The identifier for a resource set to which this client is seeking access. The identifier MUST correspond to a resource set that was previously registered.", required = true)
                                                  UmaPermission resourceSetPermissionRequest) {
        try {
            umaValidationService.assertHasProtectionScope(authorization);
            String validatedAmHost = umaValidationService.validateAmHost(amHost);
            umaValidationService.validateResourceSet(resourceSetPermissionRequest);

            final ResourceSetPermission resourceSetPermissions = resourceSetPermissionManager.createResourceSetPermission(validatedAmHost, resourceSetPermissionRequest, umaRsPermissionService.rptExpirationDate());
            resourceSetPermissionManager.addResourceSetPermission(resourceSetPermissions, tokenService.getClientDn(authorization));

            return Response.status(Response.Status.CREATED).
                            entity(new PermissionTicket(resourceSetPermissions.getTicket())).
                            build();
        } catch (Exception ex) {
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            log.error("Exception happened", ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }
}
