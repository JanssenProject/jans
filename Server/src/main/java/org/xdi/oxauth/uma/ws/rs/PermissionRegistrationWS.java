/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import com.wordnik.swagger.annotations.Api;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.RegisterPermissionRequest;
import org.xdi.oxauth.model.uma.ResourceSetPermissionTicket;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.service.uma.ResourceSetPermissionManager;
import org.xdi.oxauth.service.uma.UmaValidationService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.Calendar;
import java.util.Date;

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
@Api(value = "/host/rsrc_pr", description = "The endpoint at which the host registers permissions that it anticipates a " +
        " requester will shortly be asking for from the AM. This AM's endpoint is part " +
        " of resource set registration API.")
public class PermissionRegistrationWS {

    public static final int DEFAULT_PERMISSION_LIFETIME = 3600;

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

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response registerResourceSetPermission(@Context HttpServletRequest request,
                                                  @HeaderParam("Authorization") String authorization,
                                                  @HeaderParam("Host") String amHost,
                                                  RegisterPermissionRequest resourceSetPermissionRequest) {
        try {
            umaValidationService.validateAuthorizationWithProtectScope(authorization);
            String validatedAmHost = umaValidationService.validateAmHost(amHost);
            umaValidationService.validateAuthorizationWithProtectScope(authorization);
            umaValidationService.validateResourceSet(resourceSetPermissionRequest);

            return registerResourceSetPermissionImpl(request, authorization, validatedAmHost, resourceSetPermissionRequest);
        } catch (Exception ex) {
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            log.error("Exception happened", ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }

    private Response registerResourceSetPermissionImpl(HttpServletRequest request, String authorization, String validatedAmHost, RegisterPermissionRequest resourceSetPermissionRequest) {
        final ResourceSetPermission resourceSetPermissions = resourceSetPermissionManager.createResourceSetPermission(validatedAmHost, resourceSetPermissionRequest, rptExpirationDate());
        resourceSetPermissionManager.addResourceSetPermission(resourceSetPermissions, tokenService.getClientDn(authorization));

        return prepareResourceSetPermissionTicketResponse(request, resourceSetPermissions);
    }

    public static Date rptExpirationDate() {
        int lifeTime = ConfigurationFactory.getConfiguration().getUmaRequesterPermissionTokenLifetime();
        if (lifeTime <= 0) {
            lifeTime = DEFAULT_PERMISSION_LIFETIME;
        }

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, lifeTime);
        return calendar.getTime();
    }

    private Response prepareResourceSetPermissionTicketResponse(HttpServletRequest request,
                                                                ResourceSetPermission resourceSetPermissions) {
        ResponseBuilder builder = Response.status(Response.Status.CREATED);

        builder.entity(new ResourceSetPermissionTicket(resourceSetPermissions.getTicket()));

        // Add location
        StringBuffer location = request.getRequestURL().append("/").append(resourceSetPermissions.getConfigurationCode());
        builder.header("Location", location);

        return builder.build();
    }

    //	public Response getResourceSetPermission(HttpServletRequest request, String authorization,
    //			String amHost, String host, String configurationCode) {
    //		try {
    //            umaValidationService.validateAuthorizationWithProtectScope(authorization);
    //			String validatedAmHost = umaValidationService.validateAmHost(amHost);
    //			String validatedHost = umaValidationService.validateHost(host);
    //
    //			return getResourceSetPermissionImpl(request, authorization, validatedAmHost, validatedHost, configurationCode);
    //		} catch (Exception ex) {
    //			if (ex instanceof WebApplicationException) {
    //				throw (WebApplicationException) ex;
    //			}
    //
    //			log.error("Exception happened", ex);
    //			throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
    //					.entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
    //		}
    //	}

//	private Response getResourceSetPermissionImpl(HttpServletRequest request, String authorization, String validatedAmHost, String validatedHost, String configurationCode) {
//        final AuthorizationGrant authorizationGrant = tokenService.getAuthorizationGrant(authorization);
//
//		String resourceSetPermissionTicket = resourceSetPermissionManager.getResourceSetPermissionTicketByConfigurationCode(configurationCode, authorizationGrant.getClientDn());
//		if (StringHelper.isEmpty(resourceSetPermissionTicket)) {
//			log.error("Failed to get resourceSetPermissionTicket by configurationCode: '{0}'", configurationCode);
//			throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
//					.entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.INVALID_REQUEST)).build());
//		}
//
//        ResourceSetPermission resourceSetPermissions = resourceSetPermissionManager.getResourceSetPermissionByTicket(resourceSetPermissionTicket);
//		if (resourceSetPermissions == null) {
//			log.error("Failed to get resourceSetPermissions by resourceSetPermissionTicket: '{0}'", resourceSetPermissionTicket);
//			throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
//					.entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.INVALID_REQUEST)).build());
//		}
//
//		return prepareResourceSetPermissionTicketResponse(request, resourceSetPermissions, Response.Status.OK);
//	}
}
