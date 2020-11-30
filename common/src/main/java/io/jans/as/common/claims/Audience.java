/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

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
