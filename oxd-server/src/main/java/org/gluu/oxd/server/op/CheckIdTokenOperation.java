/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.server.op;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import org.gluu.oxauth.client.OpenIdConfigurationResponse;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.params.CheckIdTokenParams;
import org.gluu.oxd.common.response.CheckIdTokenResponse;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.Utils;
import org.gluu.oxd.server.service.Rp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

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
            OpenIdConfigurationResponse discoveryResponse = getDiscoveryService().getConnectDiscoveryResponseByOxdId(params.getOxdId());

            final Rp rp = getRp();
            final String idToken = params.getIdToken();
            final Jwt jwt = Jwt.parse(idToken);
            final Validator validator = new Validator.Builder()
                    .discoveryResponse(discoveryResponse)
                    .idToken(jwt)
                    .keyService(getKeyService())
                    .opClientFactory(getOpClientFactory())
                    .oxdServerConfiguration(getConfigurationService().getConfiguration())
                    .rp(rp)
                    .build();

            //validate at_hash in id_token
            validator.validateAccessToken(params.getToken(), atHashCheckRequired(rp.getResponseTypes()));

            final CheckIdTokenResponse opResponse = new CheckIdTokenResponse();
            opResponse.setActive(validator.isIdTokenValid());
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
        Set<String> types = Sets.newHashSet();
        responseTypes.forEach((s) -> {
            if (s != null && !s.isEmpty()) {
                types.addAll(Lists.newArrayList(s.split(" ")));
            }
        });
        if (types.contains("token") && types.contains("id_token"))
            return true;
        return false;
    }
}
