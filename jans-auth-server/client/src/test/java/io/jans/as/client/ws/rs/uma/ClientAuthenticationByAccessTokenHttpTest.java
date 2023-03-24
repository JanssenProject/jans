/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs.uma;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.uma.UmaClientFactory;
import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.client.uma.UmaTokenService;
import io.jans.as.client.uma.wrapper.UmaClient;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaNeedInfoResponse;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.util.Util;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.jans.as.test.UmaTestUtil.assertIt;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author yuriyz
 */
public class ClientAuthenticationByAccessTokenHttpTest extends BaseTest {

    public static final String REDIRECT_URI = "https://client.example.com/cb3";
    protected UmaMetadata metadata;

    protected RegisterResourceFlowHttpTest registerResourceTest;
    protected UmaRegisterPermissionFlowHttpTest permissionFlowTest;

    protected UmaRptIntrospectionService rptStatusService;
    protected UmaTokenService tokenService;

    protected Token pat;
    protected UmaNeedInfoResponse needInfo;

    protected String clientId;
    protected String clientSecret;
    protected String userAccessToken;

    @BeforeClass
    @Parameters({"umaMetaDataUrl", "umaPatClientId", "umaPatClientSecret"})
    public void init(final String umaMetaDataUrl, final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        this.metadata = UmaClientFactory.instance().createMetadataService(umaMetaDataUrl, clientEngine(true)).getMetadata();
        assertIt(metadata);

        pat = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret, clientEngine(true));
        assertIt(pat);

        this.registerResourceTest = new RegisterResourceFlowHttpTest(this.metadata);
        this.registerResourceTest.pat = this.pat;

        this.permissionFlowTest = new UmaRegisterPermissionFlowHttpTest(this.metadata);
        this.permissionFlowTest.registerResourceTest = this.registerResourceTest;

        this.rptStatusService = UmaClientFactory.instance().createRptStatusService(metadata, clientEngine(true));
        this.tokenService = UmaClientFactory.instance().createTokenService(metadata, clientEngine(true));
    }

    @Test
    public void requestClientRegistrationWithCustomAttributes() throws Exception {
        showTitle("requestClientRegistrationWithCustomAttributes");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", Collections.singletonList(REDIRECT_URI));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setExecutor(clientEngine(true));
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        clientId = response.getClientId();
        clientSecret = response.getClientSecret();
    }

    @Parameters({"userId", "userSecret"})
    @Test(dependsOnMethods = "requestClientRegistrationWithCustomAttributes")
    public void requestAccessTokenCustomClientAuth1(final String userId, final String userSecret) throws Exception {
        showTitle("requestAccessTokenCustomClientAuth1");

        // 1. Request authorization and receive the authorization code.
        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, REDIRECT_URI, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setExecutor(clientEngine(true));
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The code is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 2. Validate code and id_token
        AssertBuilder.jwtParse(idToken)
                .notNullAuthenticationTime()
                .claimsPresence(JwtClaimName.CODE_HASH)
                .check();

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(REDIRECT_URI);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setExecutor(clientEngine(true));
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();

        userAccessToken = tokenResponse.getAccessToken();
    }

    /**
     * Register resource
     */
    @Test(dependsOnMethods = "requestAccessTokenCustomClientAuth1")
    public void registerResource() throws Exception {
        showTitle("registerResource");
        this.registerResourceTest.addResource();
    }

    /**
     * RS registers permissions for specific resource.
     */
    @Test(dependsOnMethods = {"registerResource"})
    public void rsRegisterPermissions() throws Exception {
        showTitle("rsRegisterPermissions");
        permissionFlowTest.testRegisterPermission();
    }

    /**
     * RP requests RPT with ticket and gets needs_info error (not all claims are provided, so redirect to claims-gathering endpoint)
     */
    @Test(dependsOnMethods = {"rsRegisterPermissions"})
    public void requestRptAndGetNeedsInfo() throws Exception {
        showTitle("requestRptAndGetNeedsInfo");

        try {
            tokenService.requestRpt(
                    "AccessToken " + userAccessToken,
                    GrantType.OXAUTH_UMA_TICKET.getValue(),
                    permissionFlowTest.ticket,
                    null, null, null, null, null);
        } catch (ClientErrorException ex) {
            // expected need_info error :
            // sample:  {"error":"need_info","ticket":"c024311b-f451-41db-95aa-cd405f16eed4","required_claims":[{"issuer":["https://localhost:8443"],"name":"country","claim_token_format":["http://openid.net/specs/openid-connect-core-1_0.html#IDToken"],"claim_type":"string","friendly_name":"country"},{"issuer":["https://localhost:8443"],"name":"city","claim_token_format":["http://openid.net/specs/openid-connect-core-1_0.html#IDToken"],"claim_type":"string","friendly_name":"city"}],"redirect_user":"https://localhost:8443/restv1/uma/gather_claimsgathering_id=sampleClaimsGathering&&?gathering_id=sampleClaimsGathering&&"}
            String entity = ex.getResponse().readEntity(String.class);
            System.out.println(entity);

            assertEquals(ex.getResponse().getStatus(), Response.Status.FORBIDDEN.getStatusCode(), "Unexpected response status");

            needInfo = Util.createJsonMapper().readValue(entity, UmaNeedInfoResponse.class);
            assertIt(needInfo);
            return;
        }

        // Expected result is to get need_info error. It means that client was authenticated successfully.
        // If client fails to authenticate then we will get `401` invalid client error.
        throw new AssertionError("need_info error was not returned");
    }
}