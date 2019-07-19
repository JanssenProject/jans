package org.gluu.oxd.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.gluu.oxauth.client.OpenIdConfigurationResponse;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.params.ValidateParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.POJOResponse;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.service.Rp;

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

        Rp site = getRp();
        OpenIdConfigurationResponse discoveryResponse = getDiscoveryService().getConnectDiscoveryResponseByOxdId(params.getOxdId());

        final Jwt idToken = Jwt.parse(params.getIdToken());

        final Validator validator = new Validator(idToken, discoveryResponse, getKeyService());
        validator.validateNonce(getStateService());
        validator.validateIdToken(site.getClientId());
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
        if (!getStateService().isStateValid(params.getState())) {
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_STATE_NOT_VALID);
        }
        if (!Strings.isNullOrEmpty(params.getIdToken())) {
            throw new HttpException(ErrorResponseCode.NO_ID_TOKEN_PARAM);
        }
    }
}
