package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.uma.RptStatusService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.MetadataConfiguration;
import org.xdi.oxauth.model.uma.RptStatusRequest;
import org.xdi.oxauth.model.uma.RptStatusResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.RptStatusParams;
import org.xdi.oxd.common.response.RptStatusOpResponse;
import org.xdi.oxd.server.DiscoveryService;
import org.xdi.oxd.server.HttpService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class RptStatusOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RptStatusOperation.class);

    protected RptStatusOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {
        try {
            final RptStatusParams params = asParams(RptStatusParams.class);
            if (params != null) {
                final MetadataConfiguration umaDiscovery = DiscoveryService.getInstance().getUmaDiscovery(params.getUmaDiscoveryUrl());
                final RptStatusService registrationService = UmaClientFactory.instance().createRptStatusService(umaDiscovery, HttpService.getInstance().getClientExecutor());

                final RptStatusRequest request = new RptStatusRequest();
                request.setRpt(params.getRpt());
                final RptStatusResponse statusResponse = registrationService.requestRptStatus("Bearer " + params.getPatToken(), request);

                if (statusResponse != null) {
                    final RptStatusOpResponse opResponse = new RptStatusOpResponse();
                    opResponse.setActive(statusResponse.getActive());
                    opResponse.setExpiresAt(statusResponse.getExpiresAt());
                    opResponse.setIssuedAt(statusResponse.getIssuedAt());
                    opResponse.setPermissions(statusResponse.getPermissions());
                    return okResponse(opResponse);
                } else {
                    LOG.error("No response on requestRptStatus call from OP.");
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }
}
