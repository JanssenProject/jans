package io.jans.as.client.ws.rs;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static io.jans.as.model.register.RegisterRequestParam.*;

/**
 * Integration tests to validate redirect uris regex behavior
 *
 */
public class AuthorizationRedirectUrisRegexTest extends BaseTest {
    
    @Parameters({"userId", "userSecret", "redirectUris", "sectorIdentifierUri", "redirectUrisRegex", "redirectUri"})
    @Test
    public void requestClientValidateUsingRedirectUrisRegex( final String userId, final String userSecret,
                                                final String redirectUris, final String sectorIdentifierUri,
                                                final String redirectUrisRegex, final String redirectUri) {
        showTitle("requestClientValidateUsingRedirectUrisRegex");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setRedirectUrisRegex(redirectUrisRegex);
        registerRequest.setRedirectUris(getRedirectUris());

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created()
                .notNullRegistrationClientUri()
                .check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization and receive the authorization code.
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);
        AssertBuilder.authorizationResponse(authorizationResponse).check();

    }

    private List<String> getRedirectUris() {
        return Arrays.asList("https://www.jans.org",
                "http://localhost:80/jans-auth-rp/home.htm",
                "https://localhost:8443/jans-auth-rp/home.htm",
                "https://client.example.org/callback",
                "https://client.example.org/callback2",
                "https://client.other_company.example.net/callback",
                "https://client.example.com/cb",
                "https://client.example.com/cb1",
                "https://client.example.com/cb2") ;
    }

}
