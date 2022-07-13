package io.jans.ca.server.rest;

import io.jans.ca.server.op.*;
import io.jans.ca.common.rest.ProtectedApi;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class OAuth20Resource extends BaseResource {

    @Inject
    GetDiscoveryOperation getDiscoveryOp;
    @Inject
    RegisterSiteOperation registerSiteOp;
    @Inject
    UpdateSiteOperation updateSiteOp;
    @Inject
    RemoveSiteOperation removeSiteOp;
    @Inject
    GetClientTokenOperation getClientTokenOp;
    @Inject
    GetAccessTokenByRefreshTokenOperation getAccessTokenByRefreshTokenOp;
    @Inject
    IntrospectAccessTokenOperation introspectAccessTokenOp;
    @Inject
    GetUserInfoOperation getUserInfoOp;
    @Inject
    GetJwksOperation getJwksOp;
    @Inject
    GetIssuerOperation getIssuerOp;
    @Inject
    CheckIdTokenOperation getCheckIdTokenOp;
    @Inject
    CheckAccessTokenOperation getCheckAccessTokenOp;

    @POST
    @Path("/register-site")
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerSite(String params) {
        logger.info("Api Resource: /register-site  Params: {}", params);
        return registerSiteOp.process(params, getHttpRequest());
    }

    @POST
    @Path("/update-site")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSite(String params) {
        logger.info("Api Resource: /update-site  Params: {}", params);
        return updateSiteOp.process(params, getHttpRequest());
    }

    @POST
    @Path("/remove-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeSite(String params) {
        logger.info("Api Resource: /remove-site  Params: {}", params);
        return removeSiteOp.process(params, getHttpRequest());
    }

    @POST
    @Path("/get-client-token")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClientToken(String params) {
        logger.info("Api Resource: /get-client-token  Params: {}", params);
        return getClientTokenOp.process(params, getHttpRequest());
    }

    @POST
    @ProtectedApi
    @Path("/get-access-token-by-refresh-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getAccessTokenByRefreshToken(String params) {
        logger.info("Api Resource: /get-access-token-by-refresh-token  Params: {}", params);
        return getAccessTokenByRefreshTokenOp.process(params, getHttpRequest());
    }

    @POST
    @ProtectedApi
    @Path("/introspect-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response introspectAccessToken(String params) {
        logger.info("Api Resource: /introspect-access-token  Params: {}", params);
        return introspectAccessTokenOp.process(params, getHttpRequest());
    }

    @POST
    @ProtectedApi
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUserInfo(String params) {
        logger.info("Api Resource: /get-user-info  Params: {}", params);
        return getUserInfoOp.process(params, getHttpRequest());
    }

    @POST
    @Path("/get-jwks")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getJwks(String params) {
        logger.info("Api Resource: /get-jwks  Params: {}", params);
        return getJwksOp.process(params, getHttpRequest());
    }

    @POST
    @Path("/get-discovery")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDiscovery(String params) {
        logger.info("Api Resource: /get-discovery  Params: {}", params);
        return getDiscoveryOp.process(params, getHttpRequest());
    }

    @POST
    @ProtectedApi
    @Path("/check-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkAccessToken(String params) {
        logger.info("Api Resource: /check-access-token  Params: {}", params);
        return getCheckAccessTokenOp.process(params, getHttpRequest());
    }

    @POST
    @ProtectedApi
    @Path("/check-id-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkIdToken(String params) {
        logger.info("Api Resource: /check-id-token  Params: {}", params);
        return getCheckIdTokenOp.process(params, getHttpRequest());
    }

    @POST
    @Path("/get-issuer")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getIssuer(String params) {
        logger.info("Api Resource: /get-issuer  Params: {}", params);
        return getIssuerOp.process(params, getHttpRequest());
    }
}
