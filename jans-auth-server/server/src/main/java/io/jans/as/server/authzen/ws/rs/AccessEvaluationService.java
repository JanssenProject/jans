package io.jans.as.server.authzen.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.external.ExternalAccessEvaluationService;
import io.jans.as.server.service.token.TokenService;
import io.jans.model.authzen.AccessEvaluationRequest;
import io.jans.model.authzen.AccessEvaluationResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class AccessEvaluationService {

    public static final String ACCESS_EVALUATION_SCOPE = "access_evaluation";

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private ExternalAccessEvaluationService externalAccessEvaluationService;

    @Inject
    private AccessEvaluationValidator accessEvaluationValidator;

    @Inject
    private TokenService tokenService;

    @Inject
    private ClientService clientService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ScopeService scopeService;

    public AccessEvaluationResponse evaluation(AccessEvaluationRequest request, ExecutionContext executionContext) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);

        accessEvaluationValidator.validateAccessEvaluationRequest(request);

        final AccessEvaluationResponse response = externalAccessEvaluationService.externalEvaluate(request, executionContext);

        log.debug("Access Evaluation response {}", response);
        return response;
    }

    public void validateAuthorization(String authorization) {
        AuthorizationGrant grant = tokenService.getBearerAuthorizationGrant(authorization);
        if (grant != null) {
            final String authorizationAccessToken = tokenService.getBearerToken(authorization);
            final AbstractToken accessTokenObject = grant.getAccessToken(authorizationAccessToken);
            if (accessTokenObject != null && accessTokenObject.isValid()) {
                if (grant.getScopes() != null && grant.getScopes().contains(ACCESS_EVALUATION_SCOPE)) {
                    log.debug("Authorized with bearer token.");
                    return;
                } else {
                    log.error("access_token does not have {} scope.", ACCESS_EVALUATION_SCOPE);
                }
            } else {
                log.debug("Unable to find valid access token.");
            }
        } else {
            log.debug("Unable to find grant by bearer access token.");
        }

        if (isTrue(appConfiguration.getAccessEvaluationAllowBasicClientAuthorization()) && tokenService.isBasicAuthToken(authorization)) {
            log.debug("Trying to perform basic client authorization ...");
            String encodedCredentials = tokenService.getBasicToken(authorization);

            String token = new String(Base64.decodeBase64(encodedCredentials), StandardCharsets.UTF_8);

            int delim = token.indexOf(":");

            if (delim != -1) {
                String clientId = URLDecoder.decode(token.substring(0, delim), StandardCharsets.UTF_8);
                String password = URLDecoder.decode(token.substring(delim + 1), StandardCharsets.UTF_8);
                if (clientService.authenticate(clientId, password)) {
                    log.debug("Authorized with basic client authentication successfully. client_id: {}", clientId);

                    final Client client = clientService.getClient(clientId);
                    List<String> clientScopes = scopeService.getScopeIdsByDns(client.getScopes() != null ? Arrays.asList(client.getScopes()) : new ArrayList<>());
                    if (clientScopes.contains(ACCESS_EVALUATION_SCOPE)) {
                        log.debug("Granted access to /evaluation endpoint. Client {} has scope {}.", clientId, ACCESS_EVALUATION_SCOPE);
                        return;
                    } else {
                        log.debug("Access denied to /evaluation endpoint. Client {} has no scope {}.", clientId, ACCESS_EVALUATION_SCOPE);
                    }
                }
            }
            log.debug("Unable to perform basic client authorization.");
        }

        final String msg = "Authorization is not valid. Please provide valid authorization in 'Authorization' header.";
        log.error(msg);
        throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(msg)
                .build());
    }
}
