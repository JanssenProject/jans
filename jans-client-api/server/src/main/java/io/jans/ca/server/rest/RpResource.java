package io.jans.ca.server.rest;

import io.jans.ca.common.CommandType;
import io.jans.ca.common.params.AuthorizationCodeFlowParams;
import io.jans.ca.common.params.GetJwksParams;
import io.jans.ca.common.params.GetRpParams;
import io.jans.ca.common.params.StringParam;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class RpResource extends BaseResource {

    @GET
    @Path("/get-rp-jwks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRpJwks() {
        logger.info("Api Resource: get-rp-jwks");
        String result = process(CommandType.GET_RP_JWKS, null, GetJwksParams.class, null, null);
        logger.info("Api Resource: get-rp-jwks - result:{}", result);

        return Response.ok(result).build();
    }

    @POST
    @Path("/get-rp")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRp(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: get-rp");
        String result = process(CommandType.GET_RP, params, GetRpParams.class, authorization, authorizationRpId);
        logger.info("Api Resource: get-rp - result:{}", result);

        return Response.ok(result).build();
    }

    @POST
    @Path("/authorization-code-flow")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String authorizationCodeFlow(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: authorization-code-flow");
        return process(CommandType.AUTHORIZATION_CODE_FLOW, params, AuthorizationCodeFlowParams.class, authorization, AuthorizationRpId);
    }

    @GET
    @Path("/get-request-object/{request_object_id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getRequestObject(@PathParam("request_object_id") String value) {
        logger.info("Api Resource: get-request-object/" + value);
        return process(CommandType.GET_REQUEST_OBJECT_JWT, (new StringParam(value)).toJsonString(), StringParam.class, null, null);
    }
}
