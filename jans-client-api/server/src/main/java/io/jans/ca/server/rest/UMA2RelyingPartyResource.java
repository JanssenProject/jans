package io.jans.ca.server.rest;

import io.jans.ca.server.op.RpGetGetClaimsGatheringUrlOperation;
import io.jans.ca.server.op.RpGetRptOperation;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class UMA2RelyingPartyResource extends BaseResource {

    @Inject
    RpGetRptOperation rpGetRptOp;
    @Inject
    RpGetGetClaimsGatheringUrlOperation rpGetGetClaimsGatheringUrlOp;

    @POST
    @Path("/uma-rp-get-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response umaRpGetRpt(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /uma-rp-get-rpt  Params: {}", params);
        return rpGetRptOp.process(params, authorization, authorizationRpId, getHttpRequest());
    }

    @POST
    @Path("/uma-rp-get-claims-gathering-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response umaRpGetClaimsGatheringUrl(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String authorizationRpId, String params) {
        logger.info("Api Resource: /uma-rp-get-claims-gathering-url  Params: {}", params);
        return rpGetGetClaimsGatheringUrlOp.process(params, authorization, authorizationRpId, getHttpRequest());
    }

}
