package io.jans.ca.server.rest;

import io.jans.ca.common.CommandType;
import io.jans.ca.common.params.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class UMA2ReourceServerResource extends BaseResource {

    @POST
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsProtect(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /uma-rs-protect  Params: {}", params);
        return process(CommandType.RS_PROTECT, params, RsProtectParams.class, authorization, AuthorizationRpId);
    }

    @POST
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsCheckAccess(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /uma-rs-check-access  Params: {}", params);
        return process(CommandType.RS_CHECK_ACCESS, params, RsCheckAccessParams.class, authorization, AuthorizationRpId);
    }

    @POST
    @Path("/introspect-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectRpt(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /introspect-rpt  Params: {}", params);
        return process(CommandType.INTROSPECT_RPT, params, IntrospectRptParams.class, authorization, AuthorizationRpId);
    }

    @POST
    @Path("/uma-rs-modify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsModify(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /uma-rs-modify  Params: {}", params);
        return process(CommandType.RS_MODIFY, params, RsModifyParams.class, authorization, AuthorizationRpId);
    }
}
