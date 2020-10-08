package org.gluu.oxauth.ws.rs;

import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 */
public class TokenBindingHttpTest extends BaseTest {

    public static final String ENCODED_TOKEN_BINDING_MESSAGE = "ARIAAgBBQCfsI1D1sTq5mvT_2H_dihNIvuHJCHGjHPJchPavNbGrOo26-2JgT_IsbvZd4daDFbirYBIwJ-TK1rh8FzrC-psAQO4Au9xPupLSkhwT9Y" +
            "n9aSvHXFsMLh4d4cEBKGP1clJtsfUFGDw-8HQSKwgKFN3WfZGq27y8NB3NAM1oNzvqVOIAAAECAEFArPIiuZxj9gK0dWhIcG63r2-sZ8V3LX9gpNl8Um_oGOtmwoP1v0VHNI" +
            "HEOzW3BOqcBLvUzVEG6a6KGEj3GrFcqQBA9YxqHPBIuDui_aQ1SoRGKyBEhaG2i-Wke3erRb1YwC7nTgrpqqJG3z1P8bt7cjZN6TpOyktdSSK7OJgiApwG7AAA";
    public static final String EXPECTED_ID_HASH = "suMuxh_IlrP-Zrj33LuQOQ5rX039cmBe-wt2df3BrUQ";

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void tokenBindingWithImplicitFlow(final String userId, final String userSecret,
                                            final String redirectUris, final String redirectUri,
                                            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenBindingWithImplicitFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN
        );
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUri, responseTypes, grantTypes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        Jwt jwt = Jwt.parse(authorizationResponse.getIdToken());
        Assert.assertEquals(EXPECTED_ID_HASH, jwt.getClaims().getClaimAsJSON(JwtClaimName.CNF).optString(JwtClaimName.TOKEN_BINDING_HASH));
    }

    private AuthorizationResponse requestAuthorization(final String userId, final String userSecret, final String redirectUri,
                                                       List<ResponseType> responseTypes, String clientId) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        return requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId, scopes);
    }

    private AuthorizationResponse requestAuthorization(
            final String userId, final String userSecret, final String redirectUri, List<ResponseType> responseTypes,
            String clientId, List<String> scopes) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setExecutor(new ApacheHttpClient4Executor(createHttpClientTrustAll()));
        authorizeClient.setRequest(authorizationRequest);
        authorizeClient.getHeaders().put("Sec-Token-Binding", ENCODED_TOKEN_BINDING_MESSAGE);

        AuthorizationResponse authorizationResponse = authorizeClient.exec();
        showClient(authorizeClient);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The access token is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getTokenType(), "The token type is null");
        assertNotNull(authorizationResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(authorizationResponse.getScope(), "The scope must be null");
        assertNotNull(authorizationResponse.getIdToken(), "The id token must be null");
        return authorizationResponse;
    }

    private RegisterResponse registerClient(final String redirectUris, final List<ResponseType> responseTypes,
                                            final List<GrantType> grantTypes, final String sectorIdentifierUri) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
        registerRequest.setIdTokenTokenBindingCnf(JwtClaimName.TOKEN_BINDING_HASH); // token binding hash for cnf

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setExecutor(new ApacheHttpClient4Executor(createHttpClientTrustAll()));
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        return registerResponse;
    }
}
