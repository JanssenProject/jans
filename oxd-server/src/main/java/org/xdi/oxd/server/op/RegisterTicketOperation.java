package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.uma.PermissionRegistrationService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.RegisterPermissionRequest;
import org.xdi.oxauth.model.uma.ResourceSetPermissionTicket;
import org.xdi.oxauth.model.uma.UmaConfiguration;
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

    protected RegisterTicketOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {
        try {
            final RegisterPermissionTicketParams params = asParams(RegisterPermissionTicketParams.class);
            if (params != null) {
                final UmaConfiguration umaDiscovery = DiscoveryService.getInstance().getUmaDiscovery(params.getUmaDiscoveryUrl());
                final PermissionRegistrationService registrationService = UmaClientFactory.instance().
                        createResourceSetPermissionRegistrationService(umaDiscovery, HttpService.getInstance().getClientExecutor());

                final RegisterPermissionRequest request = new RegisterPermissionRequest();
                request.setResourceSetId(params.getResourceSetId());
                request.setScopes(params.getScopes());

                final ResourceSetPermissionTicket ticketResponse = registrationService.registerResourceSetPermission(
                        "Bearer " + params.getPatToken(), params.getAmHost(), request);

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
