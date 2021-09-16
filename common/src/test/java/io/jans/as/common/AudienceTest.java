/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common;

import com.google.common.collect.Lists;
import io.jans.as.common.claims.Audience;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.jwt.JwtClaims;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 */
public class AudienceTest {

    @Test
    public void addAudience_callItTwiceWithSameValue_shouldResultInSingleAudValue() {
        JwtClaims claims = new JwtClaims();
        claims.addAudience("aud1");
        claims.addAudience("aud1");

        assertEquals(claims.getClaimAsString("aud"), "aud1");
    }

    @Test
    public void addAudience_callItTwiceWithDifferentValues_shouldResultInSingleAudValue() {
        JwtClaims claims = new JwtClaims();
        claims.addAudience("aud1");
        claims.addAudience("aud2");

        assertEquals(claims.getClaim("aud"), Lists.newArrayList("aud1", "aud2"));
    }

    @Test
    public void setAudience_withAdditionalClaims_shouldResultInAdditionalClaimsPresentinAud() {
        JwtClaims claims = new JwtClaims();

        Client client = new Client();
        client.setClientId("clientId");
        client.getAttributes().setAdditionalAudience(Lists.newArrayList("aud1", "aud2"));

        Audience.setAudience(claims, client);
        assertEquals(claims.getClaim("aud"), Lists.newArrayList("clientId", "aud1", "aud2"));
    }
}
