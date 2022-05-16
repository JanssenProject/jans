package io.jans.ca.server.rest;

import io.jans.ca.common.CommandType;
import io.jans.ca.common.params.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class UMA2ResourceServerResource extends BaseResource {

    @POST
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response umaRsProtect(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /uma-rs-protect  Params: {}", params);
        String result = process(CommandType.RS_PROTECT, params, RsProtectParams.class, authorization, AuthorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response umaRsCheckAccess(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /uma-rs-check-access  Params: {}", params);
        String result = process(CommandType.RS_CHECK_ACCESS, params, RsCheckAccessParams.class, authorization, AuthorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/introspect-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response introspectRpt(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /introspect-rpt  Params: {}", params);
        String result = process(CommandType.INTROSPECT_RPT, params, IntrospectRptParams.class, authorization, AuthorizationRpId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/uma-rs-modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response umaRsModify(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /uma-rs-modify  Params: {}", params);
        String result = process(CommandType.RS_MODIFY, params, RsModifyParams.class, authorization, AuthorizationRpId);
        return Response.ok(result).build();
    }
}
