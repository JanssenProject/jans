/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.op;

import com.google.inject.Injector;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.ca.common.Command;
import io.jans.ca.common.params.CheckIdTokenParams;
import io.jans.ca.common.response.CheckIdTokenResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.Utils;
import io.jans.ca.server.service.Rp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/10/2013
 */

public class CheckIdTokenOperation extends BaseOperation<CheckIdTokenParams> {

    private static final Logger LOG = LoggerFactory.getLogger(CheckIdTokenOperation.class);

    protected CheckIdTokenOperation(Command command, final Injector injector) {
        super(command, injector, CheckIdTokenParams.class);
    }

    @Override
    public IOpResponse execute(CheckIdTokenParams params) {
        try {
            OpenIdConfigurationResponse discoveryResponse = getDiscoveryService().getConnectDiscoveryResponseByRpId(params.getRpId());

            final Rp rp = getRp();
            final String idToken = params.getIdToken();
            final Jwt jwt = Jwt.parse(idToken);
            final Validator validator = new Validator.Builder()
                    .discoveryResponse(discoveryResponse)
                    .idToken(jwt)
                    .keyService(getKeyService())
                    .opClientFactory(getOpClientFactory())
                    .rpServerConfiguration(getConfigurationService().getConfiguration())
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
}
