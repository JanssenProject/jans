package io.swagger.client.api;

import io.swagger.client.ApiException;
import io.swagger.client.model.GetAccessTokenByRefreshTokenParams;
import io.swagger.client.model.GetAccessTokenByRefreshTokenResponse;
import io.swagger.client.model.GetAuthorizationUrlParams;
import io.swagger.client.model.GetAuthorizationUrlResponse;
import io.swagger.client.model.GetClientTokenParams;
import io.swagger.client.model.GetClientTokenResponse;
import io.swagger.client.model.GetLogoutUriParams;
import io.swagger.client.model.GetLogoutUriResponse;
import io.swagger.client.model.GetTokensByCodeParams;
import io.swagger.client.model.GetTokensByCodeResponse;
import io.swagger.client.model.GetUserInfoParams;
import io.swagger.client.model.GetUserInfoResponse;
import io.swagger.client.model.IntrospectAccessTokenParams;
import io.swagger.client.model.IntrospectAccessTokenResponse;
import io.swagger.client.model.IntrospectRptParams;
import io.swagger.client.model.IntrospectRptResponse;
import io.swagger.client.model.RegisterSiteParams;
import io.swagger.client.model.RegisterSiteResponse;
import io.swagger.client.model.RemoveSiteParams;
import io.swagger.client.model.SetupClientParams;
import io.swagger.client.model.SetupClientResponse;
import io.swagger.client.model.UmaRpGetClaimsGatheringUrlParams;
import io.swagger.client.model.UmaRpGetClaimsGatheringUrlResponse;
import io.swagger.client.model.UmaRpGetRptParams;
import io.swagger.client.model.UmaRpGetRptResponse;
import io.swagger.client.model.UmaRsCheckAccessParams;
import io.swagger.client.model.UmaRsCheckAccessResponse;
import io.swagger.client.model.UmaRsProtectParams;
import io.swagger.client.model.UpdateSiteParams;
import io.swagger.client.model.UpdateSiteResponse;
import org.junit.Test;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API tests for DevelopersApi
 */
@Ignore
public class DevelopersApiTest {

