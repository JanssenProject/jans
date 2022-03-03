package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;

import static io.jans.as.client.BaseTest.clientEngine;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class JweAssertBuilder extends BaseAssertBuilder {

    private Jwe jwe;
    private boolean notNullAccesTokenHash;

    public JweAssertBuilder(Jwe jwe) {
        this.jwe = jwe;
        this.notNullAccesTokenHash = false;
    }

    public JweAssertBuilder notNullAccesTokenHash() {
        this.notNullAccesTokenHash = true;
        return this;
    }

    private void assertNotNullHeaderClaim(String headerClaim) {
        assertNotNull(jwe.getHeader().getClaimAsString(headerClaim), "Jwe Claim Header " + headerClaim + " is null");
    }

    private void assertNotNullClaim(String claim) {
        assertNotNull(jwe.getClaims().getClaimAsString(claim), "Jwe Claim " + claim + " is null");
    }

    @Override
    public void checkAsserts() {
        assertNotNull(jwe, "Jwe is null");
        assertNotNullHeaderClaim(JwtHeaderName.TYPE);
        assertNotNullHeaderClaim(JwtHeaderName.ALGORITHM);
        assertNotNullClaim(JwtClaimName.ISSUER);
        assertNotNullClaim(JwtClaimName.AUDIENCE);
        assertNotNullClaim(JwtClaimName.EXPIRATION_TIME);
        assertNotNullClaim(JwtClaimName.ISSUED_AT);
        assertNotNullClaim(JwtClaimName.SUBJECT_IDENTIFIER);
        assertNotNullClaim(JwtClaimName.AUTHENTICATION_TIME);

        if (notNullAccesTokenHash)
            assertNotNullClaim(JwtClaimName.ACCESS_TOKEN_HASH);
    }
}
