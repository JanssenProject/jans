package io.jans.as.client.ws.rs;

import io.jans.as.client.*;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Turn off consent for pairwise / openid-only scope
 *
 * @author Javier Rojas Blum
 * @version January 24, 2022
 */
public class TurnOffConsentForPairwiseOpenIdOnlyConsentTest extends BaseTest {

    /**
     * If a client is configured for pairwise identifiers, and the openid scope is the only scope requested,
     * there is no need to present the consent page, because the AS is not releasing any PII.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void turnOffConsentForPairwiseOpenIdOnlyConsentTest(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) {
        showTitle("turnOffConsentForPairwiseOpenIdOnlyConsentTest");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS256);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Authorization
        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwner(
                authorizationEndpoint, authorizationRequest, userId, userSecret, false);

        assertNotNull(authorizationResponse.getLocation());
        assertNotNull(authorizationResponse.getAccessToken());
        assertNotNull(authorizationResponse.getState());
        assertNotNull(authorizationResponse.getTokenType());
        assertNotNull(authorizationResponse.getExpiresIn());
        assertNotNull(authorizationResponse.getScope());
        assertNotNull(authorizationResponse.getIdToken());

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200);
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ISSUER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.AUDIENCE));
    }
}
