/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

import java.security.Key;
import java.security.KeyStoreException;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testng.ITestContext;
import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.xml.XmlTest;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.ECDSAPrivateKey;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.EDDSAPrivateKey;
import io.jans.as.model.crypto.signature.EDDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.RSAPrivateKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Use;
import io.jans.as.model.jws.AbstractJwsSigner;
import io.jans.as.model.jws.ECDSASigner;
import io.jans.as.model.jws.EDDSASigner;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.server.BaseTest;
import io.jans.as.server.ConfigurableTest;
import io.jans.as.model.exception.SignatureException;

import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPrivateKey;

/**
 * JwtCrossCheckTest Unit Tests.
 * 
 * @author Yuriy Zabrovarnyy|
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public class JwtCrossCheckTest extends BaseTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Data Provider, that returns array of input parameters for Unit Tests,
     * including correspondent defined Keys Value.
     * 
     * List of Key IDs (types), which can be loaded from keystorage, using
     * JWK.load(...).
     * 
     * @param context ITestContext (unit test) context.
     * @return returns array of input parameters for Unit Tests, including
     *         correspondent defined Keys Value.
     */
    @DataProvider(name = "loadJWKDataProvider")
    public Object[][] loadJWKDataProvider(ITestContext context) {

        final String dnName = context.getCurrentXmlTest().getParameter("dnName");
        final String keyStoreFile = context.getCurrentXmlTest().getParameter("keyStoreFile");
        final String keyStoreSecret = context.getCurrentXmlTest().getParameter("keyStoreSecret");
        final XmlTest xmlTest = context.getCurrentXmlTest();

        return ConfigurableTest.ArquillianDataProvider.provide("ArquillianDataProviderTest#loadJWKDataProvider",
                new Object[][] { { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("RS256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("RS384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("RS512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("ES256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("ES256K_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("ES256K_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("ES384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("ES512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("PS256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("PS384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("PS512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("RSA1_5_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("RSA_OAEP_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("ECDH_ES_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("ECDH_ES_PLUS_A128KW_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("ECDH_ES_PLUS_A192KW_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, xmlTest.getParameter("ECDH_ES_PLUS_A256KW_keyId") } });
    }

    /**
     * Data Provider, that returns array of input parameters for Unit Tests,
     * including correspondent defined Keys Value.
     * 
     * @param context ITestContext (unit test) context.
     * @return returns array of input parameters for Unit Tests, including
     *         correspondent defined Keys Value.
     */
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret" })
    @DataProvider(name = "crossCheckDataProvider")
    public Object[][] crossCheckDataProvider(ITestContext context) {

        final String dnName = context.getCurrentXmlTest().getParameter("dnName");
        final String keyStoreFile = context.getCurrentXmlTest().getParameter("keyStoreFile");
        final String keyStoreSecret = context.getCurrentXmlTest().getParameter("keyStoreSecret");
        final XmlTest xmlTest = context.getCurrentXmlTest();

        return ConfigurableTest.ArquillianDataProvider.provide("ArquillianDataProviderTest#crossCheckDataProvider",
                new Object[][] { { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS256, xmlTest.getParameter("RS256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS384, xmlTest.getParameter("RS384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS512, xmlTest.getParameter("RS512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES256, xmlTest.getParameter("ES256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES256K, xmlTest.getParameter("ES256K_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES384, xmlTest.getParameter("ES384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES512, xmlTest.getParameter("ES512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.PS256, xmlTest.getParameter("PS256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.PS384, xmlTest.getParameter("PS384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.PS512, xmlTest.getParameter("PS512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.EDDSA, xmlTest.getParameter("Ed25519_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ED25519, xmlTest.getParameter("Ed25519_keyId") } });
    }

    /**
     * Data Provider, that returns array of input parameters for Unit Tests,
     * including correspondent defined Keys Value.
     * 
     * @param context ITestContext (unit test) context.
     * @return returns array of input parameters for Unit Tests, including
     *         correspondent defined Keys Value.
     */
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret" })
    @DataProvider(name = "crossCheckDataProvider_Nimbus")
    public Object[][] crossCheckDataProvider_Nimbus(ITestContext context) {

        final String dnName = context.getCurrentXmlTest().getParameter("dnName");
        final String keyStoreFile = context.getCurrentXmlTest().getParameter("keyStoreFile");
        final String keyStoreSecret = context.getCurrentXmlTest().getParameter("keyStoreSecret");
        final XmlTest xmlTest = context.getCurrentXmlTest();

        return ConfigurableTest.ArquillianDataProvider.provide("ArquillianDataProviderTest#crossCheckDataProvider_Nimbus",
                new Object[][] { { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS256, xmlTest.getParameter("RS256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS384, xmlTest.getParameter("RS384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS512, xmlTest.getParameter("RS512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES256, xmlTest.getParameter("ES256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES256K, xmlTest.getParameter("ES256K_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES384, xmlTest.getParameter("ES384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES512, xmlTest.getParameter("ES512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.PS256, xmlTest.getParameter("PS256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.PS384, xmlTest.getParameter("PS384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.PS512, xmlTest.getParameter("PS512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.EDDSA, xmlTest.getParameter("Ed25519_keyId") },
        });
    }

    /**
     * Data Provider, that returns array of input parameters for Unit Tests,
     * including correspondent defined Keys Value.
     * 
     * @param context ITestContext (unit test) context.
     * @return returns array of input parameters for Unit Tests, including
     *         correspondent defined Keys Value.
     */
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret" })
    @DataProvider(name = "crossCheckDataProvider_Provider")
    public Object[][] crossCheckDataProvider_Provider(ITestContext context) {

        final String dnName = context.getCurrentXmlTest().getParameter("dnName");
        final String keyStoreFile = context.getCurrentXmlTest().getParameter("keyStoreFile");
        final String keyStoreSecret = context.getCurrentXmlTest().getParameter("keyStoreSecret");
        final XmlTest xmlTest = context.getCurrentXmlTest();

        return ConfigurableTest.ArquillianDataProvider.provide("ArquillianDataProviderTest#crossCheckDataProvider_Provider",
                new Object[][] { { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS256, xmlTest.getParameter("RS256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS384, xmlTest.getParameter("RS384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS512, xmlTest.getParameter("RS512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES256, xmlTest.getParameter("ES256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES256K, xmlTest.getParameter("ES256K_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES384, xmlTest.getParameter("ES384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES512, xmlTest.getParameter("ES512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.PS256, xmlTest.getParameter("PS256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.PS384, xmlTest.getParameter("PS384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.PS512, xmlTest.getParameter("PS512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.EDDSA, xmlTest.getParameter("Ed25519_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ED25519, xmlTest.getParameter("Ed25519_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ED448, xmlTest.getParameter("Ed448_keyId") },                        
                });

    }

    /**
     * Data Provider, that returns array of input parameters for Unit Tests,
     * including correspondent defined Keys Value.
     * 
     * @param context ITestContext (unit test) context.
     * @return returns array of input parameters for Unit Tests, including
     *         correspondent defined Keys Value.
     */
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret" })
    @DataProvider(name = "crossCheckDataProvider_Signers")
    public Object[][] crossCheckDataProvider_Signers(ITestContext context) {

        final String dnName = context.getCurrentXmlTest().getParameter("dnName");
        final String keyStoreFile = context.getCurrentXmlTest().getParameter("keyStoreFile");
        final String keyStoreSecret = context.getCurrentXmlTest().getParameter("keyStoreSecret");
        final XmlTest xmlTest = context.getCurrentXmlTest();

        return ConfigurableTest.ArquillianDataProvider.provide("ArquillianDataProviderTest#crossCheckDataProvider_Signers",
                new Object[][] {
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS256, xmlTest.getParameter("RS256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS384, xmlTest.getParameter("RS384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS512, xmlTest.getParameter("RS512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES256, xmlTest.getParameter("ES256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES256K, xmlTest.getParameter("ES256K_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES384, xmlTest.getParameter("ES384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES512, xmlTest.getParameter("ES512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES256, xmlTest.getParameter("ES256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES256K, xmlTest.getParameter("ES256K_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES384, xmlTest.getParameter("ES384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES512, xmlTest.getParameter("ES512_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.PS256, xmlTest.getParameter("PS256_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.PS384, xmlTest.getParameter("PS384_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.PS512, xmlTest.getParameter("PS512_keyId") },
/*                        
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.EDDSA, xmlTest.getParameter("Ed25519_keyId") },
*/                        
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ED25519, xmlTest.getParameter("Ed25519_keyId") },
                        { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ED448, xmlTest.getParameter("Ed448_keyId") },
                });
    }

    /**
     * Data Provider, that returns array of input parameters for Unit Tests,
     * including correspondent defined Keys Value.
     * 
     * @param context ITestContext (unit test) context.
     * @return returns array of input parameters for Unit Tests, including
     *         correspondent defined Keys Value.
     */
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret" })
    @DataProvider(name = "crossCheckDataProvider_1")
    public Object[][] crossCheckDataProvider_1(ITestContext context) {

        final String dnName = context.getCurrentXmlTest().getParameter("dnName");
        final String keyStoreFile = context.getCurrentXmlTest().getParameter("keyStoreFile");
        final String keyStoreSecret = context.getCurrentXmlTest().getParameter("keyStoreSecret");
        final XmlTest xmlTest = context.getCurrentXmlTest();

        return ConfigurableTest.ArquillianDataProvider.provide("ArquillianDataProviderTest#crossCheckDataProvider_1", new Object[][] {
                /*
                 * {dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS256,
                 * xmlTest.getParameter("RS256_keyId")}, {dnName, keyStoreFile, keyStoreSecret,
                 * SignatureAlgorithm.RS384, xmlTest.getParameter("RS384_keyId")}, {dnName,
                 * keyStoreFile, keyStoreSecret, SignatureAlgorithm.RS512,
                 * xmlTest.getParameter("RS512_keyId")}, {dnName, keyStoreFile, keyStoreSecret,
                 * SignatureAlgorithm.ES256, xmlTest.getParameter("ES256_keyId")}, {dnName,
                 * keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES256K,
                 * xmlTest.getParameter("ES256K_keyId")}, {dnName, keyStoreFile, keyStoreSecret,
                 * SignatureAlgorithm.ES384, xmlTest.getParameter("ES384_keyId")}, {dnName,
                 * keyStoreFile, keyStoreSecret, SignatureAlgorithm.ES512,
                 * xmlTest.getParameter("ES512_keyId")}, {dnName, keyStoreFile, keyStoreSecret,
                 * SignatureAlgorithm.PS256, xmlTest.getParameter("PS256_keyId")}, {dnName,
                 * keyStoreFile, keyStoreSecret, SignatureAlgorithm.PS384,
                 * xmlTest.getParameter("PS384_keyId")}, {dnName, keyStoreFile, keyStoreSecret,
                 * SignatureAlgorithm.PS512, xmlTest.getParameter("PS512_keyId")}, {dnName,
                 * keyStoreFile, keyStoreSecret, SignatureAlgorithm.EDDSA,
                 * xmlTest.getParameter("Ed25519_keyId")},
                 */
//                    {dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ED25519,  xmlTest.getParameter("Ed25519_keyId")},
                { dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.EDDSA, xmlTest.getParameter("Ed25519_keyId") },
//                    {dnName, keyStoreFile, keyStoreSecret, SignatureAlgorithm.ED448,    xmlTest.getParameter("Ed448_keyId")},                    
        });
    }

    /**
     * Unit Test:
     * 
     * Reading and loading Keys and creating JWKs. Array of parameters defined by
     * the loadJWKDataProvider DataProvider.
     * 
     * @param dnName         Issuer of the generated Certificate.
     * @param keyStoreFile   Key Store (file).
     * @param keyStoreSecret Password for access to the Key Store (file).
     * @param kid            keyID (Alias name).
     * @throws Exception
     */
    @Test(dataProvider = "loadJWKDataProvider")
    public void loadJWK_Test(final String dnName, final String keyStoreFile, final String keyStoreSecret, final String kid)
            throws Exception {
        showTitle("loadJWK_Test");

        Reporter.log("dnName            = " + dnName, true);
        Reporter.log("keyStoreFile      = " + keyStoreFile, true);
        Reporter.log("keyStoreSecret    = " + keyStoreSecret, true);
        Reporter.log("kid               = " + kid, true);

        AuthCryptoProvider authCryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        JWK jwk = JWK.load(authCryptoProvider.getKeyStore(), kid, keyStoreSecret.toCharArray());

        assertTrue(jwk != null);
        assertTrue(jwk.toJSONString().length() != 0);
        assertTrue(jwk.toString().length() != 0);

        Reporter.log("jwk (JSONString)  = " + jwk.toJSONString(), true);
        Reporter.log("jwk               = " + jwk.toString(), true);
    }

    /**
     * Unit Test:
     * 
     * Provides reading and loading of the EdDSA (Ed25519) Key and creating the
     * Ed25519 JWK.
     * 
     * @param dnName         Issuer of the generated Certificate.
     * @param keyStoreFile   Key Store (file).
     * @param keyStoreSecret Password for access to the Key Store (file).
     * @param kid            keyID (Alias name).
     * @throws Exception
     */
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret", "Ed25519_keyId" })
    @Test
    public void loadJWK_ED25519Test(final String dnName, final String keyStoreFile, final String keyStoreSecret, final String kid)
            throws Exception {

        showTitle("loadJWK_ED25519Test");

        Reporter.log("dnName            = " + dnName, true);
        Reporter.log("keyStoreFile      = " + keyStoreFile, true);
        Reporter.log("keyStoreSecret    = " + keyStoreSecret, true);
        Reporter.log("kid               = " + kid, true);

        AuthCryptoProvider authCryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        Key key = authCryptoProvider.getKeyStore().getKey(kid, keyStoreSecret.toCharArray());
        BCEdDSAPrivateKey bcEdPrivKey = (BCEdDSAPrivateKey) key;
        BCEdDSAPublicKey bcEdPubKey = (BCEdDSAPublicKey) bcEdPrivKey.getPublicKey();

        Certificate certificate = authCryptoProvider.getKeyStore().getCertificate(kid);

        EDDSAPublicKey edPubKey = new EDDSAPublicKey(SignatureAlgorithm.ED25519, bcEdPubKey.getEncoded());
        EDDSAPrivateKey edPrivKey = new EDDSAPrivateKey(SignatureAlgorithm.ED25519, bcEdPrivKey.getEncoded(), bcEdPubKey.getEncoded());

        Base64URL edPubKeyBase64 = Base64URL.encode(edPubKey.getPublicKeyEncoded());
        Base64URL edPrivKeyBase64 = Base64URL.encode(edPrivKey.getPrivateKeyEncoded());

        List<Base64> edCerts = new ArrayList<Base64>();
        edCerts.add(Base64.encode(certificate.getEncoded()));

        OctetKeyPair octetKeyPair = new OctetKeyPair.Builder(Curve.Ed25519, edPubKeyBase64).d(edPrivKeyBase64).algorithm(JWSAlgorithm.EdDSA)
                .keyID(kid).x509CertChain(edCerts).build();

        JWK jwk = octetKeyPair;

        assertTrue(jwk != null);
        assertTrue(jwk.toJSONString().length() != 0);
        assertTrue(jwk.toString().length() != 0);

        Reporter.log("jwk (JSONString)  = " + jwk.toJSONString(), true);
        Reporter.log("jwk               = " + jwk.toString(), true);
    }

    /**
     * Unit Test:
     * 
     * Provides reading and loading of the EdDSA (Ed448) Key and creating the Ed448
     * JWK.
     * 
     * @param dnName         Issuer of the generated Certificate.
     * @param keyStoreFile   Key Store (file).
     * @param keyStoreSecret Password for access to the Key Store (file).
     * @param kid            keyID (Alias name).
     * @throws Exception
     */
    @Parameters({ "dnName", "keyStoreFile", "keyStoreSecret", "Ed448_keyId" })
    @Test
    public void loadJWK_ED448Test(final String dnName, final String keyStoreFile, final String keyStoreSecret, final String kid)
            throws Exception {

        showTitle("loadJWK_ED448Test");

        Reporter.log("dnName            = " + dnName, true);
        Reporter.log("keyStoreFile      = " + keyStoreFile, true);
        Reporter.log("keyStoreSecret    = " + keyStoreSecret, true);
        Reporter.log("kid               = " + kid, true);

        AuthCryptoProvider authCryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        Key key = authCryptoProvider.getKeyStore().getKey(kid, keyStoreSecret.toCharArray());
        BCEdDSAPrivateKey bcEdPrivKey = (BCEdDSAPrivateKey) key;
        BCEdDSAPublicKey bcEdPubKey = (BCEdDSAPublicKey) bcEdPrivKey.getPublicKey();

        Certificate certificate = authCryptoProvider.getKeyStore().getCertificate(kid);

        EDDSAPublicKey edPubKey = new EDDSAPublicKey(SignatureAlgorithm.ED448, bcEdPubKey.getEncoded());
        EDDSAPrivateKey edPrivKey = new EDDSAPrivateKey(SignatureAlgorithm.ED448, bcEdPrivKey.getEncoded(), bcEdPubKey.getEncoded());

        Base64URL edPubKeyBase64 = Base64URL.encode(edPubKey.getPublicKeyEncoded());
        Base64URL edPrivKeyBase64 = Base64URL.encode(edPrivKey.getPrivateKeyEncoded());

        List<Base64> edCerts = new ArrayList<Base64>();
        edCerts.add(Base64.encode(certificate.getEncoded()));

        OctetKeyPair octetKeyPair = new OctetKeyPair.Builder(Curve.Ed448, edPubKeyBase64).d(edPrivKeyBase64).algorithm(JWSAlgorithm.EdDSA)
                .keyID(kid).x509CertChain(edCerts).build();

        JWK jwk = octetKeyPair;

        assertTrue(jwk != null);
        assertTrue(jwk.toJSONString().length() != 0);
        assertTrue(jwk.toString().length() != 0);

        Reporter.log("jwk (JSONString)  = " + jwk.toJSONString(), true);
        Reporter.log("jwk               = " + jwk.toString(), true);
    }

    /**
     * Unit Test:
     * 
     * 
     * 
     * @param dnName             Issuer of the generated Certificate.
     * @param keyStoreFile       Key Store (file).
     * @param keyStoreSecret     Password for access to the Key Store (file).
     * @param signatureAlgorithm Signature Algorithm (SignatureAlgorithm).
     * @param kid                keyID (Alias name).
     * @throws Exception
     */
    @Test(dataProvider = "crossCheckDataProvider")
    public void crossCheckTest(final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final SignatureAlgorithm signatureAlgorithm, final String kid) throws Exception {

        showTitle("crossCheckTest");

        Reporter.log("dnName                = " + dnName, true);
        Reporter.log("keyStoreFile          = " + keyStoreFile, true);
        Reporter.log("keyStoreSecret        = " + keyStoreSecret, true);
        Reporter.log("signatureAlgorithm    = " + signatureAlgorithm, true);
        Reporter.log("kid                   = " + kid, true);

        crossCheck(new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName), signatureAlgorithm, kid);
    }

    @Test(dataProvider = "crossCheckDataProvider_1")
    public void testED(final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final SignatureAlgorithm signatureAlgorithm, final String kid) throws Exception {

        Jwt jwt = new Jwt();

        jwt.getHeader().setKeyId(kid);
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(signatureAlgorithm);

        jwt.getClaims().setSubjectIdentifier("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5");
        jwt.getClaims().setIssuer("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5");
        jwt.getClaims().setExpirationTime(new Date(1575559276888000L));
        jwt.getClaims().setIssuedAt(new Date(1575559276888000L));
        jwt.getClaims().setAudience("https://gomer-vbox/jans-auth/restv1/token");

        AuthCryptoProvider authCryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String signature = authCryptoProvider.sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), null, signatureAlgorithm);
        jwt.setEncodedSignature(signature);

        String jwtStr = jwt.toString();

        Reporter.log("jwtStr                = " + jwtStr, true);

        validateNimbus(jwtStr, authCryptoProvider, jwt.getHeader().getKeyId(), signatureAlgorithm);

//        validate(jwtStr, authCryptoProvider, jwt.getHeader().getKeyId(), signatureAlgorithm);        

/*        
          if(SignatureAlgorithm.ED25519 == signatureAlgorithm) {
          
          Key key = authCryptoProvider.getKeyStore().getKey(kid,
          keyStoreSecret.toCharArray()); BCEdDSAPrivateKey bcEdPrivKey =
          (BCEdDSAPrivateKey)key; BCEdDSAPublicKey bcEdPubKey = (BCEdDSAPublicKey)
          bcEdPrivKey.getPublicKey();
          
          Certificate certificate =
          authCryptoProvider.getKeyStore().getCertificate(kid);
          
          EDDSAPublicKey edPubKey = new EDDSAPublicKey(SignatureAlgorithm.ED25519,
          bcEdPubKey.getEncoded()); EDDSAPrivateKey edPrivKey = new
          EDDSAPrivateKey(SignatureAlgorithm.ED25519, bcEdPrivKey.getEncoded(),
          bcEdPubKey.getEncoded());
          
          Base64URL edPubKeyBase64 = Base64URL.encode(edPubKey.getPublicKeyEncoded());
          Base64URL edPrivKeyBase64 =
          Base64URL.encode(edPrivKey.getPrivateKeyEncoded());
          
          List<Base64> edCerts = new ArrayList<Base64>();
          edCerts.add(Base64.encode(certificate.getEncoded()));
          
          OctetKeyPair octetKeyPair = new OctetKeyPair.Builder(Curve.Ed25519,
          edPubKeyBase64).d(edPrivKeyBase64).
          algorithm(JWSAlgorithm.EdDSA).keyID(kid).x509CertChain(edCerts).build();
          
          JWK jwk = octetKeyPair;
          
          assertTrue(jwk != null); assertTrue(jwk.toJSONString().length() != 0);
          assertTrue(jwk.toString().length() != 0);
          
          EDDSASigner eddsaSigner = new EDDSASigner(signatureAlgorithm, edPrivKey);
          
          String signature1 = eddsaSigner.generateSignature(jwt.getSigningInput());
          
          } else if(SignatureAlgorithm.ED448 == signatureAlgorithm) {
          
          Key key = authCryptoProvider.getKeyStore().getKey(kid,
          keyStoreSecret.toCharArray()); BCEdDSAPrivateKey bcEdPrivKey =
          (BCEdDSAPrivateKey)key; BCEdDSAPublicKey bcEdPubKey = (BCEdDSAPublicKey)
          bcEdPrivKey.getPublicKey();
          
          Certificate certificate =
          authCryptoProvider.getKeyStore().getCertificate(kid);
          
          EDDSAPublicKey edPubKey = new EDDSAPublicKey(SignatureAlgorithm.ED448,
          bcEdPubKey.getEncoded()); EDDSAPrivateKey edPrivKey = new
          EDDSAPrivateKey(SignatureAlgorithm.ED448, bcEdPrivKey.getEncoded(),
          bcEdPubKey.getEncoded());
          
          Base64URL edPubKeyBase64 = Base64URL.encode(edPubKey.getPublicKeyEncoded());
          Base64URL edPrivKeyBase64 =
          Base64URL.encode(edPrivKey.getPrivateKeyEncoded());
          
          List<Base64> edCerts = new ArrayList<Base64>();
          edCerts.add(Base64.encode(certificate.getEncoded()));
          
          OctetKeyPair octetKeyPair = new OctetKeyPair.Builder(Curve.Ed448,
          edPubKeyBase64).d(edPrivKeyBase64).
          algorithm(JWSAlgorithm.EdDSA).keyID(kid).x509CertChain(edCerts).build();
          
          JWK jwk = octetKeyPair;
          
          assertTrue(jwk != null); assertTrue(jwk.toJSONString().length() != 0);
          assertTrue(jwk.toString().length() != 0);
          
          EDDSASigner eddsaSigner = new EDDSASigner(signatureAlgorithm, edPrivKey);
          
          String signature1 = eddsaSigner.generateSignature(jwt.getSigningInput());
      }
*/      
/*
      Reporter.log("jwk (JSONString)  = " + jwk.toJSONString(), true);
      Reporter.log("jwk               = " + jwk.toString(), true);

      String signingInput = jwt.getSigningInput();
      EDDSASigner eddsaSigner1 = new EDDSASigner(signatureAlgorithm, privateKey);
      String signature = eddsaSigner1.generateSignature(signingInput);
*/
    }

    @Test(dataProvider = "crossCheckDataProvider_Nimbus")
    public void testNimbus(final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final SignatureAlgorithm signatureAlgorithm, final String kid) throws Exception {

        Jwt jwt = new Jwt();

        jwt.getHeader().setKeyId(kid);
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(signatureAlgorithm);

        jwt.getClaims().setSubjectIdentifier("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5");
        jwt.getClaims().setIssuer("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5");
        jwt.getClaims().setExpirationTime(new Date(1575559276888000L));
        jwt.getClaims().setIssuedAt(new Date(1575559276888000L));
        jwt.getClaims().setAudience("https://gomer-vbox/jans-auth/restv1/token");

        AuthCryptoProvider authCryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String jwtStr = createNimbusJwt(authCryptoProvider, kid, signatureAlgorithm);

        Reporter.log("jwtStr                = " + jwtStr, true);

        validateNimbus(jwtStr, authCryptoProvider, jwt.getHeader().getKeyId(), signatureAlgorithm);
    }

    @Test(dataProvider = "crossCheckDataProvider_Provider")
    public void testProvider(final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final SignatureAlgorithm signatureAlgorithm, final String kid) throws Exception {

        AuthCryptoProvider authCryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String jwtStr = createOxAuthJwtProvider(authCryptoProvider, kid, signatureAlgorithm);
        Reporter.log("jwtStr                = " + jwtStr, true);
        
        validateOxAuthProvider(jwtStr, authCryptoProvider, kid, signatureAlgorithm);
        validateOxAuthSigners(jwtStr, authCryptoProvider, kid, signatureAlgorithm);
        
//        validateNimbus(jwtStr, authCryptoProvider, kid, signatureAlgorithm);        
    }

    @Test(dataProvider = "crossCheckDataProvider_Signers")
    public void testSigners(final String dnName, final String keyStoreFile, final String keyStoreSecret,
            final SignatureAlgorithm signatureAlgorithm, final String kid) throws Exception {
        
        AuthCryptoProvider authCryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        
        String jwtStr = createOxAuthJwtSigners(authCryptoProvider, kid, signatureAlgorithm);
        Reporter.log("jwtStr                = " + jwtStr, true);
        
        validateOxAuthSigners(jwtStr, authCryptoProvider, kid, signatureAlgorithm);
        validateOxAuthProvider(jwtStr, authCryptoProvider, kid, signatureAlgorithm);
        
//        validateNimbus(jwtStr, authCryptoProvider, kid, signatureAlgorithm);        
    }

    /**
     * 
     * 
     * @param cryptoProvider     Crypto Provider (tool, that provides access to
     *                           keystorages and provides basic operations with
     *                           keys).
     * @param signatureAlgorithm Signature Algorithm (SignatureAlgorithm).
     * @param kid                keyID (Alias name).
     * @throws Exception
     */
    private void crossCheck(AuthCryptoProvider cryptoProvider, SignatureAlgorithm signatureAlgorithm, String kid) throws Exception {
        Reporter.log(String.format("Cross check for %s ...", signatureAlgorithm.getName()), true);

        final String nimbusJwt = createNimbusJwt(cryptoProvider, kid, signatureAlgorithm);
        validate(nimbusJwt, cryptoProvider, kid, signatureAlgorithm);

        Reporter.log(String.format("nimbusJwt = %s", nimbusJwt), true);

        if (signatureAlgorithm == SignatureAlgorithm.ED25519) {
            signatureAlgorithm = SignatureAlgorithm.EDDSA;
        }

        final String oxauthJwt = createOxauthJwt(cryptoProvider, kid, signatureAlgorithm);
        validate(oxauthJwt, cryptoProvider, kid, signatureAlgorithm);

        Reporter.log(String.format("oxauthJwt = %s", oxauthJwt), true);

        Reporter.log(String.format("Finished cross check for %s.", signatureAlgorithm.getName()), true);
    }
    

    /**
     *
     * @param cryptoProvider
     * @param kid
     * @param signatureAlgorithm
     * @return
     * @throws Exception
     */
    private static String createNimbusJwt(AuthCryptoProvider cryptoProvider, String kid, SignatureAlgorithm signatureAlgorithm)
            throws Exception {
        final AlgorithmFamily family = signatureAlgorithm.getFamily();
        JWSSigner signer = null;
        switch (family) {
        case RSA: {
            signer = new RSASSASigner(RSAKey.load(cryptoProvider.getKeyStore(), kid, cryptoProvider.getKeyStoreSecret().toCharArray()));
            break;
        }
        case EC: {
            signer = new com.nimbusds.jose.crypto.ECDSASigner(
                    ECKey.load(cryptoProvider.getKeyStore(), kid, cryptoProvider.getKeyStoreSecret().toCharArray()));
            break;
        }
        case ED: {
            switch (signatureAlgorithm) {
            case EDDSA:
            case ED25519: {
                Key key = cryptoProvider.getKeyStore().getKey(kid, cryptoProvider.getKeyStoreSecret().toCharArray());
                BCEdDSAPrivateKey bcEdPrivKey = (BCEdDSAPrivateKey) key;
                BCEdDSAPublicKey bcEdPubKey = (BCEdDSAPublicKey) bcEdPrivKey.getPublicKey();
                EDDSAPrivateKey edPrivKey = new EDDSAPrivateKey(signatureAlgorithm, bcEdPrivKey.getEncoded(), bcEdPubKey.getEncoded());
                OctetKeyPair okp = new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(edPrivKey.getPublicKeyDecoded()))
                        .d(Base64URL.encode(edPrivKey.getPrivateKeyDecoded())).build();
                signer = new Ed25519Signer(okp);
                break;
            }
            // case ED448: // Nimbus doesn't support ED448
            default:
                throw new SignatureException(String.format("wrong type of the SignatureAlgorithm: %s", signatureAlgorithm.toString()));
            }
            break;
        }
        default:
            throw new SignatureException(String.format("wrong type of the Algorithm Family: %s", family.toString()));
        }
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5")
                .issuer("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5").expirationTime(new Date(1575559276888000L))
                .issueTime(new Date(1575559276888000L)).audience("https://gomer-vbox/jans-auth/restv1/token").build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(signatureAlgorithm.getJwsAlgorithm()).keyID(kid).build(), claimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    /**
     *
     * @param cryptoProvider
     * @param kid
     * @param signatureAlgorithm
     * @return
     * @throws Exception
     */
    private static String createOxauthJwt(AuthCryptoProvider cryptoProvider, String kid, SignatureAlgorithm signatureAlgorithm) throws Exception {
        Jwt jwt = new Jwt();

        jwt.getHeader().setKeyId(kid);
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(signatureAlgorithm);

        jwt.getClaims().setSubjectIdentifier("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5");
        jwt.getClaims().setIssuer("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5");
        jwt.getClaims().setExpirationTime(new Date(1575559276888000L));
        jwt.getClaims().setIssuedAt(new Date(1575559276888000L));
        jwt.getClaims().setAudience("https://gomer-vbox/jans-auth/restv1/token");

        String signature = cryptoProvider.sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), null, signatureAlgorithm);
        jwt.setEncodedSignature(signature);
        return jwt.toString();
    }

    /**
     *
     * @param cryptoProvider
     * @param kid
     * @param signatureAlgorithm
     * @return
     * @throws Exception
     */
    private static String createOxAuthJwtProvider(AuthCryptoProvider cryptoProvider, String kid, SignatureAlgorithm signatureAlgorithm) throws Exception {
        Jwt jwt = new Jwt();

        jwt.getHeader().setKeyId(kid);
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(signatureAlgorithm);

        jwt.getClaims().setSubjectIdentifier("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5");
        jwt.getClaims().setIssuer("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5");
        jwt.getClaims().setExpirationTime(new Date(1575559276888000L));
        jwt.getClaims().setIssuedAt(new Date(1575559276888000L));
        jwt.getClaims().setAudience("https://gomer-vbox/jans-auth/restv1/token");

        String signature = cryptoProvider.sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), null, signatureAlgorithm);
        jwt.setEncodedSignature(signature);
        return jwt.toString();
    }

    /**
     *
     * @param cryptoProvider
     * @param kid
     * @param algorithm
     * @return
     * @throws Exception
     */
    private static String createOxAuthJwtSigners(AuthCryptoProvider cryptoProvider, String kid, SignatureAlgorithm signatureAlgorithm) throws Exception {
        Jwt jwt = new Jwt();

        jwt.getHeader().setKeyId(kid);
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(signatureAlgorithm);

        jwt.getClaims().setSubjectIdentifier("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5");
        jwt.getClaims().setIssuer("1202.d50a4eeb-ab5d-474b-aaaf-e4aa47bc54a5");
        jwt.getClaims().setExpirationTime(new Date(1575559276888000L));
        jwt.getClaims().setIssuedAt(new Date(1575559276888000L));
        jwt.getClaims().setAudience("https://gomer-vbox/jans-auth/restv1/token");

        AbstractJwsSigner oxauthVerifier = null;
        switch (signatureAlgorithm.getFamily()) {
        case EC: {
            java.security.Key privateKey = cryptoProvider.getKeyStore().getKey(kid, cryptoProvider.getKeyStoreSecret().toCharArray());
            java.security.PublicKey publicKey = cryptoProvider.getKeyStore().getCertificate(kid).getPublicKey();
            java.security.cert.Certificate certificate = cryptoProvider.getKeyStore().getCertificate(kid);
            
            java.security.interfaces.ECPrivateKey ecPrivateKey = (java.security.interfaces.ECPrivateKey) privateKey;
            java.security.interfaces.ECPublicKey ecPublicKey = (java.security.interfaces.ECPublicKey) publicKey;
            
            oxauthVerifier = new ECDSASigner(jwt.getHeader().getSignatureAlgorithm(), new ECDSAPrivateKey(
                    jwt.getHeader().getSignatureAlgorithm(), ecPrivateKey.getS()));
            break;
        }
        case ED: {
            switch (signatureAlgorithm) {
            case EDDSA:
            case ED25519:
            case ED448: {
                java.security.Key privateKey = cryptoProvider.getKeyStore().getKey(kid, cryptoProvider.getKeyStoreSecret().toCharArray());
                java.security.PublicKey publicKey = cryptoProvider.getKeyStore().getCertificate(kid).getPublicKey();
                java.security.cert.Certificate certificate = cryptoProvider.getKeyStore().getCertificate(kid);
                
                BCEdDSAPrivateKey bcEdPrivKey = (BCEdDSAPrivateKey) privateKey;
                BCEdDSAPublicKey bcEdPubKey = (BCEdDSAPublicKey) bcEdPrivKey.getPublicKey();
                EDDSAPublicKey edPubKey = new EDDSAPublicKey(jwt.getHeader().getSignatureAlgorithm(), bcEdPubKey.getEncoded());
                
                oxauthVerifier = new EDDSASigner(jwt.getHeader().getSignatureAlgorithm(), new EDDSAPrivateKey(jwt.getHeader().getSignatureAlgorithm(), bcEdPrivKey.getEncoded(), bcEdPubKey.getEncoded()));
                
                break;
            }
            default:
                throw new SignatureException(String.format("wrong type of the SignatureAlgorithm: %s", signatureAlgorithm.toString()));
            }
            break;
        }
        case RSA: {
            java.security.Key privateKey = cryptoProvider.getKeyStore().getKey(kid, cryptoProvider.getKeyStoreSecret().toCharArray());
            java.security.PublicKey publicKey = cryptoProvider.getKeyStore().getCertificate(kid).getPublicKey();
            java.security.cert.Certificate certificate = cryptoProvider.getKeyStore().getCertificate(kid);
            
            java.security.interfaces.RSAPrivateKey rsaPrivateKey = (java.security.interfaces.RSAPrivateKey) privateKey;            
            java.security.interfaces.RSAPublicKey rsaPublicKey = (java.security.interfaces.RSAPublicKey) publicKey;
            
            oxauthVerifier = new RSASigner(signatureAlgorithm, new RSAPrivateKey(signatureAlgorithm, rsaPrivateKey.getModulus(), rsaPrivateKey.getPrivateExponent()));
            break;
        }
        default:
            throw new SignatureException(
                    String.format("wrong type of the Algorithm Family: %s", signatureAlgorithm.getFamily().toString()));
        }

        jwt = oxauthVerifier.sign(jwt);

        return jwt.toString();
    }

    /**
     * 
     * @param jwtAsString
     * @param cryptoProvider     Crypto Provider (tool, that provides access to
     *                           keystorages and provides basic operations with
     *                           keys).
     * @param kid                keyID (Alias name).
     * @param signatureAlgorithm Signature Algorithm (SignatureAlgorithm).
     * @throws Exception
     */
    private static void validate(String jwtAsString, AuthCryptoProvider cryptoProvider, String kid, SignatureAlgorithm signatureAlgorithm)
            throws Exception {
        // SignedJWT signedJWT = SignedJWT.parse(jwtAsString);
        Jwt jwt = Jwt.parse(jwtAsString);
        JWSVerifier nimbusVerifier = null;
        AbstractJwsSigner oxauthVerifier = null;
        switch (signatureAlgorithm.getFamily()) {
        case EC: {
            final ECKey ecKey = ECKey.load(cryptoProvider.getKeyStore(), kid, cryptoProvider.getKeyStoreSecret().toCharArray());
            final ECPublicKey ecPublicKey = ecKey.toECPublicKey();
            nimbusVerifier = new ECDSAVerifier(ecKey);
            oxauthVerifier = new ECDSASigner(jwt.getHeader().getSignatureAlgorithm(), new ECDSAPublicKey(
                    jwt.getHeader().getSignatureAlgorithm(), ecPublicKey.getW().getAffineX(), ecPublicKey.getW().getAffineY()));
            break;
        }
        case ED: {
            switch (signatureAlgorithm) {
            case EDDSA:
            case ED25519: {
                Key key = cryptoProvider.getKeyStore().getKey(kid, cryptoProvider.getKeyStoreSecret().toCharArray());
                BCEdDSAPrivateKey bcEdPrivKey = (BCEdDSAPrivateKey) key;
                BCEdDSAPublicKey bcEdPubKey = (BCEdDSAPublicKey) bcEdPrivKey.getPublicKey();
                EDDSAPublicKey edPubKey = new EDDSAPublicKey(jwt.getHeader().getSignatureAlgorithm(), bcEdPubKey.getEncoded());
                OctetKeyPair okp = new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(edPubKey.getPublicKeyDecoded())).build();
                nimbusVerifier = new Ed25519Verifier(okp);
                oxauthVerifier = new EDDSASigner(jwt.getHeader().getSignatureAlgorithm(),
                        new EDDSAPublicKey(jwt.getHeader().getSignatureAlgorithm(), bcEdPubKey.getEncoded()));
                break;
            }
            // case ED448: // Nimbus doesn't support ED448
            default:
                throw new SignatureException(String.format("wrong type of the SignatureAlgorithm: %s", signatureAlgorithm.toString()));
            }
            break;
        }
        case RSA: {
            RSAKey rsaKey = RSAKey.load(cryptoProvider.getKeyStore(), kid, cryptoProvider.getKeyStoreSecret().toCharArray());
            final java.security.interfaces.RSAPublicKey rsaPublicKey = rsaKey.toRSAPublicKey();
            nimbusVerifier = new RSASSAVerifier(rsaKey);
            oxauthVerifier = new RSASigner(signatureAlgorithm,
                    new RSAPublicKey(rsaPublicKey.getModulus(), rsaPublicKey.getPublicExponent()));
            break;
        }
        default:
            throw new SignatureException(
                    String.format("wrong type of the Algorithm Family: %s", signatureAlgorithm.getFamily().toString()));
        }

        assertNotNull(nimbusVerifier);
        assertNotNull(oxauthVerifier);

        // Nimbus
        // assertTrue(signedJWT.verify(nimbusVerifier));

        // oxauth cryptoProvider
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), kid, null, null, jwt.getHeader().getSignatureAlgorithm());
        assertTrue(validJwt);

        // oxauth verifier
        assertTrue(oxauthVerifier.validate(jwt));
    }

    private static void validateNimbus(String jwtAsString, AuthCryptoProvider cryptoProvider, String kid,
            SignatureAlgorithm signatureAlgorithm) throws Exception {

        SignedJWT signedJWT = SignedJWT.parse(jwtAsString);
        JWSVerifier nimbusVerifier = null;

        switch (signatureAlgorithm.getFamily()) {
        case EC: {
            final ECKey ecKey = ECKey.load(cryptoProvider.getKeyStore(), kid, cryptoProvider.getKeyStoreSecret().toCharArray());
            // final ECPublicKey ecPublicKey = ecKey.toECPublicKey();
            nimbusVerifier = new ECDSAVerifier(ecKey);
            break;
        }
        case ED: {
            switch (signatureAlgorithm) {
            case EDDSA:
            case ED25519: {
                Key key = cryptoProvider.getKeyStore().getKey(kid, cryptoProvider.getKeyStoreSecret().toCharArray());
                BCEdDSAPrivateKey bcEdPrivKey = (BCEdDSAPrivateKey) key;
                BCEdDSAPublicKey bcEdPubKey = (BCEdDSAPublicKey) bcEdPrivKey.getPublicKey();
                EDDSAPublicKey edPubKey = new EDDSAPublicKey(signatureAlgorithm, bcEdPubKey.getEncoded());
                OctetKeyPair okp = new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(edPubKey.getPublicKeyDecoded())).build();
                nimbusVerifier = new Ed25519Verifier(okp);
                break;
            }
            // case ED448: // Nimbus doesn't support ED448
            default:
                throw new SignatureException(String.format("wrong type of the SignatureAlgorithm: %s", signatureAlgorithm.toString()));
            }
            break;
        }
        case RSA: {
            RSAKey rsaKey = RSAKey.load(cryptoProvider.getKeyStore(), kid, cryptoProvider.getKeyStoreSecret().toCharArray());
            // final java.security.interfaces.RSAPublicKey rsaPublicKey =
            // rsaKey.toRSAPublicKey();
            nimbusVerifier = new RSASSAVerifier(rsaKey);
            break;
        }
        default:
            throw new SignatureException(
                    String.format("wrong type of the Algorithm Family: %s", signatureAlgorithm.getFamily().toString()));
        }

        assertNotNull(nimbusVerifier);

        // Nimbus
        assertTrue(signedJWT.verify(nimbusVerifier));

    }

    private static void validateOxAuthProvider(String jwtAsString, AuthCryptoProvider cryptoProvider, String kid, SignatureAlgorithm signatureAlgorithm) throws Exception {
        Jwt jwt = Jwt.parse(jwtAsString);
        // oxauth cryptoProvider
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), kid, null, null,
                jwt.getHeader().getSignatureAlgorithm());
        assertTrue(validJwt);
    }

    private static void validateOxAuthSigners(String jwtAsString, AuthCryptoProvider cryptoProvider, String kid, SignatureAlgorithm signatureAlgorithm) throws Exception {

        Jwt jwt = Jwt.parse(jwtAsString);
        AbstractJwsSigner oxauthVerifier = null;
        switch (signatureAlgorithm.getFamily()) {
        case EC: {
            java.security.Key privateKey = cryptoProvider.getKeyStore().getKey(kid, cryptoProvider.getKeyStoreSecret().toCharArray());
            java.security.PublicKey publicKey = cryptoProvider.getKeyStore().getCertificate(kid).getPublicKey();
            java.security.cert.Certificate certificate = cryptoProvider.getKeyStore().getCertificate(kid);
            
            java.security.interfaces.ECPrivateKey ecPrivateKey = (java.security.interfaces.ECPrivateKey) privateKey;
            java.security.interfaces.ECPublicKey ecPublicKey = (java.security.interfaces.ECPublicKey) publicKey;
            
/*            
            final ECKey ecKey = ECKey.load(cryptoProvider.getKeyStore(), kid, cryptoProvider.getKeyStoreSecret().toCharArray());
            final ECPublicKey ecPublicKey = ecKey.toECPublicKey();
*/            
            oxauthVerifier = new ECDSASigner(jwt.getHeader().getSignatureAlgorithm(), new ECDSAPublicKey(
                    jwt.getHeader().getSignatureAlgorithm(), ecPublicKey.getW().getAffineX(), ecPublicKey.getW().getAffineY()));
            break;
        }
        case ED: {
            switch (signatureAlgorithm) {
            case EDDSA:
            case ED25519:
            case ED448: {
                java.security.Key privateKey = cryptoProvider.getKeyStore().getKey(kid, cryptoProvider.getKeyStoreSecret().toCharArray());
                java.security.PublicKey publicKey = cryptoProvider.getKeyStore().getCertificate(kid).getPublicKey();
                java.security.cert.Certificate certificate = cryptoProvider.getKeyStore().getCertificate(kid);
                
                BCEdDSAPrivateKey bcEdPrivKey = (BCEdDSAPrivateKey) privateKey;
                BCEdDSAPublicKey bcEdPubKey = (BCEdDSAPublicKey) bcEdPrivKey.getPublicKey();
                EDDSAPublicKey edPubKey = new EDDSAPublicKey(jwt.getHeader().getSignatureAlgorithm(), bcEdPubKey.getEncoded());
                
                oxauthVerifier = new EDDSASigner(jwt.getHeader().getSignatureAlgorithm(),
                        new EDDSAPublicKey(jwt.getHeader().getSignatureAlgorithm(), bcEdPubKey.getEncoded()));
                break;
            }
            default:
                throw new SignatureException(String.format("wrong type of the SignatureAlgorithm: %s", signatureAlgorithm.toString()));
            }
            break;
        }
        case RSA: {
            java.security.Key privateKey = cryptoProvider.getKeyStore().getKey(kid, cryptoProvider.getKeyStoreSecret().toCharArray());
            java.security.PublicKey publicKey = cryptoProvider.getKeyStore().getCertificate(kid).getPublicKey();
            java.security.cert.Certificate certificate = cryptoProvider.getKeyStore().getCertificate(kid);
            
            java.security.interfaces.RSAPrivateKey rsaPrivateKey = (java.security.interfaces.RSAPrivateKey) privateKey;            
            java.security.interfaces.RSAPublicKey rsaPublicKey = (java.security.interfaces.RSAPublicKey) publicKey;
            
//            RSAKey rsaKey = RSAKey.load(cryptoProvider.getKeyStore(), kid, cryptoProvider.getKeyStoreSecret().toCharArray());
            oxauthVerifier = new RSASigner(signatureAlgorithm,
                    new RSAPublicKey(rsaPublicKey.getModulus(), rsaPublicKey.getPublicExponent()));
            break;
        }
        default:
            throw new SignatureException(
                    String.format("wrong type of the Algorithm Family: %s", signatureAlgorithm.getFamily().toString()));
        }
        // oxauth verifier
        assertTrue(oxauthVerifier.validate(jwt));        
    }

    /**
     *
     * @param algorithm
     * @param use
     * @param cryptoProvider
     * @return
     * @throws KeyStoreException
     */
    @SuppressWarnings("unused")
    private static String getKeyIdByAlgorithm(SignatureAlgorithm algorithm, Use use, AuthCryptoProvider cryptoProvider)
            throws KeyStoreException {
        final List<String> aliases = cryptoProvider.getKeys();
        for (String keyId : aliases) {
            if (keyId.endsWith(use.getParamName() + "_" + algorithm.getName().toLowerCase())) {
                return keyId;
            }
        }
        return null;
    }
}
