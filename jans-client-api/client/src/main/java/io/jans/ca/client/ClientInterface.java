package io.jans.ca.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.ca.common.introspection.CorrectRptIntrospectionResponse;
import io.jans.ca.common.params.*;
import io.jans.ca.common.response.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * @author yuriyz
 */
public interface ClientInterface {

    String healthCheck();

    JsonNode getRpJwks();

    String getRequestObject(String value);

    GetClientTokenResponse getClientToken(GetClientTokenParams params);

    IntrospectAccessTokenResponse introspectAccessToken(String authorization, String authorizationRpId, IntrospectAccessTokenParams params);

    CorrectRptIntrospectionResponse introspectRpt(String authorization, String authorizationRpId, IntrospectRptParams params);

    RegisterSiteResponse registerSite(RegisterSiteParams params);

    UpdateSiteResponse updateSite(String authorization, String authorizationRpId, UpdateSiteParams params);

    RemoveSiteResponse removeSite(String authorization, String authorizationRpId, RemoveSiteParams params);

    GetAuthorizationUrlResponse getAuthorizationUrl(String authorization, String authorizationRpId, GetAuthorizationUrlParams params);

    GetAuthorizationCodeResponse getAuthorizationCode(String authorization, String authorizationRpId, GetAuthorizationCodeParams params);

    GetTokensByCodeResponse2 getTokenByCode(String authorization, String authorizationRpId, GetTokensByCodeParams params);

    JsonNode getUserInfo(String authorization, String authorizationRpId, GetUserInfoParams params);

    GetLogoutUriResponse getLogoutUri(String authorization, String authorizationRpId, GetLogoutUrlParams params);

    GetClientTokenResponse getAccessTokenByRefreshToken(String authorization, String authorizationRpId, GetAccessTokenByRefreshTokenParams params);

    RsProtectResponse umaRsProtect(String authorization, String authorizationRpId, RsProtectParams2 params);

    RsModifyResponse umaRsModify(String authorization, String authorizationRpId, RsModifyParams params);

    RsCheckAccessResponse umaRsCheckAccess(String authorization, String authorizationRpId, RsCheckAccessParams params);

    RpGetRptResponse umaRpGetRpt(String authorization, String authorizationRpId, RpGetRptParams params);

    RpGetClaimsGatheringUrlResponse umaRpGetClaimsGatheringUrl(String authorization, String authorizationRpId, RpGetClaimsGatheringUrlParams params);

    AuthorizationCodeFlowResponse authorizationCodeFlow(String authorization, String authorizationRpId, AuthorizationCodeFlowParams params);

    CheckAccessTokenResponse checkAccessToken(String authorization, String authorizationRpId, CheckAccessTokenParams params);

    CheckIdTokenResponse checkIdToken(String authorization, String authorizationRpId, CheckIdTokenParams params);

    String getRp(String authorization, String authorizationRpId, GetRpParams params);

    GetJwksResponse getJwks(String authorization, String authorizationRpId, GetJwksParams params);

    GetDiscoveryResponse getDiscovery(GetDiscoveryParams params);

    GetIssuerResponse getIssuer(GetIssuerParams params);

    GetRequestObjectUriResponse getRequestObjectUri(String authorization, String authorizationRpId, GetRequestObjectUriParams params);

    String getApitargetURL();
}
