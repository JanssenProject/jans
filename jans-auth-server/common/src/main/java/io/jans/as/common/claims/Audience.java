/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.claims;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.jwt.JwtClaims;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static io.jans.as.model.jwt.JwtClaimName.AUDIENCE;

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

        Set<String> audiences = new LinkedHashSet<>();
        Object currentAudience = claims.getClaim(AUDIENCE);
        if (currentAudience instanceof String) {
            audiences.add((String) currentAudience);
        } else if (currentAudience instanceof Collection) {
            ((Collection<?>) currentAudience).forEach(a -> audiences.add(String.valueOf(a)));
        }

        addIfNotBlank(audiences, client.getClientId());
        client.getAttributes().getAdditionalAudience().forEach(a -> addIfNotBlank(audiences, a));

        if (audiences.isEmpty()) {
            return;
        }
        if (audiences.size() == 1) {
            claims.setAudience(audiences.iterator().next());
            return;
        }
        claims.setClaim(AUDIENCE, new ArrayList<>(audiences));
    }

    private static void addIfNotBlank(Set<String> audiences, String audience) {
        if (StringUtils.isNotBlank(audience)) {
            audiences.add(audience);
        }
    }
}
