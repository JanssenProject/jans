package io.jans.ca.server.rest;

import io.jans.ca.common.CommandType;
import io.jans.ca.common.params.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class OpenIdConnectResource extends BaseResource {

    @POST
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAuthorizationUrl(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /get-authorization-url  Params: {}", params);
        return process(CommandType.GET_AUTHORIZATION_URL, params, GetAuthorizationUrlParams.class, authorization, AuthorizationRpId);
    }

    @POST
    @Path("/get-authorization-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAuthorizationCode(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /get-authorization-code  Params: {}", params);
        return process(CommandType.GET_AUTHORIZATION_CODE, params, GetAuthorizationCodeParams.class, authorization, AuthorizationRpId);
    }

    @POST
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getTokenByCode(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /get-tokens-by-code  Params: {}", params);
        return process(CommandType.GET_TOKENS_BY_CODE, params, GetTokensByCodeParams.class, authorization, AuthorizationRpId);
    }

    @POST
    @Path("/get-logout-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getLogoutUri(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /get-logout-uri  Params: {}", params);
        return process(CommandType.GET_LOGOUT_URI, params, GetLogoutUrlParams.class, authorization, AuthorizationRpId);
    }
}
