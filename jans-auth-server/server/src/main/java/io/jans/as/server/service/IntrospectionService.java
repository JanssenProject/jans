package io.jans.as.server.service;

import io.jans.as.common.claims.Audience;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.config.Constants;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.token.JwtSigner;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */

@Named
public class IntrospectionService {

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private Logger log;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private ClientService clientService;

    public boolean isJwtResponse(String responseAsJwt, String acceptHeader) {
        return Boolean.TRUE.toString().equalsIgnoreCase(responseAsJwt) ||
                Constants.APPLICATION_TOKEN_INTROSPECTION_JWT.equalsIgnoreCase(acceptHeader);
    }

    public JwtSigner createResponseJwt(JSONObject response, AuthorizationGrant grant) throws Exception {
        final Client client = grant.getClient();
        final JwtSigner jwtSigner = JwtSigner.newJwtSigner(appConfiguration, webKeysConfiguration, client, clientService.decryptSecret(client.getClientSecret()));
        final Jwt jwt = jwtSigner.newJwt();
        fillPayload(jwt, response, grant);
        return jwtSigner;
    }

    public void fillPayload(Jwt jwt, JSONObject response, AuthorizationGrant grant) throws InvalidJwtException {
        final Client client = grant.getClient();
        Audience.setAudience(jwt.getClaims(), client);
        jwt.getClaims().setIssuer(appConfiguration.getIssuer());
        jwt.getClaims().setIatNow();

        try {
            jwt.getClaims().setClaim("token_introspection", response);
        } catch (Exception e) {
            log.error("Failed to put claims into jwt. Key: token_introspection, response: " + response.toString(), e);
        }

        if (log.isTraceEnabled()) {
            log.trace("Response before signing: {}", jwt.getClaims().toJsonString());
        }
    }

    public String createResponseAsJwt(JSONObject response, AuthorizationGrant grant) throws Exception {
        return createResponseJwt(response, grant).sign().toString();
    }
}
