package io.jans.ca.server.rest;

import io.jans.ca.server.op.RpGetGetClaimsGatheringUrlOperation;
import io.jans.ca.server.op.RpGetRptOperation;
import io.jans.configapi.core.rest.ProtectedApi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class UMA2RelyingPartyResource extends BaseResource {

    @Inject
    RpGetRptOperation rpGetRptOp;
    @Inject
    RpGetGetClaimsGatheringUrlOperation rpGetGetClaimsGatheringUrlOp;

    @POST
    @ProtectedApi
    @Path("/uma-rp-get-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response umaRpGetRpt(String params) {
        logger.info("Api Resource: /uma-rp-get-rpt  Params: {}", params);
        return rpGetRptOp.process(params, getHttpRequest());
    }

    @POST
    @ProtectedApi
    @Path("/uma-rp-get-claims-gathering-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response umaRpGetClaimsGatheringUrl(String params) {
        logger.info("Api Resource: /uma-rp-get-claims-gathering-url  Params: {}", params);
        return rpGetGetClaimsGatheringUrlOp.process(params, getHttpRequest());
    }

}
