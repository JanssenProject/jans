package org.gluu.oxd.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.gluu.oxd.common.introspection.CorrectRptIntrospectionResponse;
import org.gluu.oxd.common.params.*;
import org.gluu.oxd.common.response.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author yuriyz
 */
@Path("/")
public interface ClientInterface {

    @GET
    @Path("/health-check")
    @Produces(MediaType.APPLICATION_JSON)
    String healthCheck();

    @GET
    @Path("/get-rp-jwks")
    @Produces(MediaType.APPLICATION_JSON)
    JsonNode getRpJwks();

    @GET
    @Path("/get-request-object/{request_object_id}")
    @Produces(MediaType.TEXT_PLAIN)
    String getRequestObject(@PathParam("request_object_id") String value);

    @POST
    @Path("/get-client-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetClientTokenResponse getClientToken(GetClientTokenParams params);

    @POST
    @Path("/introspect-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    IntrospectAccessTokenResponse introspectAccessToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, IntrospectAccessTokenParams params);

    @POST
    @Path("/introspect-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CorrectRptIntrospectionResponse introspectRpt(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, IntrospectRptParams params);

    @POST
    @Path("/register-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RegisterSiteResponse registerSite(RegisterSiteParams params);

    @POST
    @Path("/update-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    UpdateSiteResponse updateSite(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, UpdateSiteParams params);

    @POST
    @Path("/remove-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RemoveSiteResponse removeSite(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, RemoveSiteParams params);

    @POST
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetAuthorizationUrlResponse getAuthorizationUrl(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, GetAuthorizationUrlParams params);

    @POST
    @Path("/get-authorization-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetAuthorizationCodeResponse getAuthorizationCode(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, GetAuthorizationCodeParams params);

    @POST
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetTokensByCodeResponse2 getTokenByCode(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, GetTokensByCodeParams params);

    @POST
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    JsonNode getUserInfo(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, GetUserInfoParams params);

    @POST
    @Path("/get-logout-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetLogoutUriResponse getLogoutUri(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, GetLogoutUrlParams params);

    @POST
    @Path("/get-access-token-by-refresh-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetClientTokenResponse getAccessTokenByRefreshToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, GetAccessTokenByRefreshTokenParams params);

    @POST
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RsProtectResponse umaRsProtect(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, RsProtectParams2 params);

    @POST
    @Path("/uma-rs-modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RsModifyResponse umaRsModify(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, RsModifyParams params);

    @POST
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RsCheckAccessResponse umaRsCheckAccess(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, RsCheckAccessParams params);

    @POST
    @Path("/uma-rp-get-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RpGetRptResponse umaRpGetRpt(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, RpGetRptParams params);

    @POST
    @Path("/uma-rp-get-claims-gathering-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RpGetClaimsGatheringUrlResponse umaRpGetClaimsGatheringUrl(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, RpGetClaimsGatheringUrlParams params);

    @POST
    @Path("/authorization-code-flow")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    AuthorizationCodeFlowResponse authorizationCodeFlow(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, AuthorizationCodeFlowParams params);

    @POST
    @Path("/check-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CheckAccessTokenResponse checkAccessToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, CheckAccessTokenParams params);

    @POST
    @Path("/check-id-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CheckIdTokenResponse checkIdToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, CheckIdTokenParams params);

    @POST
    @Path("/get-rp")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    String getRp(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, GetRpParams params);

    @POST
    @Path("/get-jwks")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetJwksResponse getJwks(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, GetJwksParams params);

    @POST
    @Path("/get-discovery")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetDiscoveryResponse getDiscovery(GetDiscoveryParams params);

    @POST
    @Path("/get-issuer")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetIssuerResponse getIssuer(GetIssuerParams params);

    @POST
    @Path("/get-request-object-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetRequestObjectUriResponse getRequestObjectUri(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationOxdId") String authorizationOxdId, GetRequestObjectUriParams params);
}
