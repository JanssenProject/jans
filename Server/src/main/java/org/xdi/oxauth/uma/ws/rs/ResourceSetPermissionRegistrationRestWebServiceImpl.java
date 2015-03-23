/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.ResourceSetPermissionRequest;
import org.xdi.oxauth.model.uma.ResourceSetPermissionTicket;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.service.uma.ResourceSetPermissionManager;
import org.xdi.oxauth.service.uma.UmaValidationService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Yuriy Movchan Date: 10/18/2012
 */
@Name("resourceSetPermissionRegistrationRestWebService")
public class ResourceSetPermissionRegistrationRestWebServiceImpl implements ResourceSetPermissionRegistrationRestWebService {

    public static final int DEFAULT_PERMISSION_LIFETIME = 3600;

    @Logger
    private Log log;
    @In
    private TokenService tokenService;
    @In
    private ResourceSetPermissionManager resourceSetPermissionManager;
    @In
    private ErrorResponseFactory errorResponseFactory;
    //	@In
//	private AuthorizationGrantList authorizationGrantList;
    @In
    private UmaValidationService umaValidationService;

    public Response registerResourceSetPermission(HttpServletRequest request, String authorization, String amHost, ResourceSetPermissionRequest resourceSetPermissionRequest) {
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

    private Response registerResourceSetPermissionImpl(HttpServletRequest request, String authorization, String validatedAmHost, ResourceSetPermissionRequest resourceSetPermissionRequest) {
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
