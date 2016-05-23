/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.uma.RptAuthorizationRequestService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.ClaimToken;
import org.xdi.oxauth.model.uma.ClaimTokenList;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.RptAuthorizationResponse;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.params.AuthorizeRptParams;
import org.xdi.oxd.server.Utils;

import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class AuthorizeRptOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizeRptOperation.class);

    protected AuthorizeRptOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {

        final AuthorizeRptParams params = asParams(AuthorizeRptParams.class);
        if (CoreUtils.allNotBlank(params.getRptToken(), params.getTicket(), params.getAatToken())) {

            final String umaDiscoveryUrl = Utils.getUmaDiscoveryUrl(params.getAmHost());
            final UmaConfiguration umaDiscovery = getDiscoveryService().getUmaDiscovery(umaDiscoveryUrl);
            if (umaDiscovery != null) {
                ClaimTokenList tokenList = new ClaimTokenList();
                for (Map.Entry<String, List<String>> claim : params.getClaims().entrySet()) {
                    tokenList.add(new ClaimToken(claim.getKey(), claim.getValue() != null && !claim.getValue().isEmpty() ? claim.getValue().get(0) : ""));
                }

                final RptAuthorizationRequest authorizationRequest = new RptAuthorizationRequest(params.getRptToken(), params.getTicket());
                authorizationRequest.setClaims(tokenList);

                LOG.debug("Try to authorize RPT with ticket: {}...", params.getTicket());
                final RptAuthorizationRequestService rptAuthorizationService = UmaClientFactory.instance().createAuthorizationRequestService(umaDiscovery, getHttpService().getClientExecutor());
                final RptAuthorizationResponse authorizationResponse = rptAuthorizationService.requestRptPermissionAuthorization(
                        "Bearer " + params.getAatToken(), params.getAmHost(), authorizationRequest);
                if (authorizationResponse != null) {
                    LOG.trace("RPT is authorized. RPT: {} ", params.getRptToken());
                    // authorized
                    return CommandResponse.ok();
                } else {
                    LOG.trace("Failed to authorize RPT: {}", params.getRptToken());
                    return CommandResponse.createErrorResponse(ErrorResponseCode.RPT_NOT_AUTHORIZED);
                }
            } else {
                LOG.error("Unable to fetch uma discovery for amHost: {}", params.getAmHost());
            }
        } else {
            return CommandResponse.createErrorResponse(ErrorResponseCode.INVALID_REQUEST);
        }

        return null;
    }
}
