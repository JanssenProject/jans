/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import com.google.common.collect.Sets;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.oxauth.model.common.AccessToken;
import org.xdi.oxauth.model.common.AuthorizationGrantInMemory;
import org.xdi.oxauth.model.common.IAuthorizationGrant;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.exception.InvalidClaimException;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.token.JsonWebResponse;
import org.xdi.util.INumGenerator;
import org.xdi.util.security.StringEncrypter;

import java.security.SignatureException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/02/2013
 */

public abstract class AbstractRPTManager implements IRPTManager {

    private static final Log LOG = Logging.getLog(AbstractRPTManager.class);

    public UmaRPT createRPT(IAuthorizationGrant grant, String amHost, String aat) {
        final AccessToken accessToken = (AccessToken) grant.getAccessToken(aat);

        try {
            final Boolean umaRptAsJwt = ConfigurationFactory.instance().getConfiguration().getUmaRptAsJwt();
            if (umaRptAsJwt != null && umaRptAsJwt) {
                return createJwrRpt(grant, accessToken, amHost);
            } else {
                String token = UUID.randomUUID().toString() + "/" + INumGenerator.generate(8);
                return new UmaRPT(token, new Date(), accessToken.getExpirationDate(), grant.getUserId(), grant.getClientId(), amHost);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException("Failed to generate RPT, aat: " + aat, e);
        }
    }

    private UmaRPT createJwrRpt(IAuthorizationGrant grant, AccessToken accessToken, String amHost) throws SignatureException, StringEncrypter.EncryptionException, InvalidJwtException, InvalidJweException, InvalidClaimException {
        final Set<String> scopes = Sets.newHashSet();
        final JsonWebResponse jwr = AuthorizationGrantInMemory.createJwr(grant, UUID.randomUUID().toString(), grant.getAuthorizationCode(), accessToken, scopes);
        Date creationDate = jwr.getClaims().getClaimAsDate(JwtClaimName.ISSUED_AT);
        Date expirationDate = jwr.getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);
        return new UmaRPT(jwr.asString(), creationDate, expirationDate, grant.getUserId(), grant.getClientId(), amHost);
    }
}
