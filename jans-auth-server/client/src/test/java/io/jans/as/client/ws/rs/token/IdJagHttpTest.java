package io.jans.as.client.ws.rs.token;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.*;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.model.config.Constants.TOKEN_TYPE_ID_JAG;
import static io.jans.as.model.config.Constants.SUBJECT_TOKEN_TYPE_ID_TOKEN;
import static org.testng.AssertJUnit.*;

/**
 * HTTP integration test for the Identity Assertion Authorization Grant (ID-JAG).
 * <p>
 * Tests the inline deployment scenario where the same AS acts as both IdP and Resource AS:
 *   Step 1 — Token Exchange: client presents an id_token → AS issues an ID-JAG
 *   Step 2 — JWT Bearer:     client presents the ID-JAG  → AS issues an access token
 * <p>
 * Prerequisites (server config):
 *   - IDENTITY_ASSERTION_AUTHZ_GRANT feature flag enabled
 *   - token-exchange and jwt-bearer in grantTypesSupported and grantTypesSupportedByDynamicRegistration
 *
 * @author Yuriy Z
 */
public class IdJagHttpTest extends BaseTest {

    /**
     * Full inline ID-JAG flow.
     */
    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri"})
    @Test
    public void idJagInlineFlow(
            final String userId, final String userSecret,
            final String redirectUris, final String redirectUri) {
        showTitle("idJagInlineFlow");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.AUTHORIZATION_CODE, GrantType.TOKEN_EXCHANGE, GrantType.JWT_BEARER);
        List<String> scopes = Arrays.asList("openid", "profile", "email");

        // 1. Register client
        RegisterResponse registerResponse = registerIdJagClient(redirectUris, responseTypes, grantTypes, scopes);
        assertTrue(registerResponse.getGrantTypes().contains(GrantType.TOKEN_EXCHANGE));
        assertTrue(registerResponse.getGrantTypes().contains(GrantType.JWT_BEARER));

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Obtain ID token via authorization code flow
        String idToken = requestIdToken(userId, userSecret, redirectUri, responseTypes, scopes, clientId, clientSecret);

        // 3. Step 1 of ID-JAG: exchange id_token for an ID-JAG
        TokenRequest idJagRequest = new TokenRequest(GrantType.TOKEN_EXCHANGE);
        idJagRequest.setSubjectToken(idToken);
        idJagRequest.setSubjectTokenType(SUBJECT_TOKEN_TYPE_ID_TOKEN);
        idJagRequest.setRequestedTokenType(TOKEN_TYPE_ID_JAG);
        idJagRequest.setAudience(issuer); // same server — inline scenario
        idJagRequest.setScope(StringUtils.implode(scopes, " "));
        idJagRequest.setAuthUsername(clientId);
        idJagRequest.setAuthPassword(clientSecret);
        idJagRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient idJagTokenClient = newTokenClient(idJagRequest);
        TokenResponse idJagResponse = idJagTokenClient.exec();
        showClient(idJagTokenClient);

        assertNotNull(idJagResponse.getAccessToken());
        assertEquals(200, idJagResponse.getStatus());
        assertEquals(ExchangeTokenType.ID_JAG, idJagResponse.getIssuedTokenType());
        assertEquals(TokenType.N_A, idJagResponse.getTokenType());

        String idJag = idJagResponse.getAccessToken();

        // 4. Step 2 of ID-JAG: present ID-JAG as jwt-bearer assertion to get an access token
        TokenRequest atRequest = new TokenRequest(GrantType.JWT_BEARER);
        atRequest.setAssertion(idJag);
        atRequest.setScope(StringUtils.implode(scopes, " "));
        atRequest.setAuthUsername(clientId);
        atRequest.setAuthPassword(clientSecret);
        atRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient atTokenClient = newTokenClient(atRequest);
        TokenResponse atResponse = atTokenClient.exec();
        showClient(atTokenClient);

        AssertBuilder.tokenResponse(atResponse).status(200).check();
    }

    private String requestIdToken(String userId, String userSecret, String redirectUri,
                                  List<ResponseType> responseTypes, List<String> scopes,
                                  String clientId, String clientSecret) {
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authRequest.setState(state);

        AuthorizationResponse authResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authRequest, userId, userSecret);
        AssertBuilder.authorizationResponse(authResponse).check();

        String authCode = authResponse.getCode();
        assertNotNull(authCode);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = newTokenClient(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();
        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse).status(200).check();

        assertNotNull(tokenResponse.getIdToken());
        return tokenResponse.getIdToken();
    }

    private RegisterResponse registerIdJagClient(String redirectUris, List<ResponseType> responseTypes,
                                                 List<GrantType> grantTypes, List<String> scopes) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app: id-jag",
                io.jans.as.model.util.StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setScope(scopes);
        registerRequest.setSubjectType(SubjectType.PUBLIC);

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();
        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();
        return registerResponse;
    }
}
