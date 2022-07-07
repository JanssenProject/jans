package io.jans.ca.server.op;

import com.google.common.base.Strings;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.model.jwt.Jwt;
import io.jans.ca.common.CommandType;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.ValidateParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.service.DiscoveryService;
import io.jans.ca.server.service.PublicOpKeyService;
import io.jans.ca.server.service.StateService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@RequestScoped
@Named
public class ValidateOperation extends BaseOperation<ValidateParams> {

    @Inject
    DiscoveryService discoveryService;
    @Inject
    PublicOpKeyService publicOpKeyService;
    @Inject
    StateService stateService;
    @Inject
    OpClientFactoryImpl opClientFactory;

   @Override
    public IOpResponse execute(ValidateParams params, HttpServletRequest httpServletRequest) throws Exception {
        validateParams(params);

        Rp rp = getRp(params);
        OpenIdConfigurationResponse discoveryResponse = discoveryService.getConnectDiscoveryResponseByRpId(params.getRpId());

        final Jwt idToken = Jwt.parse(params.getIdToken());

        final Validator validator = new Validator.Builder()
                .discoveryResponse(discoveryResponse)
                .idToken(idToken)
                .keyService(publicOpKeyService)
                .opClientFactory(opClientFactory)
                .rpServerConfiguration(getJansConfigurationService().find())
                .rp(rp)
                .build();
        validator.validateNonce(stateService);
        validator.validateIdToken(rp.getClientId());
        validator.validateAccessToken(params.getAccessToken());
        validator.validateAuthorizationCode(params.getCode());

        return new POJOResponse("");
    }

    @Override
    public Class<ValidateParams> getParameterClass() {
        return ValidateParams.class;
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.VALIDATE;
    }

    private void validateParams(ValidateParams params) {
        if (Strings.isNullOrEmpty(params.getCode())) {
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_CODE);
        }
        if (Strings.isNullOrEmpty(params.getState())) {
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_STATE);
        }
        if (!stateService.isExpiredObjectPresent(params.getState())) {
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_STATE_NOT_VALID);
        }
        if (!Strings.isNullOrEmpty(params.getIdToken())) {
            throw new HttpException(ErrorResponseCode.NO_ID_TOKEN_PARAM);
        }
    }
}
