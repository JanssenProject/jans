package org.gluu.oxauth.claims;

import org.gluu.oxauth.model.jwt.JwtClaims;
import org.gluu.oxauth.model.registration.Client;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 */
public class Audience {

    public Audience() {
    }

    public static void setAudience(JwtClaims claims, Client client) {
        if (claims == null || client == null) {
            return;
        }

        Set<String> audiences = new HashSet<>();
        audiences.add(client.getClientId());
        audiences.addAll(client.getAttributes().getAdditionalAudience());

        for (String audience : audiences) {
            claims.addAudience(audience);
        }
    }
}
