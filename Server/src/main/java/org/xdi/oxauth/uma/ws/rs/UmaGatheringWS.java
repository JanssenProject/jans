package org.xdi.oxauth.uma.ws.rs;

import org.slf4j.Logger;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.uma.authorization.UmaWebException;
import org.xdi.oxauth.uma.service.UmaValidationService;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author yuriyz on 06/04/2017.
 */
@Path("/uma/gather_claims")
public class UmaGatheringWS {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private UmaValidationService umaValidationService;

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response gatherClaims(
            @FormParam("client_id")
            String clientId,
            @FormParam("ticket")
            String ticket,
            @FormParam("claims_redirect_uri")
            String claimRedirectUri,
            @FormParam("state")
            String state,
            @Context HttpServletRequest httpRequest) {
        try {
            log.trace("gatherClaims client_id: {}, ticket: {}, claims_redirect_uri: {}, state: {}, queryString: {}"
                    , clientId, ticket, claimRedirectUri, state, httpRequest.getQueryString());

            Client client = umaValidationService.validateClientAndClaimsRedirectUri(clientId, claimRedirectUri, state);
            List<UmaPermission> permissions = umaValidationService.validateTicketWithRedirect(ticket, claimRedirectUri, state);


            // todo

            String redirectUri = claimRedirectUri;
            return Response.status(Response.Status.FOUND)
                    .location(URI.create(redirectUri))
                    .build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }
        }

        log.error("Failed to handle to UMA Claims Gathering Endpoint.");
        throw new UmaWebException(Response.Status.INTERNAL_SERVER_ERROR, errorResponseFactory, UmaErrorResponseType.SERVER_ERROR);
    }
}
