/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.server.comp;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.bouncycastle.math.ec.rfc8032.Ed448;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import org.bouncycastle.math.ec.custom.sec.SecP256R1Curve;
import org.bouncycastle.math.ec.custom.sec.SecP384R1Curve;
import org.bouncycastle.math.ec.custom.sec.SecP521R1Curve;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.Certificate;
import io.jans.as.model.crypto.Key;
import io.jans.as.model.crypto.KeyFactory;
import io.jans.as.model.crypto.signature.ECDSAKeyFactory;
import io.jans.as.model.crypto.signature.ECDSAPrivateKey;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.EDDSAKeyFactory;
import io.jans.as.model.crypto.signature.EDDSAPrivateKey;
import io.jans.as.model.crypto.signature.EDDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAKeyFactory;
import io.jans.as.model.crypto.signature.RSAPrivateKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jws.ECDSASigner;
import io.jans.as.model.jws.EDDSASigner;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.util.Base64Util;
import io.jans.as.server.BaseTest;

/**
 * SignatureTest Unit Tests.
 * 
 * @author Javier Rojas Blum Date: 12.03.2012
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
@SuppressWarnings("deprecation")
public class SignatureTest extends BaseTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static String DEF_CERTIFICATE_OWN = "CN=Test CA Certificate";
    private static String DEF_INPUT = "Hello World!";

    /**
     * Contains Private Key, Public Key, Certificate.
     * The function loadTestKeys returns instance of TestKeys class.
     * TestKeys class is used by unitTests (readXKeys).
     * 
     * @author Sergey Manoylo
     * @version August 20, 2021
     */
    private static class TestKeys {
        public java.security.Key privateKey;
        public java.security.PublicKey publicKey;
        public java.security.cert.Certificate certificate;
    };

    /**
     * Unit Test:
     * 
     * Generating RS256 Keys.
     * 
     * signatureAlgorithm == SignatureAlgorithm.RS256.
     * 
     * 1. generation asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @throws Exception
     */
    @Test
    public void generateRS256Keys() throws Exception {
        showTitle("generateRS256Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;

        KeyFactory<RSAPrivateKey, RSAPublicKey> keyFactory = new RSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<RSAPrivateKey, RSAPublicKey> key = keyFactory.getKey();

        RSAPrivateKey privateKey = key.getPrivateKey();
        RSAPublicKey publicKey = key.getPublicKey();
        Certificate certificate = key.getCertificate();

        System.out.println("PRIVATE KEY");
        System.out.println(privateKey);
        System.out.println("PUBLIC KEY");
        System.out.println(publicKey);
        System.out.println("CERTIFICATE");
        System.out.println(certificate);

        String signingInput = DEF_INPUT;
        RSASigner rsaSigner1 = new RSASigner(signatureAlgorithm, privateKey);
        String signature = rsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        RSASigner rsaSigner2 = new RSASigner(signatureAlgorithm, publicKey);
        assertTrue(rsaSigner2.validateSignature(signingInput, signature));
        RSASigner rsaSigner3 = new RSASigner(signatureAlgorithm, certificate);
        assertTrue(rsaSigner3.validateSignature(signingInput, signature));

        keyFactory = new RSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<RSAPrivateKey, RSAPublicKey> keyWrong = keyFactory.getKey();
        RSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        assertTrue(rsaSigner2.validateSignature(signingInput, signature));
        assertFalse(rsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        RSASigner rsaSigner4 = new RSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(rsaSigner4.validateSignature(signingInput, signature));
        assertFalse(rsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        RSASigner rsaSigner5 = new RSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(rsaSigner5.validateSignature(signingInput, signature));
        assertFalse(rsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Reading RS256 Keys from the KeyStorage.
     * 
     * signatureAlgorithm == SignatureAlgorithm.RS256. 
     * 
     * 1. loads  asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @param dnName Issuer of the generated Certificate.
     * @param keyStoreFile Key Store (file).
     * @param keyStoreSecret Password for access to the Key Store (file).
     * @param kid keyID (Alias name).
     * @throws Exception
     */
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret", "RS256_keyId" })
    @Test
    public void readRS256Keys(final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String kid) throws Exception {

        showTitle("readRS256Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;

        TestKeys testKeys = loadTestKeys(signatureAlgorithm, keyStoreFile, keyStoreSecret, dnName, kid);

        java.security.interfaces.RSAPrivateKey privateKey = (java.security.interfaces.RSAPrivateKey) testKeys.privateKey;
        java.security.interfaces.RSAPublicKey publicKey = (java.security.interfaces.RSAPublicKey) testKeys.publicKey;
        java.security.cert.Certificate certificate = testKeys.certificate;

        RSAPrivateKey privKey = new RSAPrivateKey(signatureAlgorithm, privateKey.getModulus(), privateKey.getPrivateExponent());
        RSAPublicKey pubKey = new RSAPublicKey(publicKey.getModulus(), publicKey.getPublicExponent());
        Certificate cert = new Certificate(signatureAlgorithm, (X509Certificate) certificate);

        System.out.println("PRIVATE KEY");
        System.out.println(privKey);
        System.out.println("PUBLIC KEY");
        System.out.println(pubKey);
        System.out.println("CERTIFICATE");
        System.out.println(cert);

        String signingInput = DEF_INPUT;
        RSASigner rsaSigner1 = new RSASigner(signatureAlgorithm, privKey);
        String signature = rsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        RSASigner rsaSigner2 = new RSASigner(signatureAlgorithm, pubKey);
        assertTrue(rsaSigner2.validateSignature(signingInput, signature));
        RSASigner rsaSigner3 = new RSASigner(signatureAlgorithm, cert);
        assertTrue(rsaSigner3.validateSignature(signingInput, signature));

        KeyFactory<RSAPrivateKey, RSAPublicKey> keyFactory = new RSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<RSAPrivateKey, RSAPublicKey> keyWrong = keyFactory.getKey();
        RSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        assertTrue(rsaSigner2.validateSignature(signingInput, signature));

        assertFalse(rsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        RSASigner rsaSigner4 = new RSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(rsaSigner4.validateSignature(signingInput, signature));

        assertFalse(rsaSigner4.validateSignature(signingInput, signature));
        assertFalse(rsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        RSASigner rsaSigner5 = new RSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(rsaSigner5.validateSignature(signingInput, signature));
        assertFalse(rsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }
    
    /**
     * Unit Test:
     * 
     * Generating RS384 Keys.
     * 
     * signatureAlgorithm == SignatureAlgorithm.RS384.
     * 
     * 1. generation asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @throws Exception
     */
    @Test
    public void generateRS384Keys() throws Exception {
        showTitle("generateRS384Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS384;

        KeyFactory<RSAPrivateKey, RSAPublicKey> keyFactory = new RSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<RSAPrivateKey, RSAPublicKey> key = keyFactory.getKey();

        RSAPrivateKey privateKey = key.getPrivateKey();
        RSAPublicKey publicKey = key.getPublicKey();
        Certificate certificate = key.getCertificate();

        System.out.println("PRIVATE KEY");
        System.out.println(privateKey);
        System.out.println("PUBLIC KEY");
        System.out.println(publicKey);
        System.out.println("CERTIFICATE");
        System.out.println(certificate);

        String signingInput = DEF_INPUT;
        RSASigner rsaSigner1 = new RSASigner(signatureAlgorithm, privateKey);
        String signature = rsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        RSASigner rsaSigner2 = new RSASigner(signatureAlgorithm, publicKey);
        assertTrue(rsaSigner2.validateSignature(signingInput, signature));
        RSASigner rsaSigner3 = new RSASigner(signatureAlgorithm, certificate);
        assertTrue(rsaSigner3.validateSignature(signingInput, signature));

        keyFactory = new RSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<RSAPrivateKey, RSAPublicKey> keyWrong = keyFactory.getKey();
        RSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        assertTrue(rsaSigner2.validateSignature(signingInput, signature));

        assertFalse(rsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        RSASigner rsaSigner4 = new RSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(rsaSigner4.validateSignature(signingInput, signature));

        assertFalse(rsaSigner4.validateSignature(signingInput, signature));
        assertFalse(rsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        RSASigner rsaSigner5 = new RSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(rsaSigner5.validateSignature(signingInput, signature));
        assertFalse(rsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Reading RS384 Keys from the KeyStorage.
     * 
     * signatureAlgorithm == SignatureAlgorithm.RS384. 
     * 
     * 1. loads  asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @param dnName Issuer of the generated Certificate.
     * @param keyStoreFile Key Store (file).
     * @param keyStoreSecret Password for access to the Key Store (file).
     * @param kid keyID (Alias name).
     * @throws Exception
     */    
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret", "RS384_keyId" })
    @Test
    public void readRS384Keys(final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String kid) throws Exception {
        showTitle("readRS384Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS384;

        TestKeys testKeys = loadTestKeys(signatureAlgorithm, keyStoreFile, keyStoreSecret, dnName, kid);

        java.security.interfaces.RSAPrivateKey privateKey = (java.security.interfaces.RSAPrivateKey) testKeys.privateKey;
        java.security.interfaces.RSAPublicKey publicKey = (java.security.interfaces.RSAPublicKey) testKeys.publicKey;
        java.security.cert.Certificate certificate = testKeys.certificate;

        RSAPrivateKey privKey = new RSAPrivateKey(signatureAlgorithm, privateKey.getModulus(), privateKey.getPrivateExponent());
        RSAPublicKey pubKey = new RSAPublicKey(publicKey.getModulus(), publicKey.getPublicExponent());
        Certificate cert = new Certificate(signatureAlgorithm, (X509Certificate) certificate);

        System.out.println("PRIVATE KEY");
        System.out.println(privKey);
        System.out.println("PUBLIC KEY");
        System.out.println(pubKey);
        System.out.println("CERTIFICATE");
        System.out.println(cert);

        String signingInput = DEF_INPUT;
        RSASigner rsaSigner1 = new RSASigner(signatureAlgorithm, privKey);
        String signature = rsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        RSASigner rsaSigner2 = new RSASigner(signatureAlgorithm, pubKey);
        assertTrue(rsaSigner2.validateSignature(signingInput, signature));
        RSASigner rsaSigner3 = new RSASigner(signatureAlgorithm, cert);
        assertTrue(rsaSigner3.validateSignature(signingInput, signature));

        KeyFactory<RSAPrivateKey, RSAPublicKey> keyFactory = new RSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<RSAPrivateKey, RSAPublicKey> keyWrong = keyFactory.getKey();
        RSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        assertTrue(rsaSigner2.validateSignature(signingInput, signature));

        assertFalse(rsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        RSASigner rsaSigner4 = new RSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(rsaSigner4.validateSignature(signingInput, signature));

        assertFalse(rsaSigner4.validateSignature(signingInput, signature));
        assertFalse(rsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        RSASigner rsaSigner5 = new RSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(rsaSigner5.validateSignature(signingInput, signature));
        assertFalse(rsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Generating RS512 Keys.
     * 
     * signatureAlgorithm == SignatureAlgorithm.RS512.
     * 
     * 1. generation asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @throws Exception
     */
    @Test
    public void generateRS512Keys() throws Exception {
        showTitle("generateRS512Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS512;

        KeyFactory<RSAPrivateKey, RSAPublicKey> keyFactory = new RSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<RSAPrivateKey, RSAPublicKey> key = keyFactory.getKey();

        RSAPrivateKey privateKey = key.getPrivateKey();
        RSAPublicKey publicKey = key.getPublicKey();
        Certificate certificate = key.getCertificate();

        System.out.println("PRIVATE KEY");
        System.out.println(privateKey);
        System.out.println("PUBLIC KEY");
        System.out.println(publicKey);
        System.out.println("CERTIFICATE");
        System.out.println(certificate);

        String signingInput = DEF_INPUT;
        RSASigner rsaSigner1 = new RSASigner(signatureAlgorithm, privateKey);
        String signature = rsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        RSASigner rsaSigner2 = new RSASigner(signatureAlgorithm, publicKey);
        assertTrue(rsaSigner2.validateSignature(signingInput, signature));
        RSASigner rsaSigner3 = new RSASigner(signatureAlgorithm, certificate);
        assertTrue(rsaSigner3.validateSignature(signingInput, signature));

        keyFactory = new RSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<RSAPrivateKey, RSAPublicKey> keyWrong = keyFactory.getKey();
        RSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        assertTrue(rsaSigner2.validateSignature(signingInput, signature));

        assertFalse(rsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        RSASigner rsaSigner4 = new RSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(rsaSigner4.validateSignature(signingInput, signature));

        assertFalse(rsaSigner4.validateSignature(signingInput, signature));
        assertFalse(rsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        RSASigner rsaSigner5 = new RSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(rsaSigner5.validateSignature(signingInput, signature));
        assertFalse(rsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Reading RS512 Keys from the KeyStorage.
     * 
     * signatureAlgorithm == SignatureAlgorithm.RS512. 
     * 
     * 1. loads  asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @param dnName Issuer of the generated Certificate.
     * @param keyStoreFile Key Store (file).
     * @param keyStoreSecret Password for access to the Key Store (file).
     * @param kid keyID (Alias name).
     * @throws Exception
     */
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret", "RS512_keyId" })
    @Test
    public void readRS512Keys(final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String kid) throws Exception {
        showTitle("readRS512Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS512;

        TestKeys testKeys = loadTestKeys(signatureAlgorithm, keyStoreFile, keyStoreSecret, dnName, kid);

        java.security.interfaces.RSAPrivateKey privateKey = (java.security.interfaces.RSAPrivateKey) testKeys.privateKey;
        java.security.interfaces.RSAPublicKey publicKey = (java.security.interfaces.RSAPublicKey) testKeys.publicKey;
        java.security.cert.Certificate certificate = testKeys.certificate;

        RSAPrivateKey privKey = new RSAPrivateKey(signatureAlgorithm, privateKey.getModulus(), privateKey.getPrivateExponent());
        RSAPublicKey pubKey = new RSAPublicKey(publicKey.getModulus(), publicKey.getPublicExponent());
        Certificate cert = new Certificate(signatureAlgorithm, (X509Certificate) certificate);

        System.out.println("PRIVATE KEY");
        System.out.println(privKey);
        System.out.println("PUBLIC KEY");
        System.out.println(pubKey);
        System.out.println("CERTIFICATE");
        System.out.println(cert);

        String signingInput = DEF_INPUT;
        RSASigner rsaSigner1 = new RSASigner(signatureAlgorithm, privKey);
        String signature = rsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        RSASigner rsaSigner2 = new RSASigner(signatureAlgorithm, pubKey);
        assertTrue(rsaSigner2.validateSignature(signingInput, signature));
        RSASigner rsaSigner3 = new RSASigner(signatureAlgorithm, cert);
        assertTrue(rsaSigner3.validateSignature(signingInput, signature));

        KeyFactory<RSAPrivateKey, RSAPublicKey> keyFactory = new RSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<RSAPrivateKey, RSAPublicKey> keyWrong = keyFactory.getKey();
        RSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        assertTrue(rsaSigner2.validateSignature(signingInput, signature));

        assertFalse(rsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        RSASigner rsaSigner4 = new RSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(rsaSigner4.validateSignature(signingInput, signature));

        assertFalse(rsaSigner4.validateSignature(signingInput, signature));
        assertFalse(rsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        RSASigner rsaSigner5 = new RSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(rsaSigner5.validateSignature(signingInput, signature));
        assertFalse(rsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(rsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(rsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Generating ES256 Keys.
     * 
     * signatureAlgorithm == SignatureAlgorithm.ES256.
     * 
     * 1. generation asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @throws Exception
     */
    @Test
    public void generateES256Keys() throws Exception {
        showTitle("generateES256Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.ES256;

        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<ECDSAPrivateKey, ECDSAPublicKey> key = keyFactory.getKey();

        ECDSAPrivateKey privateKey = key.getPrivateKey();
        ECDSAPublicKey publicKey = key.getPublicKey();
        Certificate certificate = key.getCertificate();

        System.out.println("PRIVATE KEY");
        System.out.println(privateKey);
        System.out.println("PUBLIC KEY");
        System.out.println(publicKey);
        System.out.println("CERTIFICATE");
        System.out.println(certificate);

        String signingInput = DEF_INPUT;
        ECDSASigner ecdsaSigner1 = new ECDSASigner(signatureAlgorithm, privateKey);
        String signature = ecdsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        ECDSASigner ecdsaSigner2 = new ECDSASigner(signatureAlgorithm, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));
        ECDSASigner ecdsaSigner3 = new ECDSASigner(signatureAlgorithm, certificate);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));

        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(signatureAlgorithm.getCurve().getAlias());
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privateKey.getD(), ecSpec);

        ECPoint pointQ = ecSpec.getCurve().createPoint(publicKey.getX(), publicKey.getY());
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

        java.security.KeyFactory keyFactoryNative = java.security.KeyFactory.getInstance("ECDSA", "BC");
        BCECPrivateKey privateKeyNative = (BCECPrivateKey) keyFactoryNative.generatePrivate(privateKeySpec);
        BCECPublicKey publicKeyNative = (BCECPublicKey) keyFactoryNative.generatePublic(publicKeySpec);

        ECNamedCurveParameterSpec ecSpecPrivateKey = (ECNamedCurveParameterSpec) privateKeyNative.getParameters();
        ECNamedCurveParameterSpec ecSpecPrublicKey = (ECNamedCurveParameterSpec) publicKeyNative.getParameters();

        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrivateKey.getName()));
        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrublicKey.getName()));

        assertTrue(ecSpecPrivateKey.getCurve().getClass() == SecP256R1Curve.class);
        assertTrue(ecSpecPrublicKey.getCurve().getClass() == SecP256R1Curve.class);

        assertTrue(ecSpecPrivateKey.getCurve().getFieldSize() == new SecP256R1Curve().getFieldSize());
        assertTrue(ecSpecPrublicKey.getCurve().getFieldSize() == new SecP256R1Curve().getFieldSize());

        keyFactory = new ECDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<ECDSAPrivateKey, ECDSAPublicKey> keyWrong = keyFactory.getKey();

        ECDSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner4 = new ECDSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner5 = new ECDSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Reading ES256 Keys from the KeyStorage.
     * 
     * signatureAlgorithm == SignatureAlgorithm.ES256. 
     * 
     * 1. loads  asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @param dnName Issuer of the generated Certificate.
     * @param keyStoreFile Key Store (file).
     * @param keyStoreSecret Password for access to the Key Store (file).
     * @param kid keyID (Alias name).
     * @throws Exception
     */
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret", "ES256_keyId" })
    @Test
    public void readES256Keys(final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String kid) throws Exception {
        showTitle("generateES256Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.ES256;

        TestKeys testKeys = loadTestKeys(signatureAlgorithm, keyStoreFile, keyStoreSecret, dnName, kid);

        java.security.interfaces.ECPrivateKey privateKey = (java.security.interfaces.ECPrivateKey) testKeys.privateKey;
        java.security.interfaces.ECPublicKey publicKey = (java.security.interfaces.ECPublicKey) testKeys.publicKey;
        java.security.cert.Certificate certificate = testKeys.certificate;

        ECDSAPrivateKey privKey = new ECDSAPrivateKey(signatureAlgorithm, privateKey.getS());
        ECDSAPublicKey pubKey = new ECDSAPublicKey(signatureAlgorithm, publicKey.getW().getAffineX(), publicKey.getW().getAffineY());
        Certificate cert = new Certificate(signatureAlgorithm, (X509Certificate) certificate);

        System.out.println("PRIVATE KEY");
        System.out.println(privKey);
        System.out.println("PUBLIC KEY");
        System.out.println(pubKey);
        System.out.println("CERTIFICATE");
        System.out.println(cert);

        String signingInput = DEF_INPUT;
        ECDSASigner ecdsaSigner1 = new ECDSASigner(signatureAlgorithm, privKey);
        String signature = ecdsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        ECDSASigner ecdsaSigner2 = new ECDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));
        ECDSASigner ecdsaSigner3 = new ECDSASigner(signatureAlgorithm, cert);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));

        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(signatureAlgorithm.getCurve().getAlias());
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privKey.getD(), ecSpec);

        ECPoint pointQ = ecSpec.getCurve().createPoint(pubKey.getX(), pubKey.getY());
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

        java.security.KeyFactory keyFactoryNative = java.security.KeyFactory.getInstance("ECDSA", "BC");
        BCECPrivateKey privateKeyNative = (BCECPrivateKey) keyFactoryNative.generatePrivate(privateKeySpec);
        BCECPublicKey publicKeyNative = (BCECPublicKey) keyFactoryNative.generatePublic(publicKeySpec);

        ECNamedCurveParameterSpec ecSpecPrivateKey = (ECNamedCurveParameterSpec) privateKeyNative.getParameters();
        ECNamedCurveParameterSpec ecSpecPrublicKey = (ECNamedCurveParameterSpec) publicKeyNative.getParameters();

        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrivateKey.getName()));
        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrublicKey.getName()));

        assertTrue(ecSpecPrivateKey.getCurve().getClass() == SecP256R1Curve.class);
        assertTrue(ecSpecPrublicKey.getCurve().getClass() == SecP256R1Curve.class);

        assertTrue(ecSpecPrivateKey.getCurve().getFieldSize() == new SecP256R1Curve().getFieldSize());
        assertTrue(ecSpecPrublicKey.getCurve().getFieldSize() == new SecP256R1Curve().getFieldSize());

        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(signatureAlgorithm,
                DEF_CERTIFICATE_OWN);

        Key<ECDSAPrivateKey, ECDSAPublicKey> keyWrong = keyFactory.getKey();

        ECDSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        ecdsaSigner2 = new ECDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner4 = new ECDSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner5 = new ECDSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Generating ES256K Keys.
     * 
     * signatureAlgorithm == SignatureAlgorithm.ES256K.
     * 
     * 1. generation asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @throws Exception
     */    
    @Test
    public void generateES256KKeys() throws Exception {
        showTitle("generateES256KKeys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.ES256K;

        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<ECDSAPrivateKey, ECDSAPublicKey> key = keyFactory.getKey();

        ECDSAPrivateKey privateKey = key.getPrivateKey();
        ECDSAPublicKey publicKey = key.getPublicKey();
        Certificate certificate = key.getCertificate();

        System.out.println("PRIVATE KEY");
        System.out.println(privateKey);
        System.out.println("PUBLIC KEY");
        System.out.println(publicKey);
        System.out.println("CERTIFICATE");
        System.out.println(certificate);

        String signingInput = DEF_INPUT;
        ECDSASigner ecdsaSigner1 = new ECDSASigner(signatureAlgorithm, privateKey);
        String signature = ecdsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        ECDSASigner ecdsaSigner2 = new ECDSASigner(signatureAlgorithm, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));
        ECDSASigner ecdsaSigner3 = new ECDSASigner(signatureAlgorithm, certificate);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));

        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(signatureAlgorithm.getCurve().getAlias());
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privateKey.getD(), ecSpec);

        ECPoint pointQ = ecSpec.getCurve().createPoint(publicKey.getX(), publicKey.getY());
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

        java.security.KeyFactory keyFactoryNative = java.security.KeyFactory.getInstance("ECDSA", "BC");
        BCECPrivateKey privateKeyNative = (BCECPrivateKey) keyFactoryNative.generatePrivate(privateKeySpec);
        BCECPublicKey publicKeyNative = (BCECPublicKey) keyFactoryNative.generatePublic(publicKeySpec);

        ECNamedCurveParameterSpec ecSpecPrivateKey = (ECNamedCurveParameterSpec) privateKeyNative.getParameters();
        ECNamedCurveParameterSpec ecSpecPrublicKey = (ECNamedCurveParameterSpec) publicKeyNative.getParameters();

        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrivateKey.getName()));
        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrublicKey.getName()));

        assertTrue(ecSpecPrivateKey.getCurve().getClass() == SecP256K1Curve.class);
        assertTrue(ecSpecPrublicKey.getCurve().getClass() == SecP256K1Curve.class);

        assertTrue(ecSpecPrivateKey.getCurve().getFieldSize() == new SecP256K1Curve().getFieldSize());
        assertTrue(ecSpecPrublicKey.getCurve().getFieldSize() == new SecP256K1Curve().getFieldSize());

        keyFactory = new ECDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<ECDSAPrivateKey, ECDSAPublicKey> keyWrong = keyFactory.getKey();

        ECDSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        ecdsaSigner2 = new ECDSASigner(signatureAlgorithm, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner4 = new ECDSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner5 = new ECDSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Reading ES256K Keys from the KeyStorage.
     * 
     * signatureAlgorithm == SignatureAlgorithm.ES256K. 
     * 
     * 1. loads  asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @param dnName Issuer of the generated Certificate.
     * @param keyStoreFile Key Store (file).
     * @param keyStoreSecret Password for access to the Key Store (file).
     * @param kid keyID (Alias name).
     * @throws Exception
     */    
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret", "ES256K_keyId" })
    @Test
    public void readES256KKeys(final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final String kid) throws Exception {
        showTitle("readES256KKeys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.ES256K;

        TestKeys testKeys = loadTestKeys(signatureAlgorithm, keyStoreFile, keyStoreSecret, dnName, kid);

        java.security.interfaces.ECPrivateKey privateKey = (java.security.interfaces.ECPrivateKey) testKeys.privateKey;
        java.security.interfaces.ECPublicKey publicKey = (java.security.interfaces.ECPublicKey) testKeys.publicKey;
        java.security.cert.Certificate certificate = testKeys.certificate;

        ECDSAPrivateKey privKey = new ECDSAPrivateKey(signatureAlgorithm, privateKey.getS());
        ECDSAPublicKey pubKey = new ECDSAPublicKey(signatureAlgorithm, publicKey.getW().getAffineX(), publicKey.getW().getAffineY());
        Certificate cert = new Certificate(signatureAlgorithm, (X509Certificate) certificate);

        System.out.println("PRIVATE KEY");
        System.out.println(privKey);
        System.out.println("PUBLIC KEY");
        System.out.println(pubKey);
        System.out.println("CERTIFICATE");
        System.out.println(cert);

        String signingInput = DEF_INPUT;
        ECDSASigner ecdsaSigner1 = new ECDSASigner(signatureAlgorithm, privKey);
        String signature = ecdsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        ECDSASigner ecdsaSigner2 = new ECDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));
        ECDSASigner ecdsaSigner3 = new ECDSASigner(signatureAlgorithm, cert);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));

        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(signatureAlgorithm.getCurve().getAlias());
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privKey.getD(), ecSpec);

        ECPoint pointQ = ecSpec.getCurve().createPoint(pubKey.getX(), pubKey.getY());
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

        java.security.KeyFactory keyFactoryNative = java.security.KeyFactory.getInstance("ECDSA", "BC");
        BCECPrivateKey privateKeyNative = (BCECPrivateKey) keyFactoryNative.generatePrivate(privateKeySpec);
        BCECPublicKey publicKeyNative = (BCECPublicKey) keyFactoryNative.generatePublic(publicKeySpec);

        ECNamedCurveParameterSpec ecSpecPrivateKey = (ECNamedCurveParameterSpec) privateKeyNative.getParameters();
        ECNamedCurveParameterSpec ecSpecPrublicKey = (ECNamedCurveParameterSpec) publicKeyNative.getParameters();

        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrivateKey.getName()));
        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrublicKey.getName()));

        assertTrue(ecSpecPrivateKey.getCurve().getClass() == SecP256K1Curve.class);
        assertTrue(ecSpecPrublicKey.getCurve().getClass() == SecP256K1Curve.class);

        assertTrue(ecSpecPrivateKey.getCurve().getFieldSize() == new SecP256K1Curve().getFieldSize());
        assertTrue(ecSpecPrublicKey.getCurve().getFieldSize() == new SecP256K1Curve().getFieldSize());

        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<ECDSAPrivateKey, ECDSAPublicKey> keyWrong = keyFactory.getKey();

        ECDSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        ecdsaSigner2 = new ECDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner4 = new ECDSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner5 = new ECDSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }


    /**
     * Unit Test:
     * 
     * Generating ES384 Keys.
     * 
     * signatureAlgorithm == SignatureAlgorithm.ES384.
     * 
     * 1. generation asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @throws Exception
     */
    @Test
    public void generateES384Keys() throws Exception {
        showTitle("generateES384Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.ES384;

        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<ECDSAPrivateKey, ECDSAPublicKey> key = keyFactory.getKey();

        ECDSAPrivateKey privateKey = key.getPrivateKey();
        ECDSAPublicKey publicKey = key.getPublicKey();
        Certificate certificate = key.getCertificate();

        System.out.println("PRIVATE KEY");
        System.out.println(privateKey);
        System.out.println("PUBLIC KEY");
        System.out.println(publicKey);
        System.out.println("CERTIFICATE");
        System.out.println(certificate);

        String signingInput = DEF_INPUT;
        ECDSASigner ecdsaSigner1 = new ECDSASigner(signatureAlgorithm, privateKey);
        String signature = ecdsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        ECDSASigner ecdsaSigner2 = new ECDSASigner(signatureAlgorithm, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));
        ECDSASigner ecdsaSigner3 = new ECDSASigner(signatureAlgorithm, certificate);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));

        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(signatureAlgorithm.getCurve().getAlias());
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privateKey.getD(), ecSpec);

        ECPoint pointQ = ecSpec.getCurve().createPoint(publicKey.getX(), publicKey.getY());
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

        java.security.KeyFactory keyFactoryNative = java.security.KeyFactory.getInstance("ECDSA", "BC");
        BCECPrivateKey privateKeyNative = (BCECPrivateKey) keyFactoryNative.generatePrivate(privateKeySpec);
        BCECPublicKey publicKeyNative = (BCECPublicKey) keyFactoryNative.generatePublic(publicKeySpec);

        ECNamedCurveParameterSpec ecSpecPrivateKey = (ECNamedCurveParameterSpec) privateKeyNative.getParameters();
        ECNamedCurveParameterSpec ecSpecPrublicKey = (ECNamedCurveParameterSpec) publicKeyNative.getParameters();

        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrivateKey.getName()));
        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrublicKey.getName()));

        assertTrue(ecSpecPrivateKey.getCurve().getClass() == SecP384R1Curve.class);
        assertTrue(ecSpecPrublicKey.getCurve().getClass() == SecP384R1Curve.class);

        assertTrue(ecSpecPrivateKey.getCurve().getFieldSize() == new SecP384R1Curve().getFieldSize());
        assertTrue(ecSpecPrublicKey.getCurve().getFieldSize() == new SecP384R1Curve().getFieldSize());

        keyFactory = new ECDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<ECDSAPrivateKey, ECDSAPublicKey> keyWrong = keyFactory.getKey();

        ECDSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        ecdsaSigner2 = new ECDSASigner(signatureAlgorithm, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner4 = new ECDSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner5 = new ECDSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Reading ES384 Keys from the KeyStorage.
     * 
     * signatureAlgorithm == SignatureAlgorithm.ES384. 
     * 
     * 1. loads  asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @param dnName Issuer of the generated Certificate.
     * @param keyStoreFile Key Store (file).
     * @param keyStoreSecret Password for access to the Key Store (file).
     * @param kid keyID (Alias name).
     * @throws Exception
     */    
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret", "ES384_keyId" })
    @Test
    public void readES384Keys(final String dnName, final String keyStoreFile, final String keyStoreSecret, final String kid) throws Exception {
        showTitle("readES384Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.ES384;

        TestKeys testKeys = loadTestKeys(signatureAlgorithm, keyStoreFile, keyStoreSecret, dnName, kid);

        java.security.interfaces.ECPrivateKey privateKey = (java.security.interfaces.ECPrivateKey) testKeys.privateKey;
        java.security.interfaces.ECPublicKey publicKey = (java.security.interfaces.ECPublicKey) testKeys.publicKey;
        java.security.cert.Certificate certificate = testKeys.certificate;

        ECDSAPrivateKey privKey = new ECDSAPrivateKey(signatureAlgorithm, privateKey.getS());
        ECDSAPublicKey pubKey = new ECDSAPublicKey(signatureAlgorithm, publicKey.getW().getAffineX(), publicKey.getW().getAffineY());
        Certificate cert = new Certificate(signatureAlgorithm, (X509Certificate) certificate);

        System.out.println("PRIVATE KEY");
        System.out.println(privKey);
        System.out.println("PUBLIC KEY");
        System.out.println(pubKey);
        System.out.println("CERTIFICATE");
        System.out.println(cert);

        String signingInput = DEF_INPUT;
        ECDSASigner ecdsaSigner1 = new ECDSASigner(signatureAlgorithm, privKey);
        String signature = ecdsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        ECDSASigner ecdsaSigner2 = new ECDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));
        ECDSASigner ecdsaSigner3 = new ECDSASigner(signatureAlgorithm, cert);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));

        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(signatureAlgorithm.getCurve().getAlias());
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privKey.getD(), ecSpec);

        ECPoint pointQ = ecSpec.getCurve().createPoint(pubKey.getX(), pubKey.getY());
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

        java.security.KeyFactory keyFactoryNative = java.security.KeyFactory.getInstance("ECDSA", "BC");
        BCECPrivateKey privateKeyNative = (BCECPrivateKey) keyFactoryNative.generatePrivate(privateKeySpec);
        BCECPublicKey publicKeyNative = (BCECPublicKey) keyFactoryNative.generatePublic(publicKeySpec);

        ECNamedCurveParameterSpec ecSpecPrivateKey = (ECNamedCurveParameterSpec) privateKeyNative.getParameters();
        ECNamedCurveParameterSpec ecSpecPrublicKey = (ECNamedCurveParameterSpec) publicKeyNative.getParameters();

        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrivateKey.getName()));
        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrublicKey.getName()));

        assertTrue(ecSpecPrivateKey.getCurve().getClass() == SecP384R1Curve.class);
        assertTrue(ecSpecPrublicKey.getCurve().getClass() == SecP384R1Curve.class);

        assertTrue(ecSpecPrivateKey.getCurve().getFieldSize() == new SecP384R1Curve().getFieldSize());
        assertTrue(ecSpecPrublicKey.getCurve().getFieldSize() == new SecP384R1Curve().getFieldSize());

        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<ECDSAPrivateKey, ECDSAPublicKey> keyWrong = keyFactory.getKey();

        ECDSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        ecdsaSigner2 = new ECDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner4 = new ECDSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner5 = new ECDSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Generating ES512 Keys.
     * 
     * signatureAlgorithm == SignatureAlgorithm.ES512.
     * 
     * 1. generation asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @throws Exception
     */    
    @Test
    public void generateES512Keys() throws Exception {
        showTitle("generateES512Keys");

        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(SignatureAlgorithm.ES512, DEF_CERTIFICATE_OWN);
        ECDSAPrivateKey privateKey = keyFactory.getPrivateKey();
        ECDSAPublicKey publicKey = keyFactory.getPublicKey();
        Certificate certificate = keyFactory.getCertificate();

        System.out.println("PRIVATE KEY");
        System.out.println(privateKey);
        System.out.println("PUBLIC KEY");
        System.out.println(publicKey);
        System.out.println("CERTIFICATE");
        System.out.println(certificate);

        String signingInput = DEF_INPUT;
        ECDSASigner ecdsaSigner1 = new ECDSASigner(SignatureAlgorithm.ES512, privateKey);
        String signature = ecdsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        ECDSASigner ecdsaSigner2 = new ECDSASigner(SignatureAlgorithm.ES512, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));
        ECDSASigner ecdsaSigner3 = new ECDSASigner(SignatureAlgorithm.ES512, certificate);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));

        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable
                .getParameterSpec(SignatureAlgorithm.ES512.getCurve().getAlias());
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privateKey.getD(), ecSpec);

        ECPoint pointQ = ecSpec.getCurve().createPoint(publicKey.getX(), publicKey.getY());
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

        java.security.KeyFactory keyFactoryNative = java.security.KeyFactory.getInstance("ECDSA", "BC");
        BCECPrivateKey privateKeyNative = (BCECPrivateKey) keyFactoryNative.generatePrivate(privateKeySpec);
        BCECPublicKey publicKeyNative = (BCECPublicKey) keyFactoryNative.generatePublic(publicKeySpec);

        ECNamedCurveParameterSpec ecSpecPrivateKey = (ECNamedCurveParameterSpec) privateKeyNative.getParameters();
        ECNamedCurveParameterSpec ecSpecPrublicKey = (ECNamedCurveParameterSpec) publicKeyNative.getParameters();

        assertTrue(SignatureAlgorithm.ES512.getCurve().getAlias().equals(ecSpecPrivateKey.getName()));
        assertTrue(SignatureAlgorithm.ES512.getCurve().getAlias().equals(ecSpecPrublicKey.getName()));

        assertTrue(ecSpecPrivateKey.getCurve().getClass() == SecP521R1Curve.class);
        assertTrue(ecSpecPrublicKey.getCurve().getClass() == SecP521R1Curve.class);

        assertTrue(ecSpecPrivateKey.getCurve().getFieldSize() == new SecP521R1Curve().getFieldSize());
        assertTrue(ecSpecPrublicKey.getCurve().getFieldSize() == new SecP521R1Curve().getFieldSize());

        keyFactory = new ECDSAKeyFactory(SignatureAlgorithm.ES512, DEF_CERTIFICATE_OWN);

        Key<ECDSAPrivateKey, ECDSAPublicKey> keyWrong = keyFactory.getKey();

        ECDSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        ecdsaSigner2 = new ECDSASigner(SignatureAlgorithm.ES512, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner4 = new ECDSASigner(SignatureAlgorithm.ES512, publicKeyWrong);
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner5 = new ECDSASigner(SignatureAlgorithm.ES512, certificateWrong);
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Reading ES512 Keys from the KeyStorage.
     * 
     * signatureAlgorithm == SignatureAlgorithm.ES512. 
     * 
     * 1. loads  asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @param dnName Issuer of the generated Certificate.
     * @param keyStoreFile Key Store (file).
     * @param keyStoreSecret Password for access to the Key Store (file).
     * @param kid keyID (Alias name).
     * @throws Exception
     */
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret", "ES512_keyId" })
    @Test
    public void readES512Keys(final String dnName, final String keyStoreFile, final String keyStoreSecret, final String kid) throws Exception {
        showTitle("readES512Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.ES512;

        TestKeys testKeys = loadTestKeys(signatureAlgorithm, keyStoreFile, keyStoreSecret, dnName, kid);

        java.security.interfaces.ECPrivateKey privateKey = (java.security.interfaces.ECPrivateKey) testKeys.privateKey;
        java.security.interfaces.ECPublicKey publicKey = (java.security.interfaces.ECPublicKey) testKeys.publicKey;
        java.security.cert.Certificate certificate = testKeys.certificate;

        ECDSAPrivateKey privKey = new ECDSAPrivateKey(signatureAlgorithm, privateKey.getS());
        ECDSAPublicKey pubKey = new ECDSAPublicKey(signatureAlgorithm, publicKey.getW().getAffineX(),
                publicKey.getW().getAffineY());
        Certificate cert = new Certificate(signatureAlgorithm, (X509Certificate) certificate);

        System.out.println("PRIVATE KEY");
        System.out.println(privKey);
        System.out.println("PUBLIC KEY");
        System.out.println(pubKey);
        System.out.println("CERTIFICATE");
        System.out.println(cert);

        String signingInput = DEF_INPUT;
        ECDSASigner ecdsaSigner1 = new ECDSASigner(signatureAlgorithm, privKey);
        String signature = ecdsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);
        ECDSASigner ecdsaSigner2 = new ECDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));
        ECDSASigner ecdsaSigner3 = new ECDSASigner(signatureAlgorithm, cert);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));

        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(signatureAlgorithm.getCurve().getAlias());
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privKey.getD(), ecSpec);

        ECPoint pointQ = ecSpec.getCurve().createPoint(pubKey.getX(), pubKey.getY());
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

        java.security.KeyFactory keyFactoryNative = java.security.KeyFactory.getInstance("ECDSA", "BC");
        BCECPrivateKey privateKeyNative = (BCECPrivateKey) keyFactoryNative.generatePrivate(privateKeySpec);
        BCECPublicKey publicKeyNative = (BCECPublicKey) keyFactoryNative.generatePublic(publicKeySpec);

        ECNamedCurveParameterSpec ecSpecPrivateKey = (ECNamedCurveParameterSpec) privateKeyNative.getParameters();
        ECNamedCurveParameterSpec ecSpecPrublicKey = (ECNamedCurveParameterSpec) publicKeyNative.getParameters();

        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrivateKey.getName()));
        assertTrue(signatureAlgorithm.getCurve().getAlias().equals(ecSpecPrublicKey.getName()));

        assertTrue(ecSpecPrivateKey.getCurve().getClass() == SecP521R1Curve.class);
        assertTrue(ecSpecPrublicKey.getCurve().getClass() == SecP521R1Curve.class);

        assertTrue(ecSpecPrivateKey.getCurve().getFieldSize() == new SecP521R1Curve().getFieldSize());
        assertTrue(ecSpecPrublicKey.getCurve().getFieldSize() == new SecP521R1Curve().getFieldSize());

        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);

        Key<ECDSAPrivateKey, ECDSAPublicKey> keyWrong = keyFactory.getKey();

        ECDSAPublicKey publicKeyWrong = keyWrong.getPublicKey();
        Certificate certificateWrong = keyWrong.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        ecdsaSigner2 = new ECDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner4 = new ECDSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        ECDSASigner ecdsaSigner5 = new ECDSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Generating ED25519 Keys.
     * 
     * signatureAlgorithm == SignatureAlgorithm.ED25519.
     * 
     * 1. generation asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @throws Exception
     */
    @Test
    public void generateED25519Keys() throws Exception {
        showTitle("generateED25519Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.ED25519;

        KeyFactory<EDDSAPrivateKey, EDDSAPublicKey> keyFactory = new EDDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);
        EDDSAPrivateKey privateKey = keyFactory.getPrivateKey();
        EDDSAPublicKey publicKey = keyFactory.getPublicKey();
        Certificate certificate = keyFactory.getCertificate();

        System.out.println("PRIVATE KEY");
        System.out.println(privateKey);
        System.out.println("PUBLIC KEY");
        System.out.println(publicKey);
        System.out.println("CERTIFICATE");
        System.out.println(certificate);

        String signingInput = DEF_INPUT;
        EDDSASigner eddsaSigner1 = new EDDSASigner(signatureAlgorithm, privateKey);
        String signature = eddsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);

        EDDSASigner ecdsaSigner2 = new EDDSASigner(signatureAlgorithm, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        ecdsaSigner2 = new EDDSASigner(signatureAlgorithm, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        EDDSASigner ecdsaSigner3 = new EDDSASigner(signatureAlgorithm, certificate);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));

        int privateKeyLen = getDecodedKeysLength(privateKey);
        int publicKeyLen = getDecodedKeysLength(publicKey);

        assertTrue(Ed25519.SECRET_KEY_SIZE == privateKeyLen);
        assertTrue(Ed25519.PUBLIC_KEY_SIZE == publicKeyLen);

        keyFactory = new EDDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);
        EDDSAPublicKey publicKeyWrong = keyFactory.getPublicKey();
        Certificate certificateWrong = keyFactory.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        EDDSASigner ecdsaSigner4 = new EDDSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        EDDSASigner ecdsaSigner5 = new EDDSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Reading ED25519 Keys from the KeyStorage.
     * 
     * signatureAlgorithm == SignatureAlgorithm.ED25519. 
     * 
     * 1. loads  asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @param dnName Issuer of the generated Certificate.
     * @param keyStoreFile Key Store (file).
     * @param keyStoreSecret Password for access to the Key Store (file).
     * @param kid keyID (Alias name).
     * @throws Exception
     */
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret", "Ed25519_keyId" })
    @Test
    public void readED25519Keys(final String dnName, final String keyStoreFile, final String keyStoreSecret, final String kid) throws Exception {
        showTitle("readED25519Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.ED25519;

        TestKeys testKeys = loadTestKeys(signatureAlgorithm, keyStoreFile, keyStoreSecret, dnName, kid);

        BCEdDSAPrivateKey privateKey = (BCEdDSAPrivateKey) testKeys.privateKey;
        BCEdDSAPublicKey publicKey = (BCEdDSAPublicKey) testKeys.publicKey;
        java.security.cert.Certificate certificate = testKeys.certificate;

        EDDSAPrivateKey privKey = new EDDSAPrivateKey(signatureAlgorithm, privateKey.getEncoded(), publicKey.getEncoded());
        EDDSAPublicKey pubKey = new EDDSAPublicKey(signatureAlgorithm, publicKey.getEncoded());
        Certificate cert = new Certificate(signatureAlgorithm, (X509Certificate) certificate);

        System.out.println("PRIVATE KEY");
        System.out.println(privKey);
        System.out.println("PUBLIC KEY");
        System.out.println(pubKey);
        System.out.println("CERTIFICATE");
        System.out.println(cert);

        String signingInput = DEF_INPUT;
        EDDSASigner eddsaSigner1 = new EDDSASigner(signatureAlgorithm, privKey);
        String signature = eddsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);

        EDDSASigner ecdsaSigner2 = new EDDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        ecdsaSigner2 = new EDDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        EDDSASigner ecdsaSigner3 = new EDDSASigner(signatureAlgorithm, cert);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));

        int privKeyLen = getDecodedKeysLength(privKey);
        int pubKeyLen = getDecodedKeysLength(pubKey);

        assertTrue(Ed25519.SECRET_KEY_SIZE == privKeyLen);
        assertTrue(Ed25519.PUBLIC_KEY_SIZE == pubKeyLen);

        KeyFactory<EDDSAPrivateKey, EDDSAPublicKey> keyFactory = new EDDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);
        EDDSAPublicKey publicKeyWrong = keyFactory.getPublicKey();
        Certificate certificateWrong = keyFactory.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        ecdsaSigner2 = new EDDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        EDDSASigner ecdsaSigner4 = new EDDSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        EDDSASigner ecdsaSigner5 = new EDDSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Generating ED448 Keys.
     * 
     * signatureAlgorithm == SignatureAlgorithm.ED448.
     * 
     * 1. generation asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @throws Exception
     */    
    @Test
    public void generateED448Keys() throws Exception {
        showTitle("generateED448Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.ED448;

        KeyFactory<EDDSAPrivateKey, EDDSAPublicKey> keyFactory = new EDDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);
        EDDSAPrivateKey privateKey = keyFactory.getPrivateKey();
        EDDSAPublicKey publicKey = keyFactory.getPublicKey();
        Certificate certificate = keyFactory.getCertificate();

        System.out.println("PRIVATE KEY");
        System.out.println(privateKey);
        System.out.println("PUBLIC KEY");
        System.out.println(publicKey);
        System.out.println("CERTIFICATE");
        System.out.println(certificate);

        String signingInput = DEF_INPUT;
        EDDSASigner eddsaSigner1 = new EDDSASigner(signatureAlgorithm, privateKey);
        String signature = eddsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);

        EDDSASigner ecdsaSigner2 = new EDDSASigner(signatureAlgorithm, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        EDDSASigner ecdsaSigner3 = new EDDSASigner(signatureAlgorithm, certificate);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));

        int privateKeyLen = getDecodedKeysLength(privateKey);
        int publicKeyLen = getDecodedKeysLength(publicKey);

        assertTrue(Ed448.SECRET_KEY_SIZE == privateKeyLen);
        assertTrue(Ed448.PUBLIC_KEY_SIZE == publicKeyLen);

        keyFactory = new EDDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);
        EDDSAPublicKey publicKeyWrong = keyFactory.getPublicKey();
        Certificate certificateWrong = keyFactory.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        ecdsaSigner2 = new EDDSASigner(signatureAlgorithm, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        EDDSASigner ecdsaSigner4 = new EDDSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        EDDSASigner ecdsaSigner5 = new EDDSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Unit Test:
     * 
     * Reading ED448 Keys from the KeyStorage.
     * 
     * signatureAlgorithm == SignatureAlgorithm.ED448. 
     * 
     * 1. loads  asymmetric keypair and certificate (public key in X509Certificate format);     
     * 
     * 2. generation the signature (using private key);
     * 3. verification the signature (using public key);
     * 4. verification the signature (using certificate - public key in X509Certificate format);
     * 
     * 5. true verification the signature (public key);
     * 6. fail verification the signature (wrong sign input);
     * 7. fail verification the signature (wrong signature);
     * 8. fail verification the signature (wrong sign input, wrong signature);       
     * 
     * 9. fail verification the signature (wrong public key);
     * 10. fail verification the signature (wrong public key, wrong sign input);
     * 11. fail verification the signature (wrong public key, wrong signature);
     * 12. fail verification the signature (wrong public key, wrong sign input, wrong signature);
     *
     * 13. fail verification the signature (wrong certificate);
     * 14. fail verification the signature (wrong certificate, wrong sign input);
     * 15. fail verification the signature (wrong certificate, wrong signature);
     * 16. fail verification the signature (wrong certificate, wrong sign input, wrong signature);
     * .
     * 
     * @param dnName Issuer of the generated Certificate.
     * @param keyStoreFile Key Store (file).
     * @param keyStoreSecret Password for access to the Key Store (file).
     * @param kid keyID (Alias name).
     * @throws Exception
     */
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret", "Ed448_keyId" })
    @Test
    public void readED448Keys(final String dnName, final String keyStoreFile, final String keyStoreSecret, final String kid) throws Exception {
        showTitle("readED448Keys");

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.ED448;

        TestKeys testKeys = loadTestKeys(signatureAlgorithm, keyStoreFile, keyStoreSecret, dnName, kid);

        BCEdDSAPrivateKey privateKey = (BCEdDSAPrivateKey) testKeys.privateKey;
        BCEdDSAPublicKey publicKey = (BCEdDSAPublicKey) testKeys.publicKey;
        java.security.cert.Certificate certificate = testKeys.certificate;

        EDDSAPrivateKey privKey = new EDDSAPrivateKey(signatureAlgorithm, privateKey.getEncoded(), publicKey.getEncoded());
        EDDSAPublicKey pubKey = new EDDSAPublicKey(signatureAlgorithm, publicKey.getEncoded());
        Certificate cert = new Certificate(signatureAlgorithm, (X509Certificate) certificate);

        System.out.println("PRIVATE KEY");
        System.out.println(privKey);
        System.out.println("PUBLIC KEY");
        System.out.println(pubKey);
        System.out.println("CERTIFICATE");
        System.out.println(cert);

        String signingInput = DEF_INPUT;
        EDDSASigner eddsaSigner1 = new EDDSASigner(signatureAlgorithm, privKey);
        String signature = eddsaSigner1.generateSignature(signingInput);
        assertTrue(signature.length() > 0);

        EDDSASigner ecdsaSigner2 = new EDDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        ecdsaSigner2 = new EDDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        EDDSASigner ecdsaSigner3 = new EDDSASigner(signatureAlgorithm, cert);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));

        int privKeyLen = getDecodedKeysLength(privKey);
        int pubKeyLen = getDecodedKeysLength(pubKey);

        assertTrue(Ed448.SECRET_KEY_SIZE == privKeyLen);
        assertTrue(Ed448.PUBLIC_KEY_SIZE == pubKeyLen);

        KeyFactory<EDDSAPrivateKey, EDDSAPublicKey> keyFactory = new EDDSAKeyFactory(signatureAlgorithm, DEF_CERTIFICATE_OWN);
        EDDSAPublicKey publicKeyWrong = keyFactory.getPublicKey();
        Certificate certificateWrong = keyFactory.getCertificate();

        byte[] signatureArray = Base64Util.base64urldecode(signature);
        signatureArray[signatureArray.length - 1] = (byte) (~signatureArray[signatureArray.length - 1]);
        String signatureWrong = Base64Util.base64urlencode(signatureArray);

        String signingInputWrong = signingInput + 'z';

        ecdsaSigner2 = new EDDSASigner(signatureAlgorithm, pubKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));

        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner2.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner2.validateSignature(signingInputWrong, signatureWrong));

        EDDSASigner ecdsaSigner4 = new EDDSASigner(signatureAlgorithm, publicKeyWrong);
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner4.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner4.validateSignature(signingInputWrong, signatureWrong));

        EDDSASigner ecdsaSigner5 = new EDDSASigner(signatureAlgorithm, certificateWrong);
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signature));
        assertFalse(ecdsaSigner5.validateSignature(signingInput, signatureWrong));
        assertFalse(ecdsaSigner5.validateSignature(signingInputWrong, signatureWrong));
    }

    /**
     * Returns Length of Decoded EdDSA Private Key.
     * 
     * @param eddsaPrivateKey EdDSA Private Key.
     * @return Length of Decoded EdDSA Private Key.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private int getDecodedKeysLength(EDDSAPrivateKey eddsaPrivateKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        int resLength = 0;
        PKCS8EncodedKeySpec privateKeySpec = eddsaPrivateKey.getPrivateKeySpec();
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance(eddsaPrivateKey.getSignatureAlgorithm().getName());
        BCEdDSAPrivateKey privateKey = (BCEdDSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);
        String privateKeyStr = privateKey.toString();
        String privateKeyValueStr;
        while (true) {
            if (!privateKeyStr.contains(eddsaPrivateKey.getSignatureAlgorithm().getAlgorithm()))
                break;
            if (!privateKeyStr.contains("Private Key"))
                break;
            int lastIdx = privateKeyStr.lastIndexOf("public data:");
            privateKeyValueStr = privateKeyStr.substring(lastIdx + new String("public data:").length());
            resLength = privateKeyValueStr.trim().length() / 2;
            break;
        }
        return resLength;
    }

    /**
     * Returns Length of Decoded EdDSA Public Key.
     * 
     * @param eddsaPublicKey EdDSA Public Key.
     * @return Length of Decoded EdDSA Public Key.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private int getDecodedKeysLength(EDDSAPublicKey eddsaPublicKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        int resLength = 0;
        X509EncodedKeySpec publicKeySpec = eddsaPublicKey.getPublicKeySpec();
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance(eddsaPublicKey.getSignatureAlgorithm().getName());
        BCEdDSAPublicKey publicKey = (BCEdDSAPublicKey) keyFactory.generatePublic(publicKeySpec);
        String publicKeyStr = publicKey.toString();
        String publicKeyValueStr;
        while (true) {
            if (!publicKeyStr.contains(eddsaPublicKey.getSignatureAlgorithm().getAlgorithm()))
                break;
            if (!publicKeyStr.contains("Public Key"))
                break;
            int lastIdx = publicKeyStr.lastIndexOf("public data:");
            publicKeyValueStr = publicKeyStr.substring(lastIdx + new String("public data:").length());
            resLength = publicKeyValueStr.trim().length() / 2;
            break;
        }
        return resLength;
    }

    /**
     * Loads Keys from a keystore and saves them in the class TestKeys (privateKey, publicKey, certificate).
     * 
     * @param signatureAlgorithm SignatureAlgorithm - signature algorithm.
     * @param keyStore Key Store (file).
     * @param keyStoreSecret Password for access to the Key Store (file).  
     * @param dName Issuer of the generated Certificate.
     * @param keyID Key Id (Alias name).
     * @return instance   
     * @throws Exception
     */
    private TestKeys loadTestKeys(SignatureAlgorithm signatureAlgorithm, String keyStore, String keyStoreSecret,
            String dName, String keyID) throws Exception {

        TestKeys testKeys = new TestKeys();

        AuthCryptoProvider authCryptoProvider = new AuthCryptoProvider(keyStore, keyStoreSecret, dName);
        java.security.Key privateKey = authCryptoProvider.getKeyStore().getKey(keyID, authCryptoProvider.getKeyStoreSecret().toCharArray());
        java.security.PublicKey publicKey = authCryptoProvider.getKeyStore().getCertificate(keyID).getPublicKey();
        java.security.cert.Certificate certificate = authCryptoProvider.getKeyStore().getCertificate(keyID);

        testKeys.privateKey = privateKey;
        testKeys.publicKey = publicKey;
        testKeys.certificate = certificate;

        return testKeys;
    }
}
