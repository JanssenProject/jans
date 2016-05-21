/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.service;

import com.google.common.base.Strings;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.gluu.oxeleven.model.JwksRequestParam;
import org.gluu.oxeleven.model.KeyRequestParam;
import org.gluu.oxeleven.model.SignatureAlgorithm;
import org.gluu.oxeleven.model.SignatureAlgorithmFamily;
import org.gluu.oxeleven.util.Base64Util;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import sun.security.pkcs11.SunPKCS11;
import sun.security.rsa.RSAPublicKeyImpl;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
 * @version May 21, 2016
 */
public class PKCS11Service {

    private static final Log LOG = Logging.getLog(PKCS11Service.class);
    public static String UTF8_STRING_ENCODING = "UTF-8";

    private Provider provider;
    private KeyStore keyStore;
    private char[] pin;

    public PKCS11Service(String pin, Map<String, String> pkcs11Config)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        this.pin = pin.toCharArray();

        init(pkcs11Config);
    }

    private void init(Map<String, String> pkcs11Config) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        provider = new SunPKCS11(getTokenCfg(pkcs11Config));

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
                LOG.error(e.getMessage(), e);
                verified = false;
            } catch (SignatureException e) {
                LOG.error(e.getMessage(), e);
                verified = false;
            } catch (InvalidKeyException e) {
                LOG.error(e.getMessage(), e);
                verified = false;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
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
                        publicKey = new RSAPublicKeyImpl(
                                new BigInteger(1, Base64Util.base64UrlDecode(key.getN())),
                                new BigInteger(1, Base64Util.base64UrlDecode(key.getE())));
                    } else if (signatureAlgorithm.getFamily().equals(SignatureAlgorithmFamily.EC)) {
                        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC", "SunEC");
                        parameters.init(new ECGenParameterSpec(signatureAlgorithm.getCurve().getAlias()));
                        ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);

                        publicKey = KeyFactory.getInstance("EC", "SunEC").generatePublic(new ECPublicKeySpec(
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

            Certificate certificate = keyStore.getCertificate(alias);
            if (certificate == null) {
                return null;
            }
            publicKey = certificate.getPublicKey();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        return publicKey;
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


}
