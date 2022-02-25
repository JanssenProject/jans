package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.JwkClient;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import static io.jans.as.client.BaseTest.clientEngine;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class JwtAssertBuilder extends AssertBuilder {

    private Jwt jwt;
    private boolean notNullAccesTokenHash;
    private boolean notNullAuthenticationTime;
    private boolean notNullOxOpenIDConnectVersion;
    private boolean notNullAuthenticationContextClassReference;
    private boolean notNullAuthenticationMethodReferences;
    private boolean notNullClaimsAddressdata;
    private String[] claimsPresence;

    private RSAPublicKey publicKey;
    private SignatureAlgorithm signatureAlgorithm;

    public JwtAssertBuilder(Jwt jwt) {
        this.jwt = jwt;
        this.notNullAccesTokenHash = false;
        this.notNullAuthenticationTime = false;
        this.notNullOxOpenIDConnectVersion = false;
        this.notNullAuthenticationContextClassReference = false;
        this.notNullAuthenticationMethodReferences = false;
        this.claimsPresence = null;

        this.publicKey = null;
        this.signatureAlgorithm = null;
    }

    public JwtAssertBuilder notNullAccesTokenHash() {
        this.notNullAccesTokenHash = true;
        return this;
    }

    public JwtAssertBuilder notNullAuthenticationTime() {
        this.notNullAuthenticationTime = true;
        return this;
    }

    public JwtAssertBuilder notNullOxOpenIDConnectVersion() {
        this.notNullOxOpenIDConnectVersion = true;
        return this;
    }

    public JwtAssertBuilder notNullAuthenticationContextClassReference() {
        this.notNullAuthenticationContextClassReference = true;
        return this;
    }

    public JwtAssertBuilder notNullAuthenticationMethodReferences() {
        this.notNullAuthenticationMethodReferences = true;
        return this;
    }

    public JwtAssertBuilder notNullClaimsAddressdata() {
        this.notNullClaimsAddressdata = true;
        return this;
    }

    public JwtAssertBuilder claimsPresence(String... claimsPresence) {
        this.claimsPresence = claimsPresence;
        return this;
    }

    public JwtAssertBuilder validateIdToken(String idToken, String jwksUri, SignatureAlgorithm signatureAlgorithm) throws InvalidJwtException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this.jwt = Jwt.parse(idToken);
        this.publicKey = JwkClient.getRSAPublicKey(jwksUri, jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID), clientEngine(true));
        this.signatureAlgorithm = signatureAlgorithm;
        return this;
    }

    private void assertNotNullHeaderClaim(String headerClaim) {
        assertNotNull(jwt.getHeader().getClaimAsString(headerClaim), "Jwt Claim Header " + headerClaim + " is null");
    }

    private void assertNotNullClaim(String claim) {
        assertNotNull(jwt.getClaims().getClaimAsString(claim), "Jwt Claim " + claim + " is null");
    }

    @Override
    public void checkAsserts() {
        assertNotNull(jwt, "Jwt is null");
        assertNotNullHeaderClaim(JwtHeaderName.TYPE);
        assertNotNullHeaderClaim(JwtHeaderName.ALGORITHM);
        assertNotNullClaim(JwtClaimName.ISSUER);
        assertNotNullClaim(JwtClaimName.AUDIENCE);
        assertNotNullClaim(JwtClaimName.EXPIRATION_TIME);
        assertNotNullClaim(JwtClaimName.ISSUED_AT);
        assertNotNullClaim(JwtClaimName.SUBJECT_IDENTIFIER);

        if (notNullAccesTokenHash)
            assertNotNullClaim(JwtClaimName.ACCESS_TOKEN_HASH);
        if (notNullAuthenticationContextClassReference)
            assertNotNullClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE);
        if (notNullAuthenticationMethodReferences)
            assertNotNullClaim(JwtClaimName.AUTHENTICATION_METHOD_REFERENCES);
        if (notNullAuthenticationTime)
            assertNotNullClaim(JwtClaimName.AUTHENTICATION_TIME);
        if (notNullOxOpenIDConnectVersion)
            assertNotNullClaim(JwtClaimName.OX_OPENID_CONNECT_VERSION);
        if (notNullClaimsAddressdata) {
            assertNotNullClaim(JwtClaimName.ADDRESS_STREET_ADDRESS);
            assertNotNullClaim(JwtClaimName.ADDRESS_COUNTRY);
            assertNotNull(jwt.getClaims().getClaim(JwtClaimName.ADDRESS));
            assertNotNull(jwt.getClaims().getClaimAsJSON(JwtClaimName.ADDRESS).has(JwtClaimName.ADDRESS_STREET_ADDRESS));
            assertNotNull(jwt.getClaims().getClaimAsJSON(JwtClaimName.ADDRESS).has(JwtClaimName.ADDRESS_COUNTRY));
            assertNotNull(jwt.getClaims().getClaimAsJSON(JwtClaimName.ADDRESS).has(JwtClaimName.ADDRESS_LOCALITY));
            assertNotNull(jwt.getClaims().getClaimAsJSON(JwtClaimName.ADDRESS).has(JwtClaimName.ADDRESS_REGION));
        }

        if (claimsPresence != null) {
            for (String claim : claimsPresence) {
                assertNotNull(claim, "Jwt Claim " + claim + " is not found");
            }
        }

        if (signatureAlgorithm != null && publicKey != null) {
            RSASigner rsaSigner = new RSASigner(signatureAlgorithm, publicKey);
            assertTrue(rsaSigner.validate(jwt));
        }
    }
}
