/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.codehaus.jackson.node.POJONode;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.model.uma.UmaNeedInfoResponse;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxd.common.*;
import org.xdi.oxd.common.params.RpGetRptParams;

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
    public CommandResponse execute(RpGetRptParams params) throws Exception {
        try {
            return okResponse(getUmaTokenService().getRpt(params));
        } catch (ClientResponseFailure ex) {
            LOG.trace(ex.getMessage(), ex);
            String entity = (String) ex.getResponse().getEntity(String.class);
            final UmaNeedInfoResponse needInfo = parseNeedInfoSilently(entity);
            if (needInfo != null) {
                ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.UMA_NEED_INFO);
                errorResponse.setDetails(new POJONode(needInfo));
                return CommandResponse.createErrorResponse(errorResponse);
            } else {
                LOG.trace("No need_info error, re-throw exception ...", ex);
                throw new HttpErrorResponseException(ex.getResponse().getStatus(), entity);
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
