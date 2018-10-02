package org.xdi.oxd.client;

import org.xdi.oxd.common.params.*;

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
    CommandResponse2 getClientToken(GetClientTokenParams params);

    @POST
    @Path("/introspect-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 introspectAccessToken(@HeaderParam("Authorization") String authorization, IntrospectAccessTokenParams params);

    @POST
    @Path("/introspect-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 introspectRpt(@HeaderParam("Authorization") String authorization, IntrospectRptParams params);

    @POST
    @Path("/register-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 registerSite(RegisterSiteParams params);

    @POST
    @Path("/update-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 updateSite(@HeaderParam("Authorization") String authorization, UpdateSiteParams params);

    @POST
    @Path("/remove-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 removeSite(@HeaderParam("Authorization") String authorization, RemoveSiteParams params);

    @POST
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 getAuthorizationUrl(@HeaderParam("Authorization") String authorization, GetAuthorizationUrlParams params);

    @POST
    @Path("/get-authorization-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 getAuthorizationCode(@HeaderParam("Authorization") String authorization, GetAuthorizationCodeParams params);

    @POST
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 getTokenByCode(@HeaderParam("Authorization") String authorization, GetTokensByCodeParams params);

    @POST
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 getUserInfo(@HeaderParam("Authorization") String authorization, GetUserInfoParams params);

    @POST
    @Path("/get-logout-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 getLogoutUri(@HeaderParam("Authorization") String authorization, GetLogoutUrlParams params);

    @POST
    @Path("/get-access-token-by-refresh-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 getAccessTokenByRefreshToken(@HeaderParam("Authorization") String authorization, GetAccessTokenByRefreshTokenParams params);

    @POST
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 umaRsProtect(@HeaderParam("Authorization") String authorization, RsProtectParams2 params);

    @POST
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 umaRsCheckAccess(@HeaderParam("Authorization") String authorization, RsCheckAccessParams params);

    @POST
    @Path("/uma-rp-get-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 umaRpGetRpt(@HeaderParam("Authorization") String authorization, RpGetRptParams params);

    @POST
    @Path("/uma-rp-get-claims-gathering-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 umaRpGetClaimsGatheringUrl(@HeaderParam("Authorization") String authorization, RpGetClaimsGatheringUrlParams params);

    @POST
    @Path("/authorization-code-flow")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 authorizationCodeFlow(@HeaderParam("Authorization") String authorization, AuthorizationCodeFlowParams params);

    @POST
    @Path("/check-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 checkAccessToken(@HeaderParam("Authorization") String authorization, CheckAccessTokenParams params);

    @POST
    @Path("/check-id-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 checkIdToken(@HeaderParam("Authorization") String authorization, CheckIdTokenParams params);

    @POST
    @Path("/get-rp")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CommandResponse2 getRp(@HeaderParam("Authorization") String authorization, GetRpParams params);
}
