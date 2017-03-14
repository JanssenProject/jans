/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.OpenIdConfigurationResponse;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.CheckIdTokenParams;
import org.xdi.oxd.common.response.CheckIdTokenResponse;
import org.xdi.oxd.server.service.SiteConfiguration;

import java.util.Date;

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
    public CommandResponse execute(CheckIdTokenParams params) throws Exception {

        OpenIdConfigurationResponse discoveryResponse = getDiscoveryService().getConnectDiscoveryResponseByOxdId(params.getOxdId());

        final SiteConfiguration site = getSite();
        final String idToken = params.getIdToken();
        final Jwt jwt = Jwt.parse(idToken);
        final Validator validator = new Validator(jwt, discoveryResponse);

        final Date issuedAt = jwt.getClaims().getClaimAsDate(JwtClaimName.ISSUED_AT);
        final Date expiresAt = jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);

        final CheckIdTokenResponse opResponse = new CheckIdTokenResponse();
        opResponse.setActive(validator.isIdTokenValid(site.getClientId()));
        opResponse.setIssuedAt(issuedAt != null ? issuedAt.getTime() / 1000 : 0);
        opResponse.setExpiresAt(expiresAt != null ? expiresAt.getTime() / 1000 : 0);
        opResponse.setClaims(jwt.getClaims().toMap());
        return okResponse(opResponse);
    }

}
