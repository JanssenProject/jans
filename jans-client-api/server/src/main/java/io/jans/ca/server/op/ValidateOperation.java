package io.jans.ca.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.model.jwt.Jwt;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.ValidateParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.service.Rp;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/03/2017
 */

public class ValidateOperation extends BaseOperation<ValidateParams> {

    //private static final Logger LOG = LoggerFactory.getLogger(ValidateOperation.class);

    /**
     * @param command command
     */
    protected ValidateOperation(Command command, final Injector injector) {
        super(command, injector, ValidateParams.class);
    }

    @Override
    public IOpResponse execute(ValidateParams params) throws Exception {
        validateParams(params);

        Rp rp = getRp();
        OpenIdConfigurationResponse discoveryResponse = getDiscoveryService().getConnectDiscoveryResponseByRpId(params.getRpId());

        final Jwt idToken = Jwt.parse(params.getIdToken());

        final Validator validator = new Validator.Builder()
                .discoveryResponse(discoveryResponse)
                .idToken(idToken)
                .keyService(getKeyService())
                .opClientFactory(getOpClientFactory())
                .rpServerConfiguration(getConfigurationService().getConfiguration())
                .rp(rp)
                .build();
        validator.validateNonce(getStateService());
        validator.validateIdToken(rp.getClientId());
        validator.validateAccessToken(params.getAccessToken());
        validator.validateAuthorizationCode(params.getCode());

        return new POJOResponse("");
    }

    private void validateParams(ValidateParams params) {
        if (Strings.isNullOrEmpty(params.getCode())) {
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_CODE);
        }
        if (Strings.isNullOrEmpty(params.getState())) {
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_STATE);
        }
        if (!getStateService().isExpiredObjectPresent(params.getState())) {
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_STATE_NOT_VALID);
        }
        if (!Strings.isNullOrEmpty(params.getIdToken())) {
            throw new HttpException(ErrorResponseCode.NO_ID_TOKEN_PARAM);
        }
    }
}
