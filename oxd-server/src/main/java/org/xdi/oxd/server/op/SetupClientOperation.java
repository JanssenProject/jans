package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.SetupClientParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.SetupClientResponse;
import org.xdi.oxd.server.Utils;
import org.xdi.oxd.server.service.Rp;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/03/2017
 */

public class SetupClientOperation extends BaseOperation<SetupClientParams> {

    private static final Logger LOG = LoggerFactory.getLogger(SetupClientOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected SetupClientOperation(Command command, final Injector injector) {
        super(command, injector, SetupClientParams.class);
    }

    @Override
    public CommandResponse execute(SetupClientParams params) throws Exception {
        try {
            RegisterSiteOperation registerSiteOperation = new RegisterSiteOperation(getCommand(), getInjector());
            RegisterSiteResponse setupClient = registerSiteOperation.execute_(params);
            RegisterSiteResponse registeredClient = registerSiteOperation.execute_(params);

            Rp setup = getSiteService().getRp(setupClient.getOxdId());

            SetupClientResponse response = new SetupClientResponse();
            response.setOxdId(registeredClient.getOxdId());
            response.setOpHost(registeredClient.getOpHost());

            response.setClientId(setup.getClientId());
            response.setClientSecret(setup.getClientSecret());
            response.setClientRegistrationAccessToken(setup.getClientRegistrationAccessToken());
            response.setClientRegistrationClientUri(setup.getClientRegistrationClientUri());
            response.setClientIdIssuedAt(Utils.date(setup.getClientIdIssuedAt()));
            response.setClientSecretExpiresAt(Utils.date(setup.getClientSecretExpiresAt()));

            return okResponse(response);
        } catch (ErrorResponseException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }
}
