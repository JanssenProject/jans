package org.gluu.oxauth.claims;

import org.gluu.oxauth.model.jwt.JwtClaims;
import org.gluu.oxauth.model.registration.Client;

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
