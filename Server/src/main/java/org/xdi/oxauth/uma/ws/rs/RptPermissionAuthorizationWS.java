/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import com.google.common.base.Strings;
import com.wordnik.swagger.annotations.Api;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.federation.FederationTrust;
import org.xdi.oxauth.model.federation.FederationTrustStatus;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.uma.RptAuthorizationResponse;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.FederationDataService;
import org.xdi.oxauth.service.uma.RPTManager;
import org.xdi.oxauth.service.uma.ResourceSetPermissionManager;
import org.xdi.oxauth.service.uma.UmaValidationService;
import org.xdi.oxauth.service.uma.authorization.AuthorizationService;
import org.xdi.oxauth.util.ServerUtil;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * The endpoint at which the requester asks for authorization to have a new permission.
 */
@Path("/requester/perm")
@Api(value = "/requester/perm", description = "RPT authorization endpoint. RPT is authorized with new permission(s).")
@Name("rptPermissionAuthorizationRestWebService")
public class RptPermissionAuthorizationWS {

    @Logger
    private Log log;
    @In
    private ErrorResponseFactory errorResponseFactory;
    @In
    private RPTManager rptManager;
    @In
    private ResourceSetPermissionManager resourceSetPermissionManager;
    @In
    private UmaValidationService umaValidationService;
    @In
    private AuthorizationService umaAuthorizationService;
    @In
    private FederationDataService federationDataService;
    @In
    private ClientService clientService;

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response requestRptPermissionAuthorization(
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("Host") String amHost,
            RptAuthorizationRequest rptAuthorizationRequest,
            @Context HttpServletRequest httpRequest) {
        try {
            final AuthorizationGrant grant = umaValidationService.validateAuthorizationWithAuthScope(authorization);

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
        if (Strings.isNullOrEmpty(rptAuthorizationRequest.getRpt())) {
            rpt = rptManager.createRPT(authorization, amHost);
        } else {
            rpt = rptManager.getRPTByCode(rptAuthorizationRequest.getRpt());
        }

        // Validate RPT
        try {
            umaValidationService.validateRPT(rpt);
        } catch(WebApplicationException e) {
            // according to latest UMA spec ( dated 2015-02-23 https://docs.kantarainitiative.org/uma/draft-uma-core.html)
            // it's up to impelementation whether to create new RPT for each request or pass back requests RPT.
            // Here we decided to pass back new RPT if request's RPT in invalid.
            rpt = rptManager.getRPTByCode(rptAuthorizationRequest.getRpt());
        }

        final ResourceSetPermission resourceSetPermission = resourceSetPermissionManager.getResourceSetPermissionByTicket(rptAuthorizationRequest.getTicket());

        // Validate resource set permission
        umaValidationService.validateResourceSetPermission(resourceSetPermission);

        final Boolean federationEnabled = ConfigurationFactory.instance().getConfiguration().getFederationEnabled();
        if (federationEnabled != null && federationEnabled) {
            final Client client = clientService.getClient(rpt.getClientId());
            final List<FederationTrust> trustList = federationDataService.getTrustByClient(client, FederationTrustStatus.ACTIVE);
            if (trustList != null && !trustList.isEmpty()) {
                for (FederationTrust t : trustList) {
                    final Boolean skipAuthorization = t.getSkipAuthorization();
                    if (skipAuthorization != null && skipAuthorization) {
                        // grant access directly, client is in trust and skipAuthorization=true
                        log.trace("grant access directly, client is in trust and skipAuthorization=true");
                        rptManager.addPermissionToRPT(rpt, resourceSetPermission);
                        return rpt;
                    }
                }
            } else {
                log.trace("Forbid RPT authorization - client is not in any trust however federation is enabled on server.");
                // throw not authorized exception
                throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                        .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.NOT_AUTHORIZED_PERMISSION)).build());

            }
        }

        // Add permission to RPT
        if (umaAuthorizationService.allowToAddPermission(grant, rpt, resourceSetPermission, httpRequest, rptAuthorizationRequest)) {
            rptManager.addPermissionToRPT(rpt, resourceSetPermission);
            return rpt;
        }

        // throw not authorized exception
        throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.NOT_AUTHORIZED_PERMISSION)).build());
    }
}
