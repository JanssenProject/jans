/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import org.json.JSONObject;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 */
public class JwtUtilTest {

    @Test
    public void transferIntoJwtClaimsTest() {
        JSONObject json = new JSONObject();
        json.put("active", true);
        json.put("key", "valueTest");

        Jwt jwt = new Jwt();
        JwtUtil.transferIntoJwtClaims(json, jwt);

        final JwtClaims claims = jwt.getClaims();
        assertEquals("true", claims.getClaimAsString("active"));
        assertEquals("valueTest", claims.getClaimAsString("key"));
    }
}
