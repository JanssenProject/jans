package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.service.ScopeService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

import javax.inject.Named;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author Yuriy Z
 */
@RequestScoped
@Named
public class AuthorizationChallengeValidator {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ScopeService scopeService;

    public void validateGrantType(Client client, String state) {
        if (client == null) {
            final String msg = "Unable to find client.";
            log.debug(msg);
            throw new WebApplicationException(errorResponseFactory
                    .newErrorResponse(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state, msg))
                    .build());
        }

        if (client.getGrantTypes() == null || !Arrays.asList(client.getGrantTypes()).contains(GrantType.AUTHORIZATION_CODE)) {
            String msg = String.format("Client %s does not support grant_type=authorization_code", client.getClientId());
            log.debug(msg);
            throw new WebApplicationException(errorResponseFactory
                    .newErrorResponse(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state, msg))
                    .build());
        }

        final Set<GrantType> grantTypesSupported = appConfiguration.getGrantTypesSupported();
        if (grantTypesSupported == null || !grantTypesSupported.contains(GrantType.AUTHORIZATION_CODE)) {
            String msg = "AS configuration does not allow grant_type=authorization_code";
            log.debug(msg);
            throw new WebApplicationException(errorResponseFactory
                    .newErrorResponse(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state, msg))
                    .build());
        }
    }

    public void validateAccess(Client client) {
        if (client == null || ArrayUtils.isEmpty(client.getScopes())) {
            log.debug("Client is null or have no scopes defined. Rejected request.");
            throw new WebApplicationException(
                    Response.status(Response.Status.UNAUTHORIZED.getStatusCode())
                            .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST))
                            .build());
        }

        List<String> scopesAllowedIds = scopeService.getScopeIdsByDns(Arrays.asList(client.getScopes()));

        if (!scopesAllowedIds.contains(Constants.AUTHORIZATION_CHALLENGE_SCOPE)) {
            log.debug("Client does not have required 'authorization_challenge' scope.");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED.getStatusCode())
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST))
                    .build());
        }
    }
}
