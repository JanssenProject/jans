package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.jboss.resteasy.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.uma.AuthorizationRequestService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.AuthorizationResponse;
import org.xdi.oxauth.model.uma.MetadataConfiguration;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.params.AuthorizeRptParams;
import org.xdi.oxd.server.DiscoveryService;
import org.xdi.oxd.server.HttpService;
import org.xdi.oxd.server.Utils;

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
        try {
            final AuthorizeRptParams params = asParams(AuthorizeRptParams.class);
            if (params != null && CoreUtils.allNotBlank(params.getRptToken(), params.getTicket(), params.getAatToken())) {

                final String umaDiscoveryUrl = Utils.getUmaDiscoveryUrl(params.getAmHost());
                final MetadataConfiguration umaDiscovery = DiscoveryService.getInstance().getUmaDiscovery(umaDiscoveryUrl);
                if (umaDiscovery != null) {
                    final RptAuthorizationRequest authorizationRequest = new RptAuthorizationRequest(params.getRptToken(), params.getTicket());
                    authorizationRequest.setClaims(params.getClaims());

                    LOG.debug("Try to authorize RPT with ticket: {}...", params.getTicket());
                    final AuthorizationRequestService rptAuthorizationService = UmaClientFactory.instance().createAuthorizationRequestService(umaDiscovery, HttpService.getInstance().getClientExecutor());
                    final ClientResponse<AuthorizationResponse> clientAuthorizationResponse = rptAuthorizationService.requestRptPermissionAuthorization(
                            "Bearer " + params.getAatToken(), params.getAmHost(), authorizationRequest);
                    final AuthorizationResponse authorizationResponse = clientAuthorizationResponse.getEntity();
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
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }
}
