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
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.CheckIdTokenParams;
import org.xdi.oxd.common.response.CheckIdTokenResponse;
import org.xdi.oxd.server.DiscoveryService;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/10/2013
 */

public class CheckIdTokenOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(CheckIdTokenOperation.class);

    protected CheckIdTokenOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {
        try {
            final CheckIdTokenParams params = asParams(CheckIdTokenParams.class);
            if (params != null) {
                final OpenIdConfigurationResponse discoveryResponse = DiscoveryService.getInstance().getDiscoveryResponse(params.getDiscoveryUrl());

                final String idToken = params.getIdToken();
                final Jwt jwt = Jwt.parse(idToken);

                final Date issuedAt = jwt.getClaims().getClaimAsDate(JwtClaimName.ISSUED_AT);
                final Date expiresAt = jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);

                final CheckIdTokenResponse opResponse = new CheckIdTokenResponse();
                opResponse.setActive(isValid(jwt, discoveryResponse));
                opResponse.setIssuedAt(issuedAt != null ? issuedAt.getTime() / 1000 : 0);
                opResponse.setExpiresAt(expiresAt != null ? expiresAt.getTime() / 1000 : 0);
                opResponse.setClaims(jwt.getClaims().toMap());
                return okResponse(opResponse);
            }
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }

    private boolean isValid(Jwt jwt, OpenIdConfigurationResponse p_discoveryResponse) {
        try {
            //                final String type = jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE);
            final String algorithm = jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM);
            final String jwkUrl = p_discoveryResponse.getJwksUri();
            final String kid = jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);

            final String issuer = jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER);
            final Date expiresAt = jwt.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);
            if (new Date().after(expiresAt)) {
                return false;
            }

            // 1. validate issuer
            if (!issuer.equals(p_discoveryResponse.getIssuer())) {
                return false;
            }

            // 2. validate signature
            final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromName(algorithm);

            final RSAPublicKey publicKey = JwkClient.getRSAPublicKey(jwkUrl, kid);
            final RSASigner rsaSigner = new RSASigner(signatureAlgorithm, publicKey);
            return rsaSigner.validate(jwt);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }
}
