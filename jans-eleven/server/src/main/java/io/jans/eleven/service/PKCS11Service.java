/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.service;

import com.google.common.base.Strings;
import io.jans.eleven.model.JwksRequestParam;
import io.jans.eleven.model.KeyRequestParam;
import io.jans.eleven.model.SignatureAlgorithm;
import io.jans.eleven.model.SignatureAlgorithmFamily;
import io.jans.eleven.util.Base64Util;
import jakarta.enterprise.inject.Vetoed;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.*;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version May 18, 2022
 */
@Vetoed
public class PKCS11Service implements Serializable {

    private static final long serialVersionUID = -2541585376018724618L;

    private Logger log = LoggerFactory.getLogger(PKCS11Service.class);

    public static final String UTF8_STRING_ENCODING = "UTF-8";

    private Provider provider;
    private KeyStore keyStore;
    private char[] pin;

    public PKCS11Service() {
    }

    public void init(String pin, Map<String, String> pkcs11Config) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        this.pin = pin.toCharArray();
        this.provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);

        Provider installedProvider = Security.getProvider(provider.getName());
        if (installedProvider == null) {
            Security.addProvider(provider);
        } else {
            provider = installedProvider;
        }

        keyStore = KeyStore.getInstance("PKCS11", provider);
        keyStore.load(null, this.pin);

        installedProvider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (installedProvider == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static InputStream getTokenCfg(Map<String, String> pkcs11Config) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> entry : pkcs11Config.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            sb.append(key).append("=").append(value).append("\n");
        }

        String cfg = sb.toString();

        return new ByteArrayInputStream(cfg.getBytes());
    }

    public String generateKey(String dnName, SignatureAlgorithm signatureAlgorithm, Long expirationTime)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, CertificateException,
            NoSuchProviderException, InvalidKeyException, SignatureException, KeyStoreException, IOException {
        KeyPairGenerator keyGen = null;

        if (signatureAlgorithm == null) {
            throw new RuntimeException("The signature algorithm parameter cannot be null");
        } else if (SignatureAlgorithmFamily.RSA.equals(signatureAlgorithm.getFamily())) {
            keyGen = KeyPairGenerator.getInstance(signatureAlgorithm.getFamily(), provider);
            keyGen.initialize(2048, new SecureRandom());
        } else if (SignatureAlgorithmFamily.EC.equals(signatureAlgorithm.getFamily())) {
            ECGenParameterSpec eccgen = new ECGenParameterSpec(signatureAlgorithm.getCurve().getAlias());
            keyGen = KeyPairGenerator.getInstance(signatureAlgorithm.getFamily());
            keyGen.initialize(eccgen, new SecureRandom());
        } else {
            throw new RuntimeException("The provided signature algorithm parameter is not supported");
        }

        // Generate the key
        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey pk = keyPair.getPrivate();

        // Java API requires a certificate chain
        X509Certificate[] chain = generateV3Certificate(keyPair, dnName, signatureAlgorithm, expirationTime);

        String alias = UUID.randomUUID().toString();

        keyStore.setKeyEntry(alias, pk, pin, chain);
        keyStore.store(null);

        return alias;
    }

    public String getSignature(byte[] signingInput, String alias, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws UnrecoverableEntryException,
            NoSuchAlgorithmException, KeyStoreException, InvalidKeyException, SignatureException, UnsupportedEncodingException {
        if (signatureAlgorithm == SignatureAlgorithm.NONE) {
            return null;
        } else if (SignatureAlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily())) {
            SecretKey secretKey = new SecretKeySpec(sharedSecret.getBytes(UTF8_STRING_ENCODING), signatureAlgorithm.getAlgorithm());
            Mac mac = Mac.getInstance(signatureAlgorithm.getAlgorithm());
            mac.init(secretKey);
            byte[] sig = mac.doFinal(signingInput);
            return Base64Util.base64UrlEncode(sig);
        } else { // EC or RSA
            PrivateKey privateKey = getPrivateKey(alias);

            Signature signature = Signature.getInstance(signatureAlgorithm.getAlgorithm(), provider);
            signature.initSign(privateKey);
            signature.update(signingInput);

            return Base64Util.base64UrlEncode(signature.sign());
        }
    }

    public boolean verifySignature(String signingInput, String encodedSignature, String alias, String sharedSecret,
                                   JwksRequestParam jwksRequestParam, SignatureAlgorithm signatureAlgorithm) throws InvalidKeyException, NoSuchAlgorithmException, KeyStoreException, UnsupportedEncodingException, SignatureException, UnrecoverableEntryException {
        boolean verified = false;

        if (signatureAlgorithm == SignatureAlgorithm.NONE) {
            return Strings.isNullOrEmpty(encodedSignature);
        } else if (SignatureAlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily())) {
            String expectedSignature = getSignature(signingInput.getBytes(), null, sharedSecret, signatureAlgorithm);
            return expectedSignature.equals(encodedSignature);
        } else { // EC or RSA
            PublicKey publicKey = null;

            try {
                if (jwksRequestParam == null) {
                    publicKey = getPublicKey(alias);
                } else {
                    publicKey = getPublicKey(alias, jwksRequestParam);
                }
                if (publicKey == null) {
                    return false;
                }

                byte[] signature = Base64Util.base64UrlDecode(encodedSignature);

                Signature verifier = Signature.getInstance(signatureAlgorithm.getAlgorithm());
                verifier.initVerify(publicKey);
                verifier.update(signingInput.getBytes());
                verified = verifier.verify(signature);
            } catch (NoSuchAlgorithmException e) {
                log.error(e.getMessage(), e);
                verified = false;
            } catch (SignatureException e) {
                log.error(e.getMessage(), e);
                verified = false;
            } catch (InvalidKeyException e) {
                log.error(e.getMessage(), e);
                verified = false;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                verified = false;
            }
        }

        return verified;
    }

    public void deleteKey(String alias) throws KeyStoreException {
        keyStore.deleteEntry(alias);
    }

    public PublicKey getPublicKey(String alias, JwksRequestParam jwksRequestParam) throws InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException {
        PublicKey publicKey = null;

        for (KeyRequestParam key : jwksRequestParam.getKeyRequestParams()) {
            if (alias.equals(key.getKid())) {
                SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromName(key.getAlg());
                if (signatureAlgorithm != null) {
                    if (signatureAlgorithm.getFamily().equals(SignatureAlgorithmFamily.RSA)) {
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(
                                new BigInteger(1, Base64Util.base64UrlDecode(key.getN())),
                                new BigInteger(1, Base64Util.base64UrlDecode(key.getE())));
                        publicKey = keyFactory.generatePublic(pubKeySpec);
                    } else if (signatureAlgorithm.getFamily().equals(SignatureAlgorithmFamily.EC)) {
                        AlgorithmParameters parameters = AlgorithmParameters.getInstance(SignatureAlgorithmFamily.EC);
                        parameters.init(new ECGenParameterSpec(signatureAlgorithm.getCurve().getAlias()));
                        ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);

                        publicKey = KeyFactory.getInstance(SignatureAlgorithmFamily.EC).generatePublic(new ECPublicKeySpec(
                                new ECPoint(
                                        new BigInteger(1, Base64Util.base64UrlDecode(key.getX())),
                                        new BigInteger(1, Base64Util.base64UrlDecode(key.getY()))
                                ), ecParameters));
                    }
                }
            }
        }

        return publicKey;
    }

    public PublicKey getPublicKey(String alias) {
        PublicKey publicKey = null;

        try {
            if (Strings.isNullOrEmpty(alias)) {
                return null;
            }

            Certificate certificate = getCertificate(alias);
            if (certificate == null) {
                return null;
            }
            publicKey = certificate.getPublicKey();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        return publicKey;
    }

    public Certificate getCertificate(String alias) throws KeyStoreException {
        return keyStore.getCertificate(alias);
    }

    private PrivateKey getPrivateKey(String alias)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        if (Strings.isNullOrEmpty(alias)) {
            return null;
        }

        Key key = keyStore.getKey(alias, pin);
        if (key == null) {
            return null;
        }
        PrivateKey privateKey = (PrivateKey) key;

        return privateKey;
    }

    private X509Certificate[] generateV3Certificate(KeyPair pair, String dnName, SignatureAlgorithm signatureAlgorithm,
                                                    Long expirationTime)
            throws NoSuchAlgorithmException, CertificateEncodingException, NoSuchProviderException, InvalidKeyException,
            SignatureException {
        X500Principal principal = new X500Principal(dnName);
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

        certGen.setSerialNumber(serialNumber);
        certGen.setIssuerDN(principal);
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 10000));
        certGen.setNotAfter(new Date(expirationTime));
        certGen.setSubjectDN(principal);
        certGen.setPublicKey(pair.getPublic());
        certGen.setSignatureAlgorithm(signatureAlgorithm.getAlgorithm());

        //certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false));
        //certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        //certGen.addExtension(X509Extensions.ExtendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        //certGen.addExtension(X509Extensions.SubjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.rfc822Name, "test@test.test")));

        X509Certificate[] chain = new X509Certificate[1];
        chain[0] = certGen.generate(pair.getPrivate(), "SunPKCS11-SoftHSM");

        return chain;
    }


    /*public X509Certificate generateV3Certificate(KeyPair keyPair, String issuer, SignatureAlgorithm signatureAlgorithm, Long expirationTime) throws CertIOException, OperatorCreationException, CertificateException {
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // Signers name
        X500Name issuerName = new X500Name(issuer);

        // Subjects name - the same as we are self signed.
        X500Name subjectName = new X500Name(issuer);

        // Serial
        BigInteger serial = new BigInteger(256, new SecureRandom());

        // Not before
        Date notBefore = new Date(System.currentTimeMillis() - 10000);
        Date notAfter = new Date(expirationTime);

        // Create the certificate - version 3
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName, serial, notBefore, notAfter, subjectName, publicKey);

        ASN1EncodableVector purposes = new ASN1EncodableVector();
        purposes.add(KeyPurposeId.id_kp_serverAuth);
        purposes.add(KeyPurposeId.id_kp_clientAuth);
        purposes.add(KeyPurposeId.anyExtendedKeyUsage);

        ASN1ObjectIdentifier extendedKeyUsage = new ASN1ObjectIdentifier("2.5.29.37").intern();
        builder.addExtension(extendedKeyUsage, false, new DERSequence(purposes));

        ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm.getAlgorithm()).setProvider("BC").build(privateKey);
        X509CertificateHolder holder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);

        return cert;
    }*/
}
