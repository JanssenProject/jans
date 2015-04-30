package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.uma.ResourceSetRegistrationService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.ResourceSet;
import org.xdi.oxauth.model.uma.ResourceSetStatus;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.RegisterResourceParams;
import org.xdi.oxd.common.response.RegisterResourceOpResponse;
import org.xdi.oxd.server.DiscoveryService;
import org.xdi.oxd.server.HttpService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class RegisterResourceOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterResourceOperation.class);

    protected RegisterResourceOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {
        try {
            final RegisterResourceParams params = asParams(RegisterResourceParams.class);
            if (params != null) {
                final UmaConfiguration umaDiscovery = DiscoveryService.getInstance().getUmaDiscovery(params.getUmaDiscoveryUrl());
                final ResourceSetRegistrationService registrationService = UmaClientFactory.instance().createResourceSetRegistrationService(umaDiscovery, HttpService.getInstance().getClientExecutor());

                final ResourceSet resourceSet = new ResourceSet();
                resourceSet.setName(params.getName());
                resourceSet.setScopes(params.getScopes());

                final ResourceSetStatus addResponse = registrationService.addResourceSet("Bearer " + params.getPatToken(), resourceSet);

                if (addResponse != null) {
                    final RegisterResourceOpResponse opResponse = new RegisterResourceOpResponse();
                    opResponse.setId(addResponse.getId());
                    return okResponse(opResponse);
                } else {
                    LOG.error("No response on addResourceSet call from OP.");
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }
}
