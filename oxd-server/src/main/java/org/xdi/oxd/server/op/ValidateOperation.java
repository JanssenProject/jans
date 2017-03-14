package org.xdi.oxd.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.OpenIdConfigurationResponse;
import org.xdi.oxauth.model.jws.RSASigner;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.ValidateParams;
import org.xdi.oxd.server.service.SiteConfiguration;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/03/2017
 */

public class ValidateOperation extends BaseOperation<ValidateParams> {

    private static final Logger LOG = LoggerFactory.getLogger(ValidateOperation.class);

    /**
     * @param command command
     */
    protected ValidateOperation(Command command, final Injector injector) {
        super(command, injector, ValidateParams.class);
    }

    @Override
    public CommandResponse execute(ValidateParams params) throws Exception {
        validateParams(params);

        SiteConfiguration site = getSite();
        OpenIdConfigurationResponse discoveryResponse = getDiscoveryService().getConnectDiscoveryResponseByOxdId(params.getOxdId());

        if (!Strings.isNullOrEmpty(params.getIdToken())) {
            throw new ErrorResponseException(ErrorResponseCode.NO_ID_TOKEN_PARAM);
        }

        final Jwt idToken = Jwt.parse(params.getIdToken());
        final String nonceFromToken = idToken.getClaims().getClaimAsString(JwtClaimName.NONCE);

        if (!getStateService().isNonceValid(nonceFromToken)) {
            throw new ErrorResponseException(ErrorResponseCode.INVALID_NONCE);
        }

        RSASigner rsaSigner = Validator.createRSASigner(idToken, discoveryResponse);

        // id_token validation
        if (!Validator.isIdTokenValid(idToken, getDiscoveryService().getConnectDiscoveryResponse(site.getOpHost()), nonceFromToken, site.getClientId(), rsaSigner)) {
            LOG.error("ID Token is not valid, token: " + params.getIdToken());
            throw new ErrorResponseException(ErrorResponseCode.INVALID_ID_TOKEN);
        }

        // access_token validation
        if (!Strings.isNullOrEmpty(params.getAccessToken())) {
            if (!rsaSigner.validateAccessToken(params.getAccessToken(), idToken)) {
                throw new ErrorResponseException(ErrorResponseCode.INVALID_ACCESS_TOKEN_BAD_HASH);
            }
        }

        // code validation
        if (!Strings.isNullOrEmpty(params.getCode())) {
            if (!rsaSigner.validateAuthorizationCode(params.getCode(), idToken)) {
                throw new ErrorResponseException(ErrorResponseCode.INVALID_AUTHORIZATION_CODE_BAD_HASH);
            }
        }

        return CommandResponse.ok();
    }

    private void validateParams(ValidateParams params) {
        if (Strings.isNullOrEmpty(params.getCode())) {
            throw new ErrorResponseException(ErrorResponseCode.BAD_REQUEST_NO_CODE);
        }
        if (Strings.isNullOrEmpty(params.getState())) {
            throw new ErrorResponseException(ErrorResponseCode.BAD_REQUEST_NO_STATE);
        }
        if (!getStateService().isStateValid(params.getState())) {
            throw new ErrorResponseException(ErrorResponseCode.BAD_REQUEST_STATE_NOT_VALID);
        }
    }
}
