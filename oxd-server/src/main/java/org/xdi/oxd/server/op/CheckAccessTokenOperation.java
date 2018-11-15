/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.JwkClient;
import org.xdi.oxauth.client.OpenIdConfigurationResponse;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jws.RSASigner;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.jwt.JwtHeaderName;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.params.CheckAccessTokenParams;
import org.xdi.oxd.common.response.CheckAccessTokenResponse;
import org.xdi.oxd.common.response.IOpResponse;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/10/2013
 */

public class CheckAccessTokenOperation extends BaseOperation<CheckAccessTokenParams> {

    private static final Logger LOG = LoggerFactory.getLogger(CheckAccessTokenOperation.class);

    protected CheckAccessTokenOperation(Command command, final Injector injector) {
        super(command, injector, CheckAccessTokenParams.class);
    }

    @Override
    public IOpResponse execute(CheckAccessTokenParams params) throws Exception {
        final OpenIdConfigurationResponse discoveryResponse = getDiscoveryService().getConnectDiscoveryResponseByOxdId(params.getOxdId());
        final String idToken = params.getIdToken();
        final String accessToken = params.getAccessToken();

        final Jwt jwt = Jwt.parse(idToken);

        final Date issuedAt = jwt.getClaims().getClaimAsDate(JwtClaimName.ISSUED_AT);
        final Date expiresAt = jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);

        final CheckAccessTokenResponse opResponse = new CheckAccessTokenResponse();
        opResponse.setActive(isAccessTokenValid(accessToken, jwt, discoveryResponse));
        opResponse.setIssuedAt(issuedAt);
        opResponse.setExpiresAt(expiresAt);
        return opResponse;
    }

    private boolean isAccessTokenValid(String p_accessToken, Jwt jwt, OpenIdConfigurationResponse discoveryResponse) {
        try {
            //                final String type = jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE);
            final String algorithm = jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM);
            final String jwkUrl = discoveryResponse.getJwksUri();
            final String kid = jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);

            final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm);

            final RSAPublicKey publicKey = JwkClient.getRSAPublicKey(jwkUrl, kid);
            final RSASigner rsaSigner = new RSASigner(signatureAlgorithm, publicKey);
            return rsaSigner.validateAccessToken(p_accessToken, jwt);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }
}
