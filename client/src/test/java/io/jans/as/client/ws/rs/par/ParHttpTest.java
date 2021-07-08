package io.jans.as.client.ws.rs.par;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.par.ParClient;
import io.jans.as.client.par.ParRequest;
import io.jans.as.client.par.ParResponse;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.assertAuthorizationResponse;
import static io.jans.as.client.client.Asserter.assertOk;
import static io.jans.as.client.client.Asserter.assertParResponse;
import static io.jans.as.client.client.Asserter.assertTokenResponse;
import static io.jans.as.client.client.Asserter.validateIdToken;

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
        parRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        parRequest.setAuthUsername(registerResponse.getClientId());
        parRequest.setAuthPassword(registerResponse.getClientSecret());

        ParClient parClient = newParClient(parRequest);
        parResponse = parClient.exec();
        showClient(parClient);
        assertParResponse(parResponse);
    }

    @Parameters({"userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "registerPar")
    public void requestAuthorizationWithPar(final String userId, final String userSecret, String redirectUri) throws InvalidJwtException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri);

        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(registerResponse.getClientId());
        tokenRequest.setAuthPassword(registerResponse.getClientSecret());
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = newTokenClient(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertTokenResponse(tokenResponse);
        validateIdToken(idToken, jwksUri, SignatureAlgorithm.RS256);
    }

    private AuthorizationResponse requestAuthorization(final String userId, final String userSecret, String redirectUri) {
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(parResponse.getRequestUri());
        authorizationRequest.setState(UUID.randomUUID().toString());
        authorizationRequest.setClientId(registerResponse.getClientId());
        authorizationRequest.setRedirectUri(redirectUri); // NOT USED in real scenario. This is needed only to check whether finish selenium authorization or not.

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertAuthorizationResponse(authorizationResponse);
        return authorizationResponse;
    }
}
