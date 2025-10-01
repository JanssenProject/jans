/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import io.jans.as.model.crypto.Certificate;
import io.jans.as.model.crypto.Key;
import io.jans.as.model.crypto.KeyFactory;
import io.jans.as.model.crypto.signature.ECDSAKeyFactory;
import io.jans.as.model.crypto.signature.ECDSAPrivateKey;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAKeyFactory;
import io.jans.as.model.crypto.signature.RSAPrivateKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jws.ECDSASigner;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.server.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum Date: 12.03.2012
 */
public class SignatureTest extends BaseTest {

    @Test
    public void generateRS256Keys() throws Exception {
        showTitle("TEST: generateRS256Keys");

        KeyFactory<RSAPrivateKey, RSAPublicKey> keyFactory = new RSAKeyFactory(SignatureAlgorithm.RS256,
                "CN=Test CA Certificate");

        Key<RSAPrivateKey, RSAPublicKey> key = keyFactory.getKey();

        RSAPrivateKey privateKey = key.getPrivateKey();
        RSAPublicKey publicKey = key.getPublicKey();
        Certificate certificate = key.getCertificate();

        System.out.println(key);

        String signingInput = "Hello World!";
        RSASigner rsaSigner1 = new RSASigner(SignatureAlgorithm.RS256, privateKey);
        String signature = rsaSigner1.generateSignature(signingInput);
        RSASigner rsaSigner2 = new RSASigner(SignatureAlgorithm.RS256, publicKey);
        assertTrue(rsaSigner2.validateSignature(signingInput, signature));
        RSASigner rsaSigner3 = new RSASigner(SignatureAlgorithm.RS256, certificate);
        assertTrue(rsaSigner3.validateSignature(signingInput, signature));
    }

    @Test
    public void generateRS384Keys() throws Exception {
        showTitle("TEST: generateRS384Keys");

        KeyFactory<RSAPrivateKey, RSAPublicKey> keyFactory = new RSAKeyFactory(SignatureAlgorithm.RS384,
                "CN=Test CA Certificate");

        Key<RSAPrivateKey, RSAPublicKey> key = keyFactory.getKey();

        RSAPrivateKey privateKey = key.getPrivateKey();
        RSAPublicKey publicKey = key.getPublicKey();
        Certificate certificate = key.getCertificate();

        System.out.println(key);

        String signingInput = "Hello World!";
        RSASigner rsaSigner1 = new RSASigner(SignatureAlgorithm.RS384, privateKey);
        String signature = rsaSigner1.generateSignature(signingInput);
        RSASigner rsaSigner2 = new RSASigner(SignatureAlgorithm.RS384, publicKey);
        assertTrue(rsaSigner2.validateSignature(signingInput, signature));
        RSASigner rsaSigner3 = new RSASigner(SignatureAlgorithm.RS384, certificate);
        assertTrue(rsaSigner3.validateSignature(signingInput, signature));
    }

    @Test
    public void generateRS512Keys() throws Exception {
        showTitle("TEST: generateRS512Keys");

        KeyFactory<RSAPrivateKey, RSAPublicKey> keyFactory = new RSAKeyFactory(SignatureAlgorithm.RS512,
                "CN=Test CA Certificate");

        Key<RSAPrivateKey, RSAPublicKey> key = keyFactory.getKey();

        RSAPrivateKey privateKey = key.getPrivateKey();
        RSAPublicKey publicKey = key.getPublicKey();
        Certificate certificate = key.getCertificate();

        System.out.println(key);

        String signingInput = "Hello World!";
        RSASigner rsaSigner1 = new RSASigner(SignatureAlgorithm.RS512, privateKey);
        String signature = rsaSigner1.generateSignature(signingInput);
        RSASigner rsaSigner2 = new RSASigner(SignatureAlgorithm.RS512, publicKey);
        assertTrue(rsaSigner2.validateSignature(signingInput, signature));
        RSASigner rsaSigner3 = new RSASigner(SignatureAlgorithm.RS512, certificate);
        assertTrue(rsaSigner3.validateSignature(signingInput, signature));
    }

    @Test
    public void generateES256Keys() throws Exception {
        showTitle("TEST: generateES256Keys");

        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(SignatureAlgorithm.ES256,
                "CN=Test CA Certificate");

        Key<ECDSAPrivateKey, ECDSAPublicKey> key = keyFactory.getKey();

        ECDSAPrivateKey privateKey = key.getPrivateKey();
        ECDSAPublicKey publicKey = key.getPublicKey();
        Certificate certificate = key.getCertificate();

        System.out.println(key);

        String signingInput = "Hello World!";
        ECDSASigner ecdsaSigner1 = new ECDSASigner(SignatureAlgorithm.ES256, privateKey);
        String signature = ecdsaSigner1.generateSignature(signingInput);
        ECDSASigner ecdsaSigner2 = new ECDSASigner(SignatureAlgorithm.ES256, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));
        ECDSASigner ecdsaSigner3 = new ECDSASigner(SignatureAlgorithm.ES256, certificate);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));
    }

    @Test
    public void generateES384Keys() throws Exception {
        showTitle("TEST: generateES384Keys");

        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(SignatureAlgorithm.ES384,
                "CN=Test CA Certificate");

        Key<ECDSAPrivateKey, ECDSAPublicKey> key = keyFactory.getKey();

        ECDSAPrivateKey privateKey = key.getPrivateKey();
        ECDSAPublicKey publicKey = key.getPublicKey();
        Certificate certificate = key.getCertificate();

        System.out.println(key);

        String signingInput = "Hello World!";
        ECDSASigner ecdsaSigner1 = new ECDSASigner(SignatureAlgorithm.ES384, privateKey);
        String signature = ecdsaSigner1.generateSignature(signingInput);
        ECDSASigner ecdsaSigner2 = new ECDSASigner(SignatureAlgorithm.ES384, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));
        ECDSASigner ecdsaSigner3 = new ECDSASigner(SignatureAlgorithm.ES384, certificate);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));
    }

    @Test
    public void generateES512Keys() throws Exception {
        showTitle("TEST: generateES512Keys");

        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(SignatureAlgorithm.ES512,
                "CN=Test CA Certificate");
        ECDSAPrivateKey privateKey = keyFactory.getPrivateKey();
        ECDSAPublicKey publicKey = keyFactory.getPublicKey();
        Certificate certificate = keyFactory.getCertificate();

        System.out.println("PRIVATE KEY");
        System.out.println(privateKey);
        System.out.println("PUBLIC KEY");
        System.out.println(publicKey);
        System.out.println("CERTIFICATE");
        System.out.println(certificate);

        String signingInput = "Hello World!";
        ECDSASigner ecdsaSigner1 = new ECDSASigner(SignatureAlgorithm.ES512, privateKey);
        String signature = ecdsaSigner1.generateSignature(signingInput);
        ECDSASigner ecdsaSigner2 = new ECDSASigner(SignatureAlgorithm.ES512, publicKey);
        assertTrue(ecdsaSigner2.validateSignature(signingInput, signature));
        ECDSASigner ecdsaSigner3 = new ECDSASigner(SignatureAlgorithm.ES512, certificate);
        assertTrue(ecdsaSigner3.validateSignature(signingInput, signature));
    }
}