package io.jans.ca.server.rest;

import io.jans.ca.common.CommandType;
import io.jans.ca.common.params.RpGetClaimsGatheringUrlParams;
import io.jans.ca.common.params.RpGetRptParams;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class UMA2RelyingPartyResource extends BaseResource {

    @POST
    @Path("/uma-rp-get-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetRpt(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /uma-rp-get-rpt  Params: {}", params);
        return process(CommandType.RP_GET_RPT, params, RpGetRptParams.class, authorization, AuthorizationRpId);
    }

    @POST
    @Path("/uma-rp-get-claims-gathering-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetClaimsGatheringUrl(@HeaderParam("Authorization") String authorization, @HeaderParam("AuthorizationRpId") String AuthorizationRpId, String params) {
        logger.info("Api Resource: /uma-rp-get-claims-gathering-url  Params: {}", params);
        return process(CommandType.RP_GET_CLAIMS_GATHERING_URL, params, RpGetClaimsGatheringUrlParams.class, authorization, AuthorizationRpId);
    }

}