    private final DevelopersApi api = new DevelopersApi();

    
    /**
     * Get Access Token By Refresh Token
     *
     * Get Access Token By Refresh Token
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getAccessTokenByRefreshTokenTest() throws ApiException {
        String authorization = null;
        GetAccessTokenByRefreshTokenParams getAccessTokenByRefreshTokenParams = null;
        GetAccessTokenByRefreshTokenResponse response = api.getAccessTokenByRefreshToken(authorization, getAccessTokenByRefreshTokenParams);

        // TODO: test validations
    }
    
    /**
     * Get Authorization Url
     *
     * Gets authorization url
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getAuthorizationUrlTest() throws ApiException {
        String authorization = null;
        GetAuthorizationUrlParams getAuthorizationUrlParams = null;
        GetAuthorizationUrlResponse response = api.getAuthorizationUrl(authorization, getAuthorizationUrlParams);

        // TODO: test validations
    }
    
    /**
     * Get Client Token
     *
     * Gets Client Token
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getClientTokenTest() throws ApiException {
        GetClientTokenParams getClientTokenParams = null;
        GetClientTokenResponse response = api.getClientToken(getClientTokenParams);

        // TODO: test validations
    }
    
    /**
     * Get Logout URL
     *
     * Get Logout URL
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getLogoutUriTest() throws ApiException {
        String authorization = null;
        GetLogoutUriParams getLogoutUriParams = null;
        GetLogoutUriResponse response = api.getLogoutUri(authorization, getLogoutUriParams);

        // TODO: test validations
    }
    
    /**
     * Get Tokens By Code
     *
     * Get tokens by code
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getTokensByCodeTest() throws ApiException {
        String authorization = null;
        GetTokensByCodeParams getTokensByCodeParams = null;
        GetTokensByCodeResponse response = api.getTokensByCode(authorization, getTokensByCodeParams);

        // TODO: test validations
    }
    
    /**
     * Get User Info
     *
     * Get User Info
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getUserInfoTest() throws ApiException {
        String authorization = null;
        GetUserInfoParams getUserInfoParams = null;
        GetUserInfoResponse response = api.getUserInfo(authorization, getUserInfoParams);

        // TODO: test validations
    }
    
    /**
     * Health Check
     *
     * Health Check endpoint is for quick check whether oxd-server is alive.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void healthCheckTest() throws ApiException {
        api.healthCheck();

        // TODO: test validations
    }
    
    /**
     * Introspect Access Token
     *
     * Introspect Access Token
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void introspectAccessTokenTest() throws ApiException {
        String authorization = null;
        IntrospectAccessTokenParams introspectAccessTokenParams = null;
        IntrospectAccessTokenResponse response = api.introspectAccessToken(authorization, introspectAccessTokenParams);

        // TODO: test validations
    }
    
    /**
     * Introspect RPT
     *
     * Introspect RPT
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void introspectRptTest() throws ApiException {
        String authorization = null;
        IntrospectRptParams introspectRptParams = null;
        IntrospectRptResponse response = api.introspectRpt(authorization, introspectRptParams);

        // TODO: test validations
    }
    
    /**
     * Register Site
     *
     * Registers site at oxd-server
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void registerSiteTest() throws ApiException {
        String authorization = null;
        RegisterSiteParams registerSiteParams = null;
        RegisterSiteResponse response = api.registerSite(authorization, registerSiteParams);

        // TODO: test validations
    }
    
    /**
     * Remove Site
     *
     * Removes site from oxd-server
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void removeSiteTest() throws ApiException {
        String authorization = null;
        RemoveSiteParams removeSiteParams = null;
        UpdateSiteResponse response = api.removeSite(authorization, removeSiteParams);

        // TODO: test validations
    }
    
    /**
     * Setup Client
     *
     * Setups client is for securing communication to oxd-server
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void setupClientTest() throws ApiException {
        SetupClientParams setupClientParams = null;
        SetupClientResponse response = api.setupClient(setupClientParams);

        // TODO: test validations
    }
    
    /**
     * UMA RP Get Claims Gathering URL
     *
     * UMA RP Get Claims Gathering URL
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void umaRpGetClaimsGatheringUrlTest() throws ApiException {
        String authorization = null;
        UmaRpGetClaimsGatheringUrlParams umaRpGetClaimsGatheringUrlParams = null;
        UmaRpGetClaimsGatheringUrlResponse response = api.umaRpGetClaimsGatheringUrl(authorization, umaRpGetClaimsGatheringUrlParams);

        // TODO: test validations
    }
    
    /**
     * UMA RP Get RPT
     *
     * UMA RP Get RPT
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void umaRpGetRptTest() throws ApiException {
        String authorization = null;
        UmaRpGetRptParams umaRpGetRptParams = null;
        UmaRpGetRptResponse response = api.umaRpGetRpt(authorization, umaRpGetRptParams);

        // TODO: test validations
    }
    
    /**
     * UMA RS Check Access
     *
     * UMA RS Check Access
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void umaRsCheckAccessTest() throws ApiException {
        String authorization = null;
        UmaRsCheckAccessParams umaRsCheckAccessParams = null;
        UmaRsCheckAccessResponse response = api.umaRsCheckAccess(authorization, umaRsCheckAccessParams);

        // TODO: test validations
    }
    
    /**
     * UMA RS Protect Resources
     *
     * UMA RS Protect Resources. It&#39;s important to have a single HTTP method, mentioned only once within a given path in JSON, otherwise, the operation will fail.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void umaRsProtectTest() throws ApiException {
        String authorization = null;
        UmaRsProtectParams umaRsProtectParams = null;
        UpdateSiteResponse response = api.umaRsProtect(authorization, umaRsProtectParams);

        // TODO: test validations
    }
    
    /**
     * Update Site
     *
     * Updates site at oxd-server
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateSiteTest() throws ApiException {
        String authorization = null;
        UpdateSiteParams updateSiteParams = null;
        UpdateSiteResponse response = api.updateSite(authorization, updateSiteParams);

        // TODO: test validations
    }
    
}
