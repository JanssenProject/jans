package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.SetupClientParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.SetupClientResponse;
import org.xdi.oxd.server.Utils;
import org.xdi.oxd.server.service.Rp;

import java.util.ArrayList;
import java.util.List;

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

            List<String> grantTypes = params.getGrantType();
            prepareSetupParams(params);
            RegisterSiteResponse setupClient = registerSiteOperation.execute_(params);

            params.setGrantType(grantTypes);
            RegisterSiteResponse registeredClient = registerSiteOperation.execute_(params);

            Rp setup = getRpService().getRp(setupClient.getOxdId());
            Rp registered = getRpService().getRp(registeredClient.getOxdId());

            registered.setSetupOxdId(setup.getOxdId());
            registered.setSetupClientId(setup.getClientId());
            getRpService().update(registered);

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

    private void prepareSetupParams(SetupClientParams params) {
        if (params.getGrantType() == null) {
            params.setGrantType(new ArrayList<String>());
        }
        if (!params.getGrantType().contains(GrantType.CLIENT_CREDENTIALS.getValue())) {
            params.getGrantType().add(GrantType.CLIENT_CREDENTIALS.getValue());
        }
    }
}
