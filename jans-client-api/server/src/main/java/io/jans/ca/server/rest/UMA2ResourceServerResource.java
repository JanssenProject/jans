package io.jans.ca.server.rest;

import io.jans.ca.server.op.IntrospectRptOperation;
import io.jans.ca.server.op.RsCheckAccessOperation;
import io.jans.ca.server.op.RsModifyOperation;
import io.jans.ca.server.op.RsProtectOperation;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class UMA2ResourceServerResource extends BaseResource {

    @Inject
    RsProtectOperation rsProtectOp;
    @Inject
    RsModifyOperation rsModifyOp;
    @Inject
    IntrospectRptOperation introspectRptOp;
    @Inject
    RsCheckAccessOperation rsCheckAccessOp;

    @POST
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response umaRsProtect(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /uma-rs-protect  Params: {}", params);
        return rsProtectOp.process(params, authorization, authorizationRpId, getHttpRequest());
    }

    @POST
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response umaRsCheckAccess(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /uma-rs-check-access  Params: {}", params);
        return rsCheckAccessOp.process(params, authorization, authorizationRpId, getHttpRequest());
    }

    @POST
    @Path("/introspect-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response introspectRpt(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /introspect-rpt  Params: {}", params);
        return introspectRptOp.process(params, authorization, authorizationRpId, getHttpRequest());
    }

    @POST
    @Path("/uma-rs-modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response umaRsModify(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /uma-rs-modify  Params: {}", params);
        return rsModifyOp.process(params, authorization, authorizationRpId, getHttpRequest());
    }
}
