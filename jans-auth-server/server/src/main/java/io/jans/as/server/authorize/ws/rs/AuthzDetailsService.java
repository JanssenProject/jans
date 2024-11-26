package io.jans.as.server.authorize.ws.rs;

import io.jans.as.model.authzdetails.AuthzDetail;
import io.jans.as.model.authzdetails.AuthzDetails;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.error.IErrorType;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.ExternalAuthzDetailTypeService;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Yuriy Z
 */
@Stateless
@Named
public class AuthzDetailsService {

    @Inject
    private Logger log;

    @Inject
    private ExternalAuthzDetailTypeService externalAuthzDetailTypeService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    public AuthzDetails validateAuthorizationDetails(String authorizationDetailsString, ExecutionContext executionContext) {
        if (StringUtils.isBlank(authorizationDetailsString)) {
            return null; // nothing to validate
        }

        // 1. check whether authz details is valid json and can be parsed
        final AuthzDetails authzDetails = AuthzDetails.ofSilently(authorizationDetailsString);
        if (authzDetails == null) {
            log.debug("Unable to parse 'authorization_details' {}", authorizationDetailsString);
            throw new WebApplicationException(error(400, TokenErrorResponseType.INVALID_AUTHORIZATION_DETAILS,
                    "Unable to parse 'authorization_details'").build());
        }

        if (authzDetails.getDetails() == null || authzDetails.getDetails().isEmpty()) {
            return null; // nothing to validate
        }

        final Set<String> requestAuthzDetailsTypes = authzDetails.getTypes();

        // 2. check whether authorization_details type is supported globally by AS
        final Set<String> supportedAuthzDetailsTypes = externalAuthzDetailTypeService.getSupportedAuthzDetailsTypes();
        if (!supportedAuthzDetailsTypes.containsAll(requestAuthzDetailsTypes)) {
            log.debug("Not all authorization_details type are supported. Requested {}. AS supports: {}", requestAuthzDetailsTypes, supportedAuthzDetailsTypes);

            throw new WebApplicationException(error(400, TokenErrorResponseType.INVALID_AUTHORIZATION_DETAILS,
                    "Found not supported 'authorization_details' type.").build());
        }

        // 3. check whether authorization_details type is supported by client
        final Client client = executionContext.getClient();
        if (!client.getAttributes().getAuthorizationDetailsTypes().containsAll(requestAuthzDetailsTypes)) {
            log.debug("Client does not support all authorization_details types' {}. Client supports {}",
                    requestAuthzDetailsTypes, client.getAttributes().getAuthorizationDetailsTypes());

            throw new WebApplicationException(error(400, TokenErrorResponseType.UNAUTHORIZED_CLIENT,
                    "Client does not support authorization_details type'").build());
        }

        // 4. external script validation
        executionContext.setAuthzDetails(authzDetails);
        externalAuthzDetailTypeService.externalValidateAuthzDetails(executionContext);
        return authzDetails;
    }

    public Response.ResponseBuilder error(int status, IErrorType type, String reason) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(type, reason));
    }

    public AuthzDetails checkAuthzDetails(AuthzDetails requestedAuthzDetails, final AuthzDetails authorizedDetails) {
        if (AuthzDetails.isEmpty(authorizedDetails) || AuthzDetails.isEmpty(requestedAuthzDetails)) {
            return null;
        }

        List<AuthzDetail> grantedDetails = new ArrayList<>();

        for (AuthzDetail authorized : authorizedDetails.getDetails()) {
            for (AuthzDetail requested : requestedAuthzDetails.getDetails()) {
                if (authorized.getJsonObject().similar(requested.getJsonObject()) && !grantedDetails.contains(authorized)) {
                    grantedDetails.add(authorized);
                    break;
                }
            }
        }

        return new AuthzDetails(grantedDetails);
    }

    public AuthzDetails checkAuthzDetailsAndSave(AuthzDetails requestedAuthzDetails, AuthorizationGrant authorizationGrant) {
        final AuthzDetails granted = checkAuthzDetails(requestedAuthzDetails, authorizationGrant.getAuthzDetails());
        authorizationGrant.setAuthzDetails(granted);
        authorizationGrant.save();

        return granted;
    }
}
