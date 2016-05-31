package org.xdi.oxd.client;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxd.common.CoreUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class TestUtils {

    private TestUtils() {
    }

    public static OpenIdConfigurationResponse discovery(String opHost) {
        try {
            final OpenIdConfigurationClient client = new OpenIdConfigurationClient(opHost + "/.well-known/openid-configuration");
            client.setExecutor(new ApacheHttpClient4Executor(CoreUtils.createHttpClientTrustAll()));
            return client.execOpenIdConfiguration();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static TokenResponse obtainAccessToken(String userId, String userSecret, String clientId, String clientSecret,
                                                  String redirectUrl, String opHost) {
        try {
            OpenIdConfigurationResponse discovery = discovery(opHost);

            // 1. Request authorization and receive the authorization code.
            final List<ResponseType> responseTypes = new ArrayList<ResponseType>();
            responseTypes.add(ResponseType.CODE);
            responseTypes.add(ResponseType.ID_TOKEN);
            final List<String> scopes = new ArrayList<String>();
            scopes.add("openid");

            final AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUrl, null);
            request.setState("af0ifjsldkj");
            request.setAuthUsername(userId);
            request.setAuthPassword(userSecret);
            request.getPrompts().add(Prompt.NONE);
            request.setNonce(UUID.randomUUID().toString());
            request.setMaxAge(Integer.MAX_VALUE);

            final AuthorizeClient authorizeClient = new AuthorizeClient(discovery.getAuthorizationEndpoint());
            authorizeClient.setRequest(request);
            final ApacheHttpClient4Executor clientExecutor = new ApacheHttpClient4Executor(CoreUtils.createHttpClientTrustAll());
            final AuthorizationResponse response1 = authorizeClient.exec(clientExecutor);

            ClientUtils.showClient(authorizeClient);

            final String scope = response1.getScope();
            final String authorizationCode = response1.getCode();

            if (Util.allNotBlank(authorizationCode)) {

                // 2. Request access token using the authorization code.
                final TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
                tokenRequest.setCode(authorizationCode);
                tokenRequest.setRedirectUri(redirectUrl);
                tokenRequest.setAuthUsername(clientId);
                tokenRequest.setAuthPassword(clientSecret);
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
                tokenRequest.setScope(scope);

                final TokenClient tokenClient1 = new TokenClient(discovery.getTokenEndpoint());
                tokenClient1.setExecutor(clientExecutor);
                tokenClient1.setRequest(tokenRequest);
                final TokenResponse response2 = tokenClient1.exec();

                ClientUtils.showClient(tokenClient1);
                if (response2.getStatus() == 200) {
                    return response2;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return null;
    }

    public static void notEmpty(String str) {
        assertTrue(StringUtils.isNotBlank(str));
    }

    public static void notEmpty(List<String> str) {
        assertTrue(str != null && !str.isEmpty() && StringUtils.isNotBlank(str.get(0)));
    }
}
