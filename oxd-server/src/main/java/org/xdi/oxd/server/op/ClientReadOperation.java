/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.ClientReadParams;
import org.xdi.oxd.common.response.RegisterClientOpResponse;
import org.xdi.oxd.server.Convertor;
import org.xdi.oxd.server.HttpService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/06/2014
 */

public class ClientReadOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(ClientReadOperation.class);

    public ClientReadOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {
        try {
            final ClientReadParams params = asParams(ClientReadParams.class);
            LOG.trace("Start process, params: {}", params);
            if (params != null) {
                RegisterRequest registerRequest = new RegisterRequest(params.getRegistrationAccessToken());
                RegisterClient registerClient = new RegisterClient(params.getRegistrationClientUri());
                registerClient.setExecutor(HttpService.getInstance().getClientExecutor());
                registerClient.setRequest(registerRequest);
                RegisterResponse response = registerClient.exec();

                LOG.trace("RegisterResponse: {}", response);
                if (response != null) {
                    final RegisterClientOpResponse r = Convertor.asRegisterClientOpResponse(response);
                    return okResponse(r);
                } else {
                    LOG.error("There is no response for registerClient.");
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }
}
