package org.gluu.oxeleven.service;

import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.gluu.oxeleven.model.SignatureAlgorithm;
import org.gluu.oxeleven.model.SignatureAlgorithmFamily;
import org.gluu.oxeleven.util.Base64Util;
import sun.security.pkcs11.SunPKCS11;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * @author Javier Rojas Blum
 * @version March 31, 2016
 */
public class PKCS11Service {

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

    public String generateKey(String dnName, SignatureAlgorithm signatureAlgorithm)
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
            keyGen = KeyPairGenerator.getInstance("EC", provider);
            keyGen.initialize(eccgen, new SecureRandom());
        } else {
            throw new RuntimeException("The provided signature algorithm parameter is not supported");
        }

        // Generate the key
        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey pk = keyPair.getPrivate();

        // Java API requires a certificate chain
        X509Certificate[] chain = generateV3Certificate(keyPair, dnName, signatureAlgorithm);

        String alias = UUID.randomUUID().toString();

        keyStore.setKeyEntry(alias, pk, pin, chain);
        keyStore.store(null);

        return alias;
    }

    public String getSignature(byte[] signingInput, String alias, SignatureAlgorithm signatureAlgorithm) throws UnrecoverableEntryException,
            NoSuchAlgorithmException, KeyStoreException, InvalidKeyException, SignatureException {
        PrivateKey privateKey = getPrivateKey(alias);

        Signature signature = Signature.getInstance(signatureAlgorithm.getAlgorithm(), provider);
        signature.initSign(privateKey);
        signature.update(signingInput);

        return Base64Util.base64UrlEncode(signature.sign());
    }

    public boolean verifySignature(String signingInput, String encodedSignature, String alias, SignatureAlgorithm signatureAlgorithm)
            throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException, NoSuchPaddingException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException, SignatureException {
        PublicKey publicKey = getPublicKey(alias);

        byte[] signature = Base64Util.base64UrlDecode(encodedSignature);

        Signature verifier = Signature.getInstance(signatureAlgorithm.getAlgorithm(), provider);
        verifier.initVerify(publicKey);
        verifier.update(signingInput.getBytes());
        return verifier.verify(signature);
    }

    public void deleteKey(String alias) throws KeyStoreException {
        keyStore.deleteEntry(alias);
    }

    public PublicKey getPublicKey(String alias)
            throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException {
        PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();

        return publicKey;
    }

    private PrivateKey getPrivateKey(String alias)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        Key key = keyStore.getKey(alias, pin);
        PrivateKey privateKey = (PrivateKey) key;

        return privateKey;
    }

    private X509Certificate[] generateV3Certificate(KeyPair pair, String dnName, SignatureAlgorithm signatureAlgorithm)
            throws NoSuchAlgorithmException, CertificateEncodingException, NoSuchProviderException, InvalidKeyException,
            SignatureException {
        X500Principal principal = new X500Principal(dnName);
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

        certGen.setSerialNumber(serialNumber);
        certGen.setIssuerDN(principal);
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 10000));
        certGen.setNotAfter(new Date(System.currentTimeMillis() + 10000));
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
