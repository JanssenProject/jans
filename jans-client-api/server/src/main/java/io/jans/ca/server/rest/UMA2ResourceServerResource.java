package io.jans.ca.server.rest;

import io.jans.ca.server.op.IntrospectRptOperation;
import io.jans.ca.server.op.RsCheckAccessOperation;
import io.jans.ca.server.op.RsModifyOperation;
import io.jans.ca.server.op.RsProtectOperation;
import io.jans.configapi.core.rest.ProtectedApi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
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
    @ProtectedApi
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response umaRsProtect(String params) {
        logger.info("Api Resource: /uma-rs-protect  Params: {}", params);
        return rsProtectOp.process(params, getHttpRequest());
    }

    @POST
    @ProtectedApi
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response umaRsCheckAccess(String params) {
        logger.info("Api Resource: /uma-rs-check-access  Params: {}", params);
        return rsCheckAccessOp.process(params, getHttpRequest());
    }

    @POST
    @ProtectedApi
    @Path("/introspect-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response introspectRpt(String params) {
        logger.info("Api Resource: /introspect-rpt  Params: {}", params);
        return introspectRptOp.process(params, getHttpRequest());
    }

    @POST
    @ProtectedApi
    @Path("/uma-rs-modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response umaRsModify(String params) {
        logger.info("Api Resource: /uma-rs-modify  Params: {}", params);
        return rsModifyOp.process(params, getHttpRequest());
    }
}
