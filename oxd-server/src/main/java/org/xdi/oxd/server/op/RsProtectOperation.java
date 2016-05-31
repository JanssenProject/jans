package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.RpGetRptParams;
import org.xdi.oxd.server.service.SiteConfiguration;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

public class RsProtectOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RsProtectOperation.class);

    protected RsProtectOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {

        final RpGetRptParams params = asParams(RpGetRptParams.class);

        SiteConfiguration site = getSite(params.getOxdId());
        UmaConfiguration discovery = getDiscoveryService().getUmaDiscoveryByOxdId(params.getOxdId());

//        final UmaConfiguration umaDiscovery = getDiscoveryService().getUmaDiscovery(params.getUmaDiscoveryUrl());
//                     final ResourceSetRegistrationService registrationService = UmaClientFactory.instance().createResourceSetRegistrationService(umaDiscovery, getHttpService().getClientExecutor());
//
//                     final ResourceSet resourceSet = new ResourceSet();
//                     resourceSet.setName(params.getName());
//                     resourceSet.setScopes(params.getScopes());
//
//                     ResourceSetResponse addResponse = registrationService.addResourceSet("Bearer " + params.getPatToken(), resourceSet);
//                     if (addResponse != null) {
//                         final RegisterResourceOpResponse opResponse = new RegisterResourceOpResponse();
//                         opResponse.setId(addResponse.getId());
//                         return okResponse(opResponse);
//                     } else {
//                         LOG.error("No response on addResourceSet call from OP.");
//                     }
        return null;
    }
}
