package org.xdi.oxd.server.op;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.uma.ResourceSetPermissionRegistrationService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.MetadataConfiguration;
import org.xdi.oxauth.model.uma.ResourceSetPermissionRequest;
import org.xdi.oxauth.model.uma.ResourceSetPermissionTicket;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.RegisterPermissionTicketParams;
import org.xdi.oxd.common.response.RegisterPermissionTicketOpResponse;
import org.xdi.oxd.server.DiscoveryService;
import org.xdi.oxd.server.HttpService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class RegisterTicketOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterTicketOperation.class);

    protected RegisterTicketOperation(Command p_command) {
        super(p_command);
    }

    @Override
    public CommandResponse execute() {
        try {
            final RegisterPermissionTicketParams params = asParams(RegisterPermissionTicketParams.class);
            if (params != null) {
                final MetadataConfiguration umaDiscovery = DiscoveryService.getInstance().getUmaDiscovery(params.getUmaDiscoveryUrl());
                final ResourceSetPermissionRegistrationService registrationService = UmaClientFactory.instance().
                        createResourceSetPermissionRegistrationService(umaDiscovery, HttpService.getInstance().getClientExecutor());

                final ResourceSetPermissionRequest request = new ResourceSetPermissionRequest();
                request.setResourceSetId(params.getResourceSetId());
                request.setScopes(params.getScopes());

                final ResourceSetPermissionTicket ticketResponse = registrationService.registerResourceSetPermission(
                        "Bearer " + params.getPatToken(), params.getAmHost(), params.getRsHost(), request);

                if (ticketResponse != null) {
                    final RegisterPermissionTicketOpResponse opResponse = new RegisterPermissionTicketOpResponse();
                    opResponse.setTicket(ticketResponse.getTicket());
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
