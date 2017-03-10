/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import com.wordnik.swagger.annotations.Api;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import javax.inject.Named;

import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.RptAuthorizationResponse;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.uma.RPTManager;
import org.xdi.oxauth.service.uma.ResourceSetPermissionManager;
import org.xdi.oxauth.service.uma.UmaValidationService;
import org.xdi.oxauth.service.uma.authorization.AuthorizationService;
import org.xdi.oxauth.util.ServerUtil;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * The endpoint at which the requester asks for authorization to have a new permission.
 */
@Path("/requester/perm")
@Api(value = "/requester/perm", description = "RPT authorization endpoint. RPT is authorized with new permission(s).")
@Named("rptPermissionAuthorizationRestWebService")
public class RptPermissionAuthorizationWS {

    @Inject
    private Logger log;
    @Inject
    private ErrorResponseFactory errorResponseFactory;
    @Inject
    private RPTManager rptManager;
    @Inject
    private ResourceSetPermissionManager resourceSetPermissionManager;
    @Inject
    private UmaValidationService umaValidationService;
    @Inject
    private AuthorizationService umaAuthorizationService;
    @Inject
    private ClientService clientService;
    @Inject
    private LdapEntryManager ldapEntryManager;

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response requestRptPermissionAuthorization(
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("Host") String amHost,
            RptAuthorizationRequest rptAuthorizationRequest,
            @Context HttpServletRequest httpRequest) {
        try {
            final AuthorizationGrant grant = umaValidationService.assertHasAuthorizationScope(authorization);

            final String validatedAmHost = umaValidationService.validateAmHost(amHost);
            final UmaRPT rpt = authorizeRptPermission(authorization, rptAuthorizationRequest, httpRequest, grant, validatedAmHost);

            // convert manually to avoid possible conflict between resteasy providers, e.g. jettison, jackson
            return Response.ok(ServerUtil.asJson(new RptAuthorizationResponse(rpt.getCode()))).build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }

    private UmaRPT authorizeRptPermission(String authorization,
                                          RptAuthorizationRequest rptAuthorizationRequest,
                                          HttpServletRequest httpRequest,
                                          AuthorizationGrant grant,
                                          String amHost) {
        UmaRPT rpt;
        if (Util.isNullOrEmpty(rptAuthorizationRequest.getRpt())) {
            rpt = rptManager.createRPT(authorization, amHost, false);
        } else {
            rpt = rptManager.getRPTByCode(rptAuthorizationRequest.getRpt());
        }

        // Validate RPT
        try {
            umaValidationService.validateRPT(rpt);
        } catch (WebApplicationException e) {
            // according to latest UMA spec ( dated 2015-02-23 https://docs.kantarainitiative.org/uma/draft-uma-core.html)
            // it's up to implementation whether to create new RPT for each request or pass back requests RPT.
            // Here we decided to pass back new RPT if request's RPT in invalid.
            rpt = rptManager.getRPTByCode(rptAuthorizationRequest.getRpt());
        }

        final ResourceSetPermission resourceSetPermission = resourceSetPermissionManager.getResourceSetPermissionByTicket(rptAuthorizationRequest.getTicket());

        // Validate resource set permission
        umaValidationService.validateResourceSetPermission(resourceSetPermission);

        // Add permission to RPT
        if (umaAuthorizationService.allowToAddPermission(grant, rpt, resourceSetPermission, httpRequest, rptAuthorizationRequest.getClaims())) {
            rptManager.addPermissionToRPT(rpt, resourceSetPermission);
            invalidateTicket(resourceSetPermission);
            return rpt;
        }

        // throw not authorized exception
        throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.NOT_AUTHORIZED_PERMISSION)).build());
    }

    private void invalidateTicket(ResourceSetPermission resourceSetPermission) {
        try {
            resourceSetPermission.setAmHost("invalidated"); // invalidate ticket and persist
            ldapEntryManager.merge(resourceSetPermission);
        } catch (Exception e) {
            log.error("Failed to invalidate ticket: " + resourceSetPermission.getTicket() + ". " + e.getMessage(), e);
        }
    }
}
