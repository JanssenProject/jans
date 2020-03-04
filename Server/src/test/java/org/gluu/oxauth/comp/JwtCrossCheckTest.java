package org.gluu.oxauth.comp;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.crypto.signature.AlgorithmFamily;
import org.gluu.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jwk.Use;
import org.gluu.oxauth.model.jws.AbstractJwsSigner;
import org.gluu.oxauth.model.jws.ECDSASigner;
import org.gluu.oxauth.model.jws.RSASigner;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtType;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.security.KeyStoreException;
import java.security.interfaces.ECPublicKey;
import java.util.Date;
import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 */
public class JwtCrossCheckTest extends BaseTest {

    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret" })
    @Test
    public void rs256CrossCheck(final String dnName,
                              final String keyStoreFile,
                              final String keyStoreSecret) throws Exception {
        crossCheck(new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName), SignatureAlgorithm.RS256);
    }

    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret" })
    @Test
    public void rs384CrossCheck(final String dnName,
                                final String keyStoreFile,
                                final String keyStoreSecret) throws Exception {
        crossCheck(new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName), SignatureAlgorithm.RS384);
    }

    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret" })
    @Test
    public void rs512CrossCheck(final String dnName,
                                final String keyStoreFile,
                                final String keyStoreSecret) throws Exception {
        crossCheck(new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName), SignatureAlgorithm.RS512);
    }

    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret" })
    @Test
    public void es256CrossCheck(final String dnName,
                                final String keyStoreFile,
                                final String keyStoreSecret) throws Exception {
        crossCheck(new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName), SignatureAlgorithm.ES256);
    }

    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret" })
    @Test
    public void es384CrossCheck(final String dnName,
                                final String keyStoreFile,
                                final String keyStoreSecret) throws Exception {
        crossCheck(new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName), SignatureAlgorithm.ES384);
    }

    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret" })
    @Test
    public void es512CrossCheck(final String dnName,
                                final String keyStoreFile,
                                final String keyStoreSecret) throws Exception {
        crossCheck(new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName), SignatureAlgorithm.ES512);
    }

    private void crossCheck(OxAuthCryptoProvider cryptoProvider, SignatureAlgorithm signatureAlgorithm) throws Exception {
        final String kid = getKeyIdByAlgorithm(signatureAlgorithm, Use.SIGNATURE, cryptoProvider);

        System.out.println(String.format("Cross check for %s ...", signatureAlgorithm.getName()));
        final String nimbusJwt = createNimbusJwt(cryptoProvider, kid, signatureAlgorithm);
        validate(nimbusJwt, cryptoProvider, kid, signatureAlgorithm);

        final String oxauthJwt = createOxauthJwt(cryptoProvider, kid, signatureAlgorithm);
        validate(oxauthJwt, cryptoProvider, kid, signatureAlgorithm);
        System.out.println(String.format("Finished cross check for %s.", signatureAlgorithm.getName()));
    }

    private static void validate(String jwtAsString, OxAuthCryptoProvider cryptoProvider, String kid, SignatureAlgorithm signatureAlgorithm) throws Exception {

        SignedJWT signedJWT = SignedJWT.parse(jwtAsString);
        Jwt jwt = Jwt.parse(jwtAsString);
        JWSVerifier nimbusVerifier = null;
        AbstractJwsSigner oxauthVerifier = null;
        switch (signatureAlgorithm.getFamily()) {
            case EC:
                final ECKey ecKey = ECKey.load(cryptoProvider.getKeyStore(), kid, cryptoProvider.getKeyStoreSecret().toCharArray());
                final ECPublicKey ecPublicKey = ecKey.toECPublicKey();
                nimbusVerifier = new ECDSAVerifier(ecKey);
                oxauthVerifier = new ECDSASigner(jwt.getHeader().getSignatureAlgorithm(), new ECDSAPublicKey(jwt.getHeader().getSignatureAlgorithm(), ecPublicKey.getW().getAffineX(), ecPublicKey.getW().getAffineY()));
                break;
            case RSA:
                RSAKey rsaKey = RSAKey.load(cryptoProvider.getKeyStore(), kid, cryptoProvider.getKeyStoreSecret().toCharArray());
                final java.security.interfaces.RSAPublicKey rsaPublicKey = rsaKey.toRSAPublicKey();
                nimbusVerifier = new RSASSAVerifier(rsaKey);
                oxauthVerifier = new RSASigner(signatureAlgorithm, new RSAPublicKey(rsaPublicKey.getModulus(), rsaPublicKey.getPublicExponent()));
                break;
        }

        assertNotNull(nimbusVerifier);
        assertNotNull(oxauthVerifier);

        // Nimbus
        assertTrue(signedJWT.verify(nimbusVerifier));

        // oxauth cryptoProvider
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), kid,
                null, null, jwt.getHeader().getSignatureAlgorithm());
        assertTrue(validJwt);

        // oxauth verifier
        assertTrue(oxauthVerifier.validate(jwt));
    }

    private static String createNimbusJwt(OxAuthCryptoProvider cryptoProvider, String kid, SignatureAlgorithm signatureAlgorithm) throws Exception {
        final AlgorithmFamily family = signatureAlgorithm.getFamily();

        JWSSigner signer = null;
        switch (family) {
            case RSA:
                signer = new RSASSASigner(RSAKey.load(cryptoProvider.getKeyStore(), kid, cryptoProvider.getKeyStoreSecret().toCharArray()));
                break;
            case EC:
                signer = new com.nimbusds.jose.crypto.ECDSASigner(ECKey.load(cryptoProvider.getKeyStore(), kid, cryptoProvider.getKeyStoreSecret().toCharArray()));
                break;
        }

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5")
                .issuer("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5")
                .expirationTime(new Date(1575559276888000L))
                .issueTime(new Date(1575559276888000L))
                .audience("https://gomer-vbox/oxauth/restv1/token")
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(signatureAlgorithm.getJwsAlgorithm()).keyID(kid).build(),
                claimsSet);

        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    private static String createOxauthJwt(OxAuthCryptoProvider cryptoProvider, String kid, SignatureAlgorithm algorithm) throws Exception {
        Jwt jwt = new Jwt();

        jwt.getHeader().setKeyId(kid);
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(algorithm);

        jwt.getClaims().setSubjectIdentifier("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5");
        jwt.getClaims().setIssuer("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5");
        jwt.getClaims().setExpirationTime(new Date(1575559276888000L));
        jwt.getClaims().setIssuedAt(new Date(1575559276888000L));
        jwt.getClaims().setAudience("https://gomer-vbox/oxauth/restv1/token");

        String signature = cryptoProvider.sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), null, algorithm);
        jwt.setEncodedSignature(signature);
        return jwt.toString();
    }

    private static String getKeyIdByAlgorithm(SignatureAlgorithm algorithm, Use use, OxAuthCryptoProvider cryptoProvider) throws KeyStoreException {
        final List<String> aliases = cryptoProvider.getKeys();
        for (String keyId : aliases) {
            if (keyId.endsWith(use.getParamName()  + "_" + algorithm.getName().toLowerCase())) {
                return keyId;
            }
        }
        return null;
    }
}
