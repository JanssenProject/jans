package io.jans.ca.server.rest;

import io.jans.ca.common.CommandType;
import io.jans.ca.common.params.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class OpenIdConnectResource extends BaseResource {

    @POST
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getAuthorizationUrl(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /get-authorization-url  Params: {}", params);
        String result = process(CommandType.GET_AUTHORIZATION_URL, params, GetAuthorizationUrlParams.class, authorization, AuthorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/get-authorization-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getAuthorizationCode(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /get-authorization-code  Params: {}", params);
        String result = process(CommandType.GET_AUTHORIZATION_CODE, params, GetAuthorizationCodeParams.class, authorization, AuthorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getTokenByCode(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /get-tokens-by-code  Params: {}", params);
        String result = process(CommandType.GET_TOKENS_BY_CODE, params, GetTokensByCodeParams.class, authorization, AuthorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/get-logout-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getLogoutUri(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /get-logout-uri  Params: {}", params);
        String result = process(CommandType.GET_LOGOUT_URI, params, GetLogoutUrlParams.class, authorization, AuthorizationRpId);
        return Response.ok(result).build();
    }
}
