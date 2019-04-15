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

    @POST
    @Path("/get-client-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetClientTokenResponse getClientToken(GetClientTokenParams params);

    @POST
    @Path("/introspect-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    IntrospectAccessTokenResponse introspectAccessToken(@HeaderParam("Authorization") String authorization, IntrospectAccessTokenParams params);

    @POST
    @Path("/introspect-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CorrectRptIntrospectionResponse introspectRpt(@HeaderParam("Authorization") String authorization, IntrospectRptParams params);

    @POST
    @Path("/register-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RegisterSiteResponse registerSite(RegisterSiteParams params);

    @POST
    @Path("/update-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    UpdateSiteResponse updateSite(@HeaderParam("Authorization") String authorization, UpdateSiteParams params);

    @POST
    @Path("/remove-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RemoveSiteResponse removeSite(@HeaderParam("Authorization") String authorization, RemoveSiteParams params);

    @POST
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetAuthorizationUrlResponse getAuthorizationUrl(@HeaderParam("Authorization") String authorization, GetAuthorizationUrlParams params);

    @POST
    @Path("/get-authorization-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetAuthorizationCodeResponse getAuthorizationCode(@HeaderParam("Authorization") String authorization, GetAuthorizationCodeParams params);

    @POST
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetTokensByCodeResponse2 getTokenByCode(@HeaderParam("Authorization") String authorization, GetTokensByCodeParams params);

    @POST
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    JsonNode getUserInfo(@HeaderParam("Authorization") String authorization, GetUserInfoParams params);

    @POST
    @Path("/get-logout-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetLogoutUriResponse getLogoutUri(@HeaderParam("Authorization") String authorization, GetLogoutUrlParams params);

    @POST
    @Path("/get-access-token-by-refresh-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetClientTokenResponse getAccessTokenByRefreshToken(@HeaderParam("Authorization") String authorization, GetAccessTokenByRefreshTokenParams params);

    @POST
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RsProtectResponse umaRsProtect(@HeaderParam("Authorization") String authorization, RsProtectParams2 params);

    @POST
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RsCheckAccessResponse umaRsCheckAccess(@HeaderParam("Authorization") String authorization, RsCheckAccessParams params);

    @POST
    @Path("/uma-rp-get-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RpGetRptResponse umaRpGetRpt(@HeaderParam("Authorization") String authorization, RpGetRptParams params);

    @POST
    @Path("/uma-rp-get-claims-gathering-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RpGetClaimsGatheringUrlResponse umaRpGetClaimsGatheringUrl(@HeaderParam("Authorization") String authorization, RpGetClaimsGatheringUrlParams params);

    @POST
    @Path("/authorization-code-flow")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    AuthorizationCodeFlowResponse authorizationCodeFlow(@HeaderParam("Authorization") String authorization, AuthorizationCodeFlowParams params);

    @POST
    @Path("/check-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CheckAccessTokenResponse checkAccessToken(@HeaderParam("Authorization") String authorization, CheckAccessTokenParams params);

    @POST
    @Path("/check-id-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CheckIdTokenResponse checkIdToken(@HeaderParam("Authorization") String authorization, CheckIdTokenParams params);

    @POST
    @Path("/get-rp")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    String getRp(@HeaderParam("Authorization") String authorization, GetRpParams params);

    @POST
    @Path("/get-jwks")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GetJwksResponse getJwks(@HeaderParam("Authorization") String authorization, GetJwksParams params);

}
