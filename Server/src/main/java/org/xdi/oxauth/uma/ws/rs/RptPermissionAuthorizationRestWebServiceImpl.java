package org.xdi.oxauth.uma.ws.rs;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

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
import org.xdi.oxauth.model.uma.AuthorizationResponse;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.FederationDataService;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.service.uma.RPTManager;
import org.xdi.oxauth.service.uma.ResourceSetPermissionManager;
import org.xdi.oxauth.service.uma.UmaValidationService;
import org.xdi.oxauth.service.uma.authorization.AuthorizationService;
import org.xdi.oxauth.util.ServerUtil;

/**
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 10/25/2012
 */
@Name("rptPermissionAuthorizationRestWebService")
public class RptPermissionAuthorizationRestWebServiceImpl implements RptPermissionAuthorizationRestWebService {

    @Logger
    private Log log;
    @In
    private TokenService tokenService;
    @In
    private ErrorResponseFactory errorResponseFactory;
    //	@In
//	private AuthorizationGrantList authorizationGrantList;
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

    public Response requestRptPermissionAuthorization(String authorization, String amHost, RptAuthorizationRequest rptAuthorizationRequest, HttpServletRequest httpRequest) {
        try {
            umaValidationService.validateAuthorizationWithAuthScope(authorization);

            final String validatedAmHost = umaValidationService.validateAmHost(amHost);
            final AuthorizationGrant authorizationGrant = tokenService.getAuthorizationGrant(authorization);
            authorizeRptPermission(authorizationGrant, validatedAmHost, rptAuthorizationRequest, httpRequest);

            // convert manually to avoid possible conflict between resteasy providers, e.g. jettison, jackson
            final String entity = ServerUtil.asJson(new AuthorizationResponse(Response.Status.OK.getReasonPhrase()));
            return Response.ok(entity).build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }

    private ResourceSetPermission authorizeRptPermission(AuthorizationGrant authorizationGrant, String amHost, RptAuthorizationRequest rptAuthorizationRequest, HttpServletRequest httpRequest) {
        UmaRPT rpt = rptManager.getRPTByCode(rptAuthorizationRequest.getRpt());

        // Validate RPT
        umaValidationService.validateRPT(rpt);

        final ResourceSetPermission resourceSetPermission = resourceSetPermissionManager.getResourceSetPermissionByTicket(rptAuthorizationRequest.getTicket());

        // Validate resource set permission
        umaValidationService.validateResourceSetPermission(resourceSetPermission);

        final Boolean federationEnabled = ConfigurationFactory.getConfiguration().getFederationEnabled();
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
                        return resourceSetPermission;
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
        if (umaAuthorizationService.allowToAddPermission(authorizationGrant, rpt, resourceSetPermission, httpRequest, rptAuthorizationRequest)) {
            rptManager.addPermissionToRPT(rpt, resourceSetPermission);
            return resourceSetPermission;
        }

        // throw not authorized exception
        throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.NOT_AUTHORIZED_PERMISSION)).build());
    }
}
