/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.op;

import com.google.inject.Injector;
import io.jans.ca.server.HttpException;
import org.apache.commons.lang.StringUtils;
import io.jans.as.model.uma.UmaNeedInfoResponse;
import io.jans.as.model.util.Util;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.RpGetRptParams;
import io.jans.ca.common.response.IOpResponse;
import jakarta.ws.rs.ClientErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class RpGetRptOperation extends BaseOperation<RpGetRptParams> {

    private static final Logger LOG = LoggerFactory.getLogger(RpGetRptOperation.class);

    protected RpGetRptOperation(Command command, final Injector injector) {
        super(command, injector, RpGetRptParams.class);
    }

    @Override
    public IOpResponse execute(RpGetRptParams params) throws Exception {
        try {
            validate(params);
            return getUmaTokenService().getRpt(params);
        } catch (ClientErrorException ex) {
            LOG.trace(ex.getMessage(), ex);
            String entity = ex.getResponse().readEntity(String.class);
            return handleRptError(ex.getResponse().getStatus(), entity);
        }
    }

    public static IOpResponse handleRptError(int status, String entity) throws IOException {
        final UmaNeedInfoResponse needInfo = parseNeedInfoSilently(entity);
        if (needInfo != null) {
            LOG.trace("Server response: " + entity);
            throw new WebApplicationException(Response
                    .status(getErrorCode(needInfo))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(Jackson2.asJson(needInfo))
                    .build());
        } else {
            LOG.trace("No need_info error, re-throw ...");
            throw new WebApplicationException(entity, status);
        }
    }

    private void validate(RpGetRptParams params) {
        if (StringUtils.isBlank(params.getTicket())) {
            throw new HttpException(ErrorResponseCode.NO_UMA_TICKET_PARAMETER);
        }

        if ((StringUtils.isBlank(params.getClaimToken()) && StringUtils.isNotBlank(params.getClaimTokenFormat())) ||
                StringUtils.isNotBlank(params.getClaimToken()) && StringUtils.isBlank(params.getClaimTokenFormat())) {
            throw new HttpException(ErrorResponseCode.INVALID_CLAIM_TOKEN_OR_CLAIM_TOKEN_FORMAT);
        }
    }

    private static Status getErrorCode(UmaNeedInfoResponse needInfo) {
        if (StringUtils.isNotBlank(needInfo.getError())) {
            switch (needInfo.getError().toLowerCase()) {
                case "invalid_claim_token_format":
                    return Response.Status.BAD_REQUEST;
                case "invalid_ticket":
                    return Response.Status.BAD_REQUEST;
                default:
                    return Response.Status.FORBIDDEN;
            }
        }
        return Response.Status.FORBIDDEN;
    }

    private static UmaNeedInfoResponse parseNeedInfoSilently(String entity) {
        try {
            // expected need_info error :
            // sample:  {"error":"need_info","ticket":"c024311b-f451-41db-95aa-cd405f16eed4","required_claims":[{"issuer":["https://localhost:8443"],"name":"country","claim_token_format":["http://openid.net/specs/openid-connect-core-1_0.html#IDToken"],"claim_type":"string","friendly_name":"country"},{"issuer":["https://localhost:8443"],"name":"city","claim_token_format":["http://openid.net/specs/openid-connect-core-1_0.html#IDToken"],"claim_type":"string","friendly_name":"city"}],"redirect_user":"https://localhost:8443/restv1/uma/gather_claimsgathering_id=sampleClaimsGathering&&?gathering_id=sampleClaimsGathering&&"}
            return Util.createJsonMapper().readValue(entity, UmaNeedInfoResponse.class);
        } catch (Exception e) {
            return null;
        }
    }
}
