package io.jans.ca.server.rest;

import io.jans.ca.common.params.StringParam;
import io.jans.ca.server.op.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class RpResource extends BaseResource {

    @Inject
    GetRpJwksOperation getRpJwksOp;
    @Inject
    GetRpOperation getRpOp;
    @Inject
    AuthorizationCodeFlowOperation authorizationCodeFlowOp;
    @Inject
    GetRequestObjectOperation getRequestObjectOp;
    @Inject
    GetRequestObjectUriOperation getRequestObjectUriOp;

    @GET
    @Path("/get-rp-jwks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRpJwks() {
        logger.info("Api Resource: get-rp-jwks");
        return getRpJwksOp.process(null, getHttpRequest());
    }

    @POST
    @Path("/get-rp")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRp(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: get-rp");
        return getRpOp.process(params, authorization, authorizationRpId, getHttpRequest());
    }

    @POST
    @Path("/authorization-code-flow")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response authorizationCodeFlow(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: authorization-code-flow");
        return authorizationCodeFlowOp.process(params, authorization, authorizationRpId, getHttpRequest());
    }

    @GET
    @Path("/get-request-object/{request_object_id}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getRequestObject(@PathParam("request_object_id") String value) {
        logger.info("Api Resource: get-request-object/{}", value);
        return getRequestObjectOp.process((new StringParam(value)).toJsonString(), getHttpRequest());
    }

    @POST
    @Path("/get-request-object-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getRequestObjectUri(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: get-request-object-uri");
        return getRequestObjectUriOp.process(params, authorization, authorizationRpId, getHttpRequest());
    }
}
