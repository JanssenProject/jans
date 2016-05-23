/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.uma.PermissionRegistrationService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.PermissionTicket;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxauth.model.uma.UmaPermission;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.RegisterPermissionTicketParams;
import org.xdi.oxd.common.response.RegisterPermissionTicketOpResponse;

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

        final RegisterPermissionTicketParams params = asParams(RegisterPermissionTicketParams.class);

        final UmaConfiguration umaDiscovery = getDiscoveryService().getUmaDiscovery(params.getUmaDiscoveryUrl());
        final PermissionRegistrationService registrationService = UmaClientFactory.instance().
                createResourceSetPermissionRegistrationService(umaDiscovery, getHttpService().getClientExecutor());

        final UmaPermission request = new UmaPermission();
        request.setResourceSetId(params.getResourceSetId());
        request.setScopes(params.getScopes());

        final PermissionTicket ticketResponse = registrationService.registerResourceSetPermission(
                "Bearer " + params.getPatToken(), params.getAmHost(), request);

        if (ticketResponse != null) {
            final RegisterPermissionTicketOpResponse opResponse = new RegisterPermissionTicketOpResponse();
            opResponse.setTicket(ticketResponse.getTicket());
            return okResponse(opResponse);
        } else {
            LOG.error("No response on requestRptStatus call from OP.");
        }

        return null;
    }
}
