package org.gluu.oxauth.model.util;

import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaims;
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
