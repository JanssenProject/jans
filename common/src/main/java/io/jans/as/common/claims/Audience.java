package io.jans.as.common.claims;

import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.common.model.registration.Client;

/**
 * @author Yuriy Zabrovarnyy
 */
public class Audience {

    private Audience() {
    }

    public static void setAudience(JwtClaims claims, Client client) {
        if (claims == null || client == null) {
            return;
        }

        claims.addAudience(client.getClientId());
        client.getAttributes().getAdditionalAudience().forEach(claims::addAudience);
    }
}
