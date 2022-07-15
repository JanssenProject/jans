package io.jans.ca.server.rest;

import io.jans.ca.server.op.GetAuthorizationCodeOperation;
import io.jans.ca.server.op.GetAuthorizationUrlOperation;
import io.jans.ca.server.op.GetLogoutUrlOperation;
import io.jans.ca.server.op.GetTokensByCodeOperation;
import io.jans.ca.common.rest.ProtectedApi;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class OpenIdConnectResource extends BaseResource {

    @Inject
    GetAuthorizationCodeOperation getAuthorizationCodeOp;
    @Inject
    GetAuthorizationUrlOperation getAuthorizationUrlOp;
    @Inject
    GetTokensByCodeOperation getTokensByCodeOp;
    @Inject
    GetLogoutUrlOperation getLogoutUrlOp;

    @POST
    @ProtectedApi
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getAuthorizationUrl(String params) {
        logger.info("Api Resource: /get-authorization-url  Params: {}", params);
        return getAuthorizationUrlOp.process(params, getHttpRequest());
    }

    @POST
    @ProtectedApi
    @Path("/get-authorization-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getAuthorizationCode(String params) {
        logger.info("Api Resource: /get-authorization-code  Params: {}", params);
        return getAuthorizationCodeOp.process(params, getHttpRequest());
    }

    @POST
    @ProtectedApi
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getTokenByCode(String params) {
        logger.info("Api Resource: /get-tokens-by-code  Params: {}", params);
        return getTokensByCodeOp.process(params, getHttpRequest());
    }

    @POST
    @ProtectedApi
    @Path("/get-logout-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getLogoutUri(String params) {
        logger.info("Api Resource: /get-logout-uri  Params: {}", params);
        return getLogoutUrlOp.process(params, getHttpRequest());
    }
}
