/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.RpAuthorizeRptParams;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class RpAuthorizeRptOperation extends BaseOperation<RpAuthorizeRptParams> {

    private static final Logger LOG = LoggerFactory.getLogger(RpAuthorizeRptOperation.class);

    protected RpAuthorizeRptOperation(Command command, final Injector injector) {
        super(command, injector, RpAuthorizeRptParams.class);
    }

    @Override
    public CommandResponse execute(RpAuthorizeRptParams params) {

//        final RpAuthorizeRptParams params = asParams(RpAuthorizeRptParams.class);
//        if (CoreUtils.allNotBlank(params.getRptToken(), params.getTicket(), params.getAatToken())) {
//
//            final UmaConfiguration umaDiscovery = getDiscoveryService().getUmaDiscovery(umaDiscoveryUrl);
//            if (umaDiscovery != null) {
//                ClaimTokenList tokenList = new ClaimTokenList();
//                for (Map.Entry<String, List<String>> claim : params.getClaims().entrySet()) {
//                    tokenList.add(new ClaimToken(claim.getKey(), claim.getValue() != null && !claim.getValue().isEmpty() ? claim.getValue().get(0) : ""));
//                }
//
//                final RptAuthorizationRequest authorizationRequest = new RptAuthorizationRequest(params.getRptToken(), params.getTicket());
//                authorizationRequest.setClaims(tokenList);
//
//                LOG.debug("Try to authorize RPT with ticket: {}...", params.getTicket());
//                final RptAuthorizationRequestService rptAuthorizationService = UmaClientFactory.instance().createAuthorizationRequestService(umaDiscovery, getHttpService().getClientExecutor());
//                final RptAuthorizationResponse authorizationResponse = rptAuthorizationService.requestRptPermissionAuthorization(
//                        "Bearer " + params.getAatToken(), params.getAmHost(), authorizationRequest);
//                if (authorizationResponse != null) {
//                    LOG.trace("RPT is authorized. RPT: {} ", params.getRptToken());
//                    // authorized
//                    return CommandResponse.ok();
//                } else {
//                    LOG.trace("Failed to authorize RPT: {}", params.getRptToken());
//                    return CommandResponse.createErrorResponse(ErrorResponseCode.RPT_NOT_AUTHORIZED);
//                }
//            } else {
//                LOG.error("Unable to fetch uma discovery for amHost: {}", params.getAmHost());
//            }
//        } else {
//            return CommandResponse.createErrorResponse(ErrorResponseCode.INVALID_REQUEST);
//        }

        return null;
    }
}
