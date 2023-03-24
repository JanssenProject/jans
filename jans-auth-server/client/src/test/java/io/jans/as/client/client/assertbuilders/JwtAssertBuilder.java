package io.jans.as.client.client.assertbuilders;

import io.jans.as.client.JwkClient;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jws.*;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.jans.as.client.BaseTest.clientEngine;
import static io.jans.as.client.client.Asserter.assertNotBlank;
import static org.testng.Assert.*;

public class JwtAssertBuilder extends BaseAssertBuilder {

    private Jwt jwt;
    private boolean notNullAccesTokenHash;
    private boolean notNullAuthenticationTime;
    private boolean notNullJansOpenIDConnectVersion;
    private boolean notNullAuthenticationContextClassReference;
    private boolean notNullAuthenticationMethodReferences;
    private boolean notNullClaimsAddressdata;
    private boolean checkMemberOfClaimNoEmpty;
    private boolean notBlankDsHash;
    private String[] claimsPresence;
    private String[] claimsNoPresence;

    private AbstractJwsSigner jwtSigner;
    private String authorizationCode;
    private String accessToken;
    private String state;

    public JwtAssertBuilder(Jwt jwt) {
        this.jwt = jwt;
        this.notNullAccesTokenHash = false;
        this.notNullAuthenticationTime = false;
        this.notNullJansOpenIDConnectVersion = false;
        this.notNullAuthenticationContextClassReference = false;
        this.notNullAuthenticationMethodReferences = false;
        this.claimsPresence = null;

        this.jwtSigner = null;
    }

    public JwtAssertBuilder notNullAccesTokenHash() {
        this.notNullAccesTokenHash = true;
        return this;
    }

    public JwtAssertBuilder notBlankDsHash() {
        notBlankDsHash = true;
        return this;
    }

    public JwtAssertBuilder notNullAuthenticationTime() {
        this.notNullAuthenticationTime = true;
        return this;
    }

    public JwtAssertBuilder notNullJansOpenIDConnectVersion() {
        this.notNullJansOpenIDConnectVersion = true;
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

    public JwtAssertBuilder claimsNoPresence(String... claimsNoPresence) {
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

    public JwtAssertBuilder authorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
        return this;
    }

    public JwtAssertBuilder state(String state) {
        this.state = state;
        return this;
    }

    public JwtAssertBuilder accessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    private void assertNotNullHeaderClaim(String headerClaim) {
        assertNotNull(jwt.getHeader().getClaimAsString(headerClaim), "Jwt Claim Header " + headerClaim + " is null");
    }

    private void assertNotNullClaim(String claim) {
        assertNotNull(jwt.getClaims().getClaimAsString(claim), "Jwt Claim " + claim + " is null");
    }

    public JwtAssertBuilder claimMemberOfNoEmpty() {
        this.checkMemberOfClaimNoEmpty = true;
        return this;
    }

    public JwtAssertBuilder validateSignatureRSA(String jwksUri, SignatureAlgorithm signatureAlgorithm) {
        this.jwtSigner = new RSASigner(signatureAlgorithm, JwkClient.getRSAPublicKey(jwksUri, jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID)));
        return this;
    }

    public JwtAssertBuilder validateSignatureRSAClientEngine(String jwksUri, SignatureAlgorithm signatureAlgorithm) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this.jwtSigner = new RSASigner(signatureAlgorithm, JwkClient.getRSAPublicKey(jwksUri, jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID), clientEngine(true)));
        return this;
    }

    public JwtAssertBuilder validateSignatureECDSA(String jwksUri, SignatureAlgorithm signatureAlgorithm) {
        this.jwtSigner = new ECDSASigner(signatureAlgorithm, JwkClient.getECDSAPublicKey(jwksUri, jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID)));
        return this;
    }

    public JwtAssertBuilder validateSignatureHMAC(SignatureAlgorithm signatureAlgorithm, String clientSecret) {
        this.jwtSigner = new HMACSigner(signatureAlgorithm, clientSecret);
        return this;
    }

    public JwtAssertBuilder validateSignaturePlainText() {
        this.jwtSigner = new PlainTextSignature();
        return this;
    }

    @Override
    public void check() {
        assertNotNull(jwt, "Jwt is null");
        assertNotNullHeaderClaim(JwtHeaderName.TYPE);
        assertNotNullHeaderClaim(JwtHeaderName.ALGORITHM);
        assertNotNullClaim(JwtClaimName.ISSUER);
        assertNotNullClaim(JwtClaimName.AUDIENCE);
        assertNotNullClaim(JwtClaimName.EXPIRATION_TIME);
        assertNotNullClaim(JwtClaimName.ISSUED_AT);
        assertNotNullClaim(JwtClaimName.SUBJECT_IDENTIFIER);

        if (notNullAuthenticationTime)
            assertNotNullClaim(JwtClaimName.AUTHENTICATION_TIME);
        if (notNullAccesTokenHash)
            assertNotNullClaim(JwtClaimName.ACCESS_TOKEN_HASH);
        if (notNullJansOpenIDConnectVersion)
            assertNotNullClaim(JwtClaimName.JANS_OPENID_CONNECT_VERSION);
        if (notNullAuthenticationContextClassReference)
            assertNotNullClaim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE);
        if (notNullAuthenticationMethodReferences)
            assertNotNullClaim(JwtClaimName.AUTHENTICATION_METHOD_REFERENCES);
        if (checkMemberOfClaimNoEmpty) {
            assertNotNull(jwt.getClaims().getClaimAsStringList("member_of"));
            assertTrue(jwt.getClaims().getClaimAsStringList("member_of").size() > 1);
        }

        if (notBlankDsHash) {
            assertNotBlank(jwt.getClaims().getClaimAsString("ds_hash"), "ds_hash claim is not present");
        }

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
                assertNotNull(claim, "Claim name is null");
                assertNotNull(jwt.getClaims().getClaimAsString(claim), "Jwt Claim " + claim + " is not found");
            }
        }

        if (claimsNoPresence != null) {
            for (String claim : claimsNoPresence) {
                assertNotNull(claim, "Claim name is null");
                assertNull(jwt.getClaims().getClaimAsString(claim), "Jwt Claim " + claim + " is found");
            }
        }

        if (jwtSigner != null) {
            assertTrue(jwtSigner.validate(jwt));

            if (authorizationCode != null) {
//                assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.CODE_HASH));
                assertTrue(jwtSigner.validateAuthorizationCode(authorizationCode, jwt));
            }
            if (accessToken != null) {
                assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ACCESS_TOKEN_HASH));
                assertTrue(jwtSigner.validateAccessToken(accessToken, jwt));
            }
            if (state != null) {
                assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.STATE_HASH));
                assertTrue(jwtSigner.validateState(state, jwt));
            }
        }
    }
}
