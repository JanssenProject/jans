package io.jans.as.model.jwt;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Z
 */
public class JwtClaimsTest {

    @Test
    public void setClaimObject_whenSetSameValue_shouldNotCreateDuplicate() {
        JwtClaims claims = new JwtClaims();
        claims.addAudience("client1");

        claims.setClaimObject("aud", "client1", false);
        assertEquals(claims.getClaim("aud"), "client1");
    }

    @Test
    public void setClaimObject_whenSetDifferentValues_shouldCreateCorrectArray() {
        JwtClaims claims = new JwtClaims();
        claims.addAudience("client1");

        claims.setClaimObject("aud", "client2", false);
        assertEquals(claims.getClaim("aud"), Lists.newArrayList("client1", "client2"));
    }

    @Test
    public void setClaimObject_whenSetDifferentValue_shouldCreateCorrectArray() {
        JwtClaims claims = new JwtClaims();
        claims.addAudience("client1");

        claims.setClaimObject("aud", "client2", false);
        claims.setClaimObject("aud", "client3", false);
        assertEquals(claims.getClaim("aud"), Lists.newArrayList("client1", "client2", "client3"));
    }

    @Test
    public void setClaimObject_whenSetDifferentValueWithOverride_shouldOverrideValue() {
        JwtClaims claims = new JwtClaims();
        claims.addAudience("client1");

        claims.setClaimObject("aud", "client2", false);
        claims.setClaimObject("aud", "client3", true);
        assertEquals(claims.getClaim("aud"), "client3");
    }
}
