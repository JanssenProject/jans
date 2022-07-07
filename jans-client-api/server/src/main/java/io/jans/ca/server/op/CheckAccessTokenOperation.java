/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.op;

import io.jans.as.client.JwkClient;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.ca.common.CommandType;
import io.jans.ca.common.params.CheckAccessTokenParams;
import io.jans.ca.common.response.CheckAccessTokenResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.service.DiscoveryService;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/10/2013
 */

public class CheckAccessTokenOperation extends BaseOperation<CheckAccessTokenParams> {

    private static final Logger LOG = LoggerFactory.getLogger(CheckAccessTokenOperation.class);

    @Inject
    DiscoveryService discoveryService;

    @Override
    public IOpResponse execute(CheckAccessTokenParams params, HttpServletRequest httpServletRequest) throws Exception {
        final OpenIdConfigurationResponse discoveryResponse = discoveryService.getConnectDiscoveryResponseByRpId(params.getRpId());
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

    @Override
    public Class<CheckAccessTokenParams> getParameterClass() {
        return CheckAccessTokenParams.class;
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.CHECK_ACCESS_TOKEN;
    }

    private boolean isAccessTokenValid(String p_accessToken, Jwt jwt, OpenIdConfigurationResponse discoveryResponse) {
        try {
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
