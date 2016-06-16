/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.uma.RptAuthorizationRequestService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.RptAuthorizationResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.RpAuthorizeRptParams;
import org.xdi.oxd.common.response.RpAuthorizeRptResponse;

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
        validate(params);

        final RptAuthorizationRequest authorizationRequest = new RptAuthorizationRequest(params.getRpt(), params.getTicket());

        LOG.debug("Try to authorize RPT with ticket: {}...", params.getTicket());
        final RptAuthorizationRequestService rptAuthorizationService = UmaClientFactory.instance().createAuthorizationRequestService(
                getDiscoveryService().getUmaDiscoveryByOxdId(params.getOxdId()), getHttpService().getClientExecutor());
        final RptAuthorizationResponse authorizationResponse = rptAuthorizationService.requestRptPermissionAuthorization(
                "Bearer " + getUmaTokenService().getAat(params.getOxdId()).getToken(), getSite().opHostWithoutProtocol(), authorizationRequest);
        if (authorizationResponse != null) {
            LOG.trace("RPT is authorized. RPT: {} ", params.getRpt());
            return okResponse(new RpAuthorizeRptResponse(params.getOxdId()));
        }

        return CommandResponse.createErrorResponse(ErrorResponseCode.RPT_NOT_AUTHORIZED);
    }

    private void validate(RpAuthorizeRptParams params) {
        if (Strings.isNullOrEmpty(params.getTicket())) {
            throw new ErrorResponseException(ErrorResponseCode.NO_UMA_TICKET_PARAMETER);
        }
        if (Strings.isNullOrEmpty(params.getRpt())) {
            throw new ErrorResponseException(ErrorResponseCode.NO_UMA_RPT_PARAMETER);
        }
    }
}
