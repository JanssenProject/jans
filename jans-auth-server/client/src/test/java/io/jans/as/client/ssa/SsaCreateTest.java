/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa;

import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.ssa.create.SsaCreateClient;
import io.jans.as.client.ssa.create.SsaCreateResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.model.util.DateUtil;
import org.apache.http.HttpStatus;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.*;

public class SsaCreateTest extends BaseTest {

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void createSsaValid(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("createSsaValid");
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // Register client
        RegisterResponse registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // Access token
        TokenResponse tokenResponse = tokenClientCredentialsGrant(SsaScopeType.SSA_ADMIN.getValue(), clientId, clientSecret);
        String accessToken = tokenResponse.getAccessToken();

        // Ssa create
        SsaCreateClient ssaCreateClient = new SsaCreateClient(ssaEndpoint);
        String orgId = "org-id-test";
        Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, 24);
        Long expirationDate = DateUtil.dateToUnixEpoch(calendar.getTime());
        String description = "test description";
        String softwareId = "gluu-scan-api";
        Integer lifetime = 86400;
        List<String> softwareRoles = Collections.singletonList("password");
        List<String> ssaGrantTypes = Collections.singletonList("client_credentials");
        SsaCreateResponse ssaCreateResponse = ssaCreateClient.execSsaCreate(accessToken, orgId, expirationDate, description,
                softwareId, softwareRoles, ssaGrantTypes, Boolean.TRUE, Boolean.TRUE, lifetime);

        showClient(ssaCreateClient);
        AssertBuilder.ssaCreate(ssaCreateClient.getRequest(), ssaCreateResponse)
                .check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void createSsaInvalidWithoutScopeAdmin(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("createSsaInvalidWithoutScopeAdmin");
        List<String> scopes = Collections.singletonList("openid");

        // Register client
        RegisterResponse registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // Access token
        TokenResponse tokenResponse = tokenClientCredentialsGrant("", clientId, clientSecret);
        String accessToken = tokenResponse.getAccessToken();

        // Ssa create
        SsaCreateClient ssaCreateClient = new SsaCreateClient(ssaEndpoint);
        String orgId = "org-id-test";
        Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, 24);
        Long expirationDate = DateUtil.dateToUnixEpoch(calendar.getTime());
        String description = "test description";
        String softwareId = "gluu-scan-api";
        Integer lifetime = 86400;
        List<String> softwareRoles = Collections.singletonList("password");
        List<String> ssaGrantTypes = Collections.singletonList("client_credentials");
        SsaCreateResponse ssaCreateResponse = ssaCreateClient.execSsaCreate(accessToken, orgId, expirationDate, description,
                softwareId, softwareRoles, ssaGrantTypes, Boolean.TRUE, Boolean.TRUE, lifetime);

        showClient(ssaCreateClient);
        AssertBuilder.ssaCreate(ssaCreateClient.getRequest(), ssaCreateResponse)
                .status(HttpStatus.SC_UNAUTHORIZED)
                .check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void createSsaValidWithoutExpiration(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("createSsaValidWithoutExpiration");
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // Register client
        RegisterResponse registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // Access token
        TokenResponse tokenResponse = tokenClientCredentialsGrant(SsaScopeType.SSA_ADMIN.getValue(), clientId, clientSecret);
        String accessToken = tokenResponse.getAccessToken();

        // Ssa create
        SsaCreateClient ssaCreateClient = new SsaCreateClient(ssaEndpoint);
        String orgId = "org-id-test";
        String description = "test description";
        String softwareId = "gluu-scan-api";
        Integer lifetime = 86400;
        List<String> softwareRoles = Collections.singletonList("password");
        List<String> ssaGrantTypes = Collections.singletonList("client_credentials");
        SsaCreateResponse ssaCreateResponse = ssaCreateClient.execSsaCreate(accessToken, orgId, null, description,
                softwareId, softwareRoles, ssaGrantTypes, Boolean.FALSE, Boolean.FALSE, lifetime);

        showClient(ssaCreateClient);
        AssertBuilder.ssaCreate(ssaCreateClient.getRequest(), ssaCreateResponse)
                .check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void create_ifLifetimeIsZero_badRequest(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("create_ifLifetimeIsZero_badRequest");
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // Register client
        RegisterResponse registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // Access token
        TokenResponse tokenResponse = tokenClientCredentialsGrant(SsaScopeType.SSA_ADMIN.getValue(), clientId, clientSecret);
        String accessToken = tokenResponse.getAccessToken();

        // Ssa create
        SsaCreateClient ssaCreateClient = new SsaCreateClient(ssaEndpoint);
        String orgId = "org-id-test";
        String description = "test description";
        String softwareId = "gluu-scan-api";
        Integer lifetime = 0; // WRONG lifetime 0
        List<String> softwareRoles = Collections.singletonList("password");
        List<String> ssaGrantTypes = Collections.singletonList("client_credentials");
        SsaCreateResponse ssaCreateResponse = ssaCreateClient.execSsaCreate(accessToken, orgId, null, description,
                softwareId, softwareRoles, ssaGrantTypes, Boolean.FALSE, Boolean.FALSE, lifetime);

        showClient(ssaCreateClient);
        System.out.println(ssaCreateClient);
        AssertBuilder.ssaCreate(ssaCreateClient.getRequest(), ssaCreateResponse)
                .status(400)
                .errorType(SsaErrorResponseType.INVALID_SSA_METADATA)
                .check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void create_ifLifetimeIsNull_success(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("create_ifLifetimeIsNull_success");
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // Register client
        RegisterResponse registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // Access token
        TokenResponse tokenResponse = tokenClientCredentialsGrant(SsaScopeType.SSA_ADMIN.getValue(), clientId, clientSecret);
        String accessToken = tokenResponse.getAccessToken();

        // Ssa create
        SsaCreateClient ssaCreateClient = new SsaCreateClient(ssaEndpoint);
        String orgId = "org-id-test";
        String description = "test description";
        String softwareId = "gluu-scan-api";
        Integer lifetime = null;
        List<String> softwareRoles = Collections.singletonList("password");
        List<String> ssaGrantTypes = Collections.singletonList("client_credentials");
        SsaCreateResponse ssaCreateResponse = ssaCreateClient.execSsaCreate(accessToken, orgId, null, description,
                softwareId, softwareRoles, ssaGrantTypes, Boolean.FALSE, Boolean.FALSE, lifetime);

        showClient(ssaCreateClient);
        System.out.println(ssaCreateClient);
        AssertBuilder.ssaCreate(ssaCreateClient.getRequest(), ssaCreateResponse)
                .check();
    }
}