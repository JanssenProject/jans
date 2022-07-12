package io.jans.ca.server.op;

import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.ca.common.params.CheckIdTokenParams;
import io.jans.ca.common.response.CheckIdTokenResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.Utils;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.service.DiscoveryService;
import io.jans.ca.server.service.PublicOpKeyService;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/10/2013
 */

public class CheckIdTokenOperation extends BaseOperation<CheckIdTokenParams> {

    private static final Logger LOG = LoggerFactory.getLogger(CheckIdTokenOperation.class);

    @Inject
    DiscoveryService discoveryService;
    @Inject
    OpClientFactoryImpl opClientFactory;
    @Inject
    PublicOpKeyService publicOpKeyService;

    @Override
    public IOpResponse execute(CheckIdTokenParams params, HttpServletRequest httpServletRequest) {
        try {
            OpenIdConfigurationResponse discoveryResponse = discoveryService.getConnectDiscoveryResponseByRpId(params.getRpId());

            final Rp rp = getRp(params);
            final String idToken = params.getIdToken();
            final Jwt jwt = Jwt.parse(idToken);
            final Validator validator = new Validator.Builder()
                    .discoveryResponse(discoveryResponse)
                    .idToken(jwt)
                    .keyService(publicOpKeyService)
                    .opClientFactory(opClientFactory)
                    .rpServerConfiguration(getJansConfigurationService().find())
                    .rp(rp)
                    .build();

            //validate at_hash in id_token
            validator.validateAccessToken(params.getAccessToken(), atHashCheckRequired(rp.getResponseTypes()));
            //validate c_hash in id_token
            validator.validateAuthorizationCode(params.getCode());
            //validate s_hash in id_token
            validator.validateState(params.getState());

            final CheckIdTokenResponse opResponse = new CheckIdTokenResponse();
            opResponse.setActive(validator.isIdTokenValid(params.getNonce()));
            opResponse.setIssuedAt(Utils.date(jwt.getClaims().getClaimAsDate(JwtClaimName.ISSUED_AT)));
            opResponse.setExpiresAt(Utils.date(jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME)));
            opResponse.setClaims(jwt.getClaims().toMap());
            return opResponse;
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        throw HttpException.internalError();
    }

    public static boolean atHashCheckRequired(List<String> responseTypes) {
        return responseTypes.stream().anyMatch(s -> ResponseType.fromString(s, " ").contains(ResponseType.TOKEN));
    }

    @Override
    public Class<CheckIdTokenParams> getParameterClass() {
        return CheckIdTokenParams.class;
    }

    @Override
    public boolean isAuthorizationRequired() {
        return true;
    }

    @Override
    public String getReturnType() {
        return MediaType.APPLICATION_JSON;
    }

}
