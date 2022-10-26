/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.model.util.StringUtils;
import jakarta.ws.rs.core.Response;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.*;

import static io.jans.as.client.client.Asserter.assertRegisterResponseClaimsNotNull;
import static io.jans.as.model.register.RegisterRequestParam.*;

public class SsaRestWebServiceHttpTest extends BaseTest {

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void createSsaValid(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("createSsaValid");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<GrantType> grantTypes = Collections.singletonList(GrantType.CLIENT_CREDENTIALS);
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "ssa client test app", StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(scopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS, APPLICATION_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. access token
        String scope = SsaScopeType.SSA_ADMIN.getValue();
        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse = tokenClient.execClientCredentialsGrant(scope, clientId, clientSecret);

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse).ok().check();

        String accessToken = tokenResponse.getAccessToken();

        // 4. Ssa create
        SsaClient ssaClient = new SsaClient(ssaEndpoint);
        Long orgId = 1L;
        Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, 24);
        Long expirationDate = calendar.getTime().getTime() / 1000L;
        String description = "test description";
        String softwareId = "gluu-scan-api";
        List<String> softwareRoles = Collections.singletonList("passwurd");
        List<String> ssaGrantTypes = Collections.singletonList("client_credentials");
        SsaResponse ssaResponse = ssaClient.execSsaCreate(accessToken, orgId, expirationDate, description, softwareId, softwareRoles, ssaGrantTypes);

        showClient(ssaClient);
        AssertBuilder.ssaResponse(ssaClient.getRequest(), ssaResponse).created().check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void createSsaInvalidWithoutScopeAdmin(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("createSsaInvalidWithoutScopeAdmin");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<GrantType> grantTypes = Collections.singletonList(GrantType.CLIENT_CREDENTIALS);
        List<String> scopes = Collections.singletonList("openid");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "ssa client test app", StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(scopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS, APPLICATION_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. access token
        String scope = "";
        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse = tokenClient.execClientCredentialsGrant(scope, clientId, clientSecret);

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse).ok().check();

        String accessToken = tokenResponse.getAccessToken();

        // 4. Ssa create
        SsaClient ssaClient = new SsaClient(ssaEndpoint);
        Long orgId = 1L;
        Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, 24);
        Long expirationDate = calendar.getTime().getTime() / 1000L;
        String description = "test description";
        String softwareId = "gluu-scan-api";
        List<String> softwareRoles = Collections.singletonList("passwurd");
        List<String> ssaGrantTypes = Collections.singletonList("client_credentials");
        SsaResponse ssaResponse = ssaClient.execSsaCreate(accessToken, orgId, expirationDate, description, softwareId, softwareRoles, ssaGrantTypes);

        showClient(ssaClient);
        AssertBuilder.ssaResponse(ssaClient.getRequest(), ssaResponse).status(Response.Status.UNAUTHORIZED.getStatusCode()).check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void createSsaValidWithoutExpiration(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("createSsaValidWithoutExpiration");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<GrantType> grantTypes = Collections.singletonList(GrantType.CLIENT_CREDENTIALS);
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "ssa client test app", StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(scopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS, APPLICATION_TYPE, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. access token
        String scope = SsaScopeType.SSA_ADMIN.getValue();
        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse = tokenClient.execClientCredentialsGrant(scope, clientId, clientSecret);

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse).ok().check();

        String accessToken = tokenResponse.getAccessToken();

        // 4. Ssa create
        SsaClient ssaClient = new SsaClient(ssaEndpoint);
        Long orgId = 1L;
        String description = "test description";
        String softwareId = "gluu-scan-api";
        List<String> softwareRoles = Collections.singletonList("passwurd");
        List<String> ssaGrantTypes = Collections.singletonList("client_credentials");
        SsaResponse ssaResponse = ssaClient.execSsaCreate(accessToken, orgId, null, description, softwareId, softwareRoles, ssaGrantTypes);

        showClient(ssaClient);
        AssertBuilder.ssaResponse(ssaClient.getRequest(), ssaResponse).created().check();
    }
}