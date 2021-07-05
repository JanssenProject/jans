package io.jans.as.client.ws.rs.par;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.par.ParClient;
import io.jans.as.client.par.ParRequest;
import io.jans.as.client.par.ParResponse;
import io.jans.as.model.common.ResponseType;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.assertOk;
import static io.jans.as.client.client.Asserter.assertParResponse;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ParHttpTest extends BaseTest {

    private RegisterResponse registerResponse;
    private ParResponse parResponse;

    @Parameters({"redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void registerPar(final String redirectUris, final String redirectUri, final String sectorIdentifierUri) {
        showTitle("registerPar");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");
        String nonce = UUID.randomUUID().toString();

        registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);
        assertOk(registerResponse);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, registerResponse.getClientId(), scopes, redirectUri, nonce);
        ParRequest parRequest = new ParRequest(authorizationRequest);

        ParClient parClient = newParClient(parRequest);
        parResponse = parClient.exec();
        showClient(parClient);
        assertParResponse(parResponse);
    }

    @Test(dependsOnMethods = "registerPar")
    public void requestAuthorizationWithPar(final String userId, final String userSecret) {
//        // 2. Request authorization and receive the authorization code.
//
//        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);
//
//        String scope = authorizationResponse.getScope();
//        String authorizationCode = authorizationResponse.getCode();
//        String idToken = authorizationResponse.getIdToken();
//
//        // 3. Request access token using the authorization code.
//        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
//        tokenRequest.setCode(authorizationCode);
//        tokenRequest.setRedirectUri(redirectUri);
//        tokenRequest.setAuthUsername(clientId);
//        tokenRequest.setAuthPassword(clientSecret);
//        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
//
//        TokenClient tokenClient1 = newTokenClient(tokenRequest);
//        tokenClient1.setRequest(tokenRequest);
//        TokenResponse tokenResponse1 = tokenClient1.exec();
//
//        showClient(tokenClient1);
//        assertEquals(tokenResponse1.getStatus(), 200, "Unexpected response code: " + tokenResponse1.getStatus());
//        assertNotNull(tokenResponse1.getEntity(), "The entity is null");
//        assertNotNull(tokenResponse1.getAccessToken(), "The access token is null");
//        assertNotNull(tokenResponse1.getExpiresIn(), "The expires in value is null");
//        assertNotNull(tokenResponse1.getTokenType(), "The token type is null");
//        assertNotNull(tokenResponse1.getRefreshToken(), "The refresh token is null");
//
//        String refreshToken = tokenResponse1.getRefreshToken();
//
//        // 4. Validate id_token
//        Jwt jwt = Jwt.parse(idToken);
//        Asserter.assertIdToken(jwt, JwtClaimName.CODE_HASH);
//
//        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(
//                jwksUri,
//                jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID), clientExecutor(true));
//        RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.RS256, publicKey);
//
//        assertTrue(rsaSigner.validate(jwt));
    }

}
