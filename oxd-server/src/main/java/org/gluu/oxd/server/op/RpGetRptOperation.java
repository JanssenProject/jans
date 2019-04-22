/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.model.uma.UmaNeedInfoResponse;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.params.RpGetRptParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.rs.protect.Jackson;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
            return getUmaTokenService().getRpt(params);
        } catch (ClientResponseFailure ex) {
            LOG.trace(ex.getMessage(), ex);
            String entity = (String) ex.getResponse().getEntity(String.class);
            final UmaNeedInfoResponse needInfo = parseNeedInfoSilently(entity);
            if (needInfo != null) {
                LOG.trace("Need info: " + entity);
                throw new WebApplicationException(Response
                        .status(Response.Status.FORBIDDEN)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(Jackson.asJson(needInfo))
                        .build());
            } else {
                LOG.trace("No need_info error, re-throw exception ...", ex);
                throw new WebApplicationException(entity, ex.getResponse().getStatus());
            }
        }
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
