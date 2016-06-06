package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.RsCheckAccessParams;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

public class RsCheckAccessOperation extends BaseOperation<RsCheckAccessParams> {

    /**
     * Constructor
     *
     * @param command command
     */
    protected RsCheckAccessOperation(Command command, final Injector injector) {
        super(command, injector, RsCheckAccessParams.class);
    }

    @Override
    public CommandResponse execute(RsCheckAccessParams params) throws Exception {

//        final RptStatusService registrationService = UmaClientFactory.instance().createRptStatusService(umaDiscovery, getHttpService().getClientExecutor());
//
//              final RptIntrospectionResponse statusResponse = registrationService.requestRptStatus("Bearer " + params.getPatToken(), params.getRpt(), "");

//        final UmaConfiguration umaDiscovery = getDiscoveryService().getUmaDiscovery(params.getUmaDiscoveryUrl());
//        final PermissionRegistrationService registrationService = UmaClientFactory.instance().
//                createResourceSetPermissionRegistrationService(umaDiscovery, getHttpService().getClientExecutor());
//
//        final UmaPermission request = new UmaPermission();
//        request.setResourceSetId(params.getResourceSetId());
//        request.setScopes(params.getScopes());
//
//        final PermissionTicket ticketResponse = registrationService.registerResourceSetPermission(
//                "Bearer " + params.getPatToken(), params.getAmHost(), request);
//
//        if (ticketResponse != null) {
//            final RegisterPermissionTicketOpResponse opResponse = new RegisterPermissionTicketOpResponse();
//            opResponse.setTicket(ticketResponse.getTicket());
//            return okResponse(opResponse);
//        } else {
//            LOG.error("No response on requestRptStatus call from OP.");
//        }
        return null;
    }
}
