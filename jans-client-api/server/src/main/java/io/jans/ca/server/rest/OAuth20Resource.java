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
    public Response updateSite(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /update-site  Params: {}", params);
        String result = process(CommandType.UPDATE_SITE, params, UpdateSiteParams.class, authorization, AuthorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/remove-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeSite(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /remove-site  Params: {}", params);
        String result = process(CommandType.REMOVE_SITE, params, RemoveSiteParams.class, authorization, AuthorizationRpId);
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
    public String getAccessTokenByRefreshToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /get-access-token-by-refresh-token  Params: {}", params);
        return process(CommandType.GET_ACCESS_TOKEN_BY_REFRESH_TOKEN, params, GetAccessTokenByRefreshTokenParams.class, authorization, AuthorizationRpId);
    }

    @POST
    @Path("/introspect-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectAccessToken(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /introspect-access-token  Params: {}", params);
        return process(CommandType.INTROSPECT_ACCESS_TOKEN, params, IntrospectAccessTokenParams.class, authorization, AuthorizationRpId);
    }

    @POST
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getUserInfo(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /get-user-info  Params: {}", params);
        return process(CommandType.GET_USER_INFO, params, GetUserInfoParams.class, authorization, AuthorizationRpId);
    }

    @POST
    @Path("/get-jwks")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getJwks(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /get-jwks  Params: {}", params);
        return process(CommandType.GET_JWKS, params, GetJwksParams.class, authorization, AuthorizationRpId);
    }

    @POST
    @Path("/get-discovery")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDiscovery(String params) {
        logger.info("Api Resource: /get-discovery  Params: {}", params);
        String result = process(CommandType.GET_DISCOVERY, params, GetDiscoveryParams.class, null, null);
        return Response.ok(result).build();
    }
}
