package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.jans.as.client.BaseTest.clientEngine;
import static org.testng.Assert.*;

public class JweAssertBuilder extends BaseAssertBuilder {

    private Jwe jwe;
    private boolean notNullAccesTokenHash;
    private boolean checkMemberOfClaimNoEmpty;
    private boolean notNullClaimsAddressdata;
    private String[] claimsPresence;
    private String[] claimsNoPresence;

    public JweAssertBuilder(Jwe jwe) {
        this.jwe = jwe;
        this.notNullAccesTokenHash = false;
    }

    public JweAssertBuilder notNullAccesTokenHash() {
        this.notNullAccesTokenHash = true;
        return this;
    }


    public JweAssertBuilder claimsPresence(String... claimsPresence) {
        if (this.claimsPresence != null) {
            List<String> listClaims = new ArrayList<>();
            listClaims.addAll(Arrays.asList(this.claimsPresence));
            listClaims.addAll(Arrays.asList(claimsPresence));
            this.claimsPresence = listClaims.toArray(new String[0]);
        } else {
            this.claimsPresence = claimsPresence;
        }
        return this;
    }

    public JweAssertBuilder claimsNoPresence(String... claimsNoPresence) {
        if (this.claimsNoPresence != null) {
            List<String> listClaims = new ArrayList<>();
            listClaims.addAll(Arrays.asList(this.claimsNoPresence));
            listClaims.addAll(Arrays.asList(claimsNoPresence));
            this.claimsNoPresence = listClaims.toArray(new String[0]);
        } else {
            this.claimsNoPresence = claimsNoPresence;
        }
        return this;
    }


    public JweAssertBuilder claimMemberOfNoEmpty() {
        this.checkMemberOfClaimNoEmpty = true;
        return this;
    }

    public JweAssertBuilder notNullClaimsAddressdata() {
        this.notNullClaimsAddressdata = true;
        return this;
    }

    private void assertNotNullHeaderClaim(String headerClaim) {
        assertNotNull(jwe.getHeader().getClaimAsString(headerClaim), "Jwe Claim Header " + headerClaim + " is null");
    }

    private void assertNotNullClaim(String claim) {
        assertNotNull(jwe.getClaims().getClaimAsString(claim), "Jwe Claim " + claim + " is null");
    }

    @Override
    public void check() {
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

        if (checkMemberOfClaimNoEmpty) {
            assertNotNull(jwe.getClaims().getClaimAsStringList("member_of"));
            assertTrue(jwe.getClaims().getClaimAsStringList("member_of").size() > 1);
        }

        if (notNullClaimsAddressdata) {
            assertNotNullClaim(JwtClaimName.ADDRESS_STREET_ADDRESS);
            assertNotNullClaim(JwtClaimName.ADDRESS_COUNTRY);
            assertNotNull(jwe.getClaims().getClaim(JwtClaimName.ADDRESS));
            assertNotNull(jwe.getClaims().getClaimAsJSON(JwtClaimName.ADDRESS).has(JwtClaimName.ADDRESS_STREET_ADDRESS));
            assertNotNull(jwe.getClaims().getClaimAsJSON(JwtClaimName.ADDRESS).has(JwtClaimName.ADDRESS_COUNTRY));
            assertNotNull(jwe.getClaims().getClaimAsJSON(JwtClaimName.ADDRESS).has(JwtClaimName.ADDRESS_LOCALITY));
            assertNotNull(jwe.getClaims().getClaimAsJSON(JwtClaimName.ADDRESS).has(JwtClaimName.ADDRESS_REGION));
        }

        if (claimsPresence != null) {
            for (String claim : claimsPresence) {
                assertNotNull(claim, "Claim name is null");
                assertNotNull(jwe.getClaims().getClaimAsString(claim), "Jwe Claim " + claim + " is not found");
            }
        }

        if (claimsNoPresence != null) {
            for (String claim : claimsNoPresence) {
                assertNotNull(claim, "Claim name is null");
                assertNull(jwe.getClaims().getClaimAsString(claim), "Jwe Claim " + claim + " is found");
            }
        }

    }
}
