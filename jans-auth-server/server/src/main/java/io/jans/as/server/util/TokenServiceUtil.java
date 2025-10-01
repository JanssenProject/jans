package io.jans.as.server.util;

import io.jans.as.client.TokenResponse;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.server.model.common.AccessToken;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.common.ClientCredentialsGrant;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.service.cdi.util.CdiUtil;

/**
 * @author Yuriy Z
 */
public class TokenServiceUtil {

    private TokenServiceUtil() {
    }

    public static TokenResponse createClientCredentialsAccessTokenWithoutScript(Client client, String scope, ExecutionContext inputContext) {
        inputContext.setSkipModifyAccessTokenScript(true);

        return createClientCredentialsAccessToken(client, scope, inputContext);
    }

    public static TokenResponse createClientCredentialsAccessToken(Client client, String scope, ExecutionContext inputContext) {
        final ExecutionContext executionContext = inputContext.copy();
        executionContext.setClient(client);

        final AuthorizationGrantList authorizationGrantList = CdiUtil.bean(AuthorizationGrantList.class);

        final ClientCredentialsGrant clientCredentialsGrant = authorizationGrantList.createClientCredentialsGrant(new User(), client);
        scope = clientCredentialsGrant.checkScopesPolicy(scope);

        executionContext.setGrant(clientCredentialsGrant);

        final AccessToken accessToken = clientCredentialsGrant.createAccessToken(executionContext); // create token after scopes are checked

        final TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(accessToken.getCode());
        tokenResponse.setTokenType(accessToken.getTokenType());
        tokenResponse.setExpiresIn(accessToken.getExpiresIn());
        tokenResponse.setScope(scope);
        return tokenResponse;
    }
}
