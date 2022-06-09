package io.jans.ca.server.rest;

import io.jans.ca.common.CommandType;
import io.jans.ca.common.params.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class OAuth20Resource extends BaseResource {

    @POST
    @Path("/register-site")
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerSite(String params) {
        logger.info("Api Resource: /register-site  Params: {}", params);
        String result = process(CommandType.REGISTER_SITE, params, RegisterSiteParams.class, null, null);
        return Response.ok(result).build();
    }

    @POST
    @Path("/update-site")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSite(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /update-site  Params: {}", params);
        String result = process(CommandType.UPDATE_SITE, params, UpdateSiteParams.class, authorization, authorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/remove-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeSite(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /remove-site  Params: {}", params);
        String result = process(CommandType.REMOVE_SITE, params, RemoveSiteParams.class, authorization, authorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/get-client-token")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClientToken(String params) {
        logger.info("Api Resource: /get-client-token  Params: {}", params);
        String result = process(CommandType.GET_CLIENT_TOKEN, params, GetClientTokenParams.class, null, null);
        return Response.ok(result).build();
    }

    @POST
    @Path("/get-access-token-by-refresh-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getAccessTokenByRefreshToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /get-access-token-by-refresh-token  Params: {}", params);
        String result = process(CommandType.GET_ACCESS_TOKEN_BY_REFRESH_TOKEN, params, GetAccessTokenByRefreshTokenParams.class, authorization, authorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/introspect-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response introspectAccessToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /introspect-access-token  Params: {}", params);
        String result = process(CommandType.INTROSPECT_ACCESS_TOKEN, params, IntrospectAccessTokenParams.class, authorization, authorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUserInfo(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /get-user-info  Params: {}", params);
        String result = process(CommandType.GET_USER_INFO, params, GetUserInfoParams.class, authorization, authorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/get-jwks")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getJwks(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /get-jwks  Params: {}", params);
        String result = process(CommandType.GET_JWKS, params, GetJwksParams.class, authorization, authorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/get-discovery")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDiscovery(String params) {
        logger.info("Api Resource: /get-discovery  Params: {}", params);
        String result = process(CommandType.GET_DISCOVERY, params, GetDiscoveryParams.class, null, null);
        return Response.ok(result).build();
    }

    @POST
    @Path("/check-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkAccessToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /check-access-token  Params: {}", params);
        String result = process(CommandType.CHECK_ACCESS_TOKEN, params, CheckAccessTokenParams.class, authorization, authorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/check-id-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkIdToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /check-id-token  Params: {}", params);
        String result = process(CommandType.CHECK_ID_TOKEN, params, CheckIdTokenParams.class, authorization, authorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/get-issuer")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getIssuer(String params) {
        logger.info("Api Resource: /get-issuer  Params: {}", params);
        String result = process(CommandType.ISSUER_DISCOVERY, params, GetIssuerParams.class, null, null);
        return Response.ok(result).build();
    }
}
