/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto;

import com.google.common.collect.Lists;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.impl.ECDSA;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.jwk.*;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.util.CertUtils;
import io.jans.as.model.util.Util;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jcajce.interfaces.EdDSAPublicKey;
import org.bouncycastle.jcajce.spec.EdDSAParameterSpec;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @author Sergey Manoylo
 * @version November 22, 2021
 */
public class AuthCryptoProvider extends AbstractCryptoProvider {

    protected static final Logger LOG = Logger.getLogger(AuthCryptoProvider.class);

    private KeyStore keyStore;
    private String keyStoreFile;
    private String keyStoreSecret;
    private String dnName;
    private final boolean rejectNoneAlg;
    private final KeySelectionStrategy keySelectionStrategy;

    public AuthCryptoProvider() throws KeyStoreException {
        this(null, null, null);
    }

    public AuthCryptoProvider(String keyStoreFile, String keyStoreSecret, String dnName) throws KeyStoreException {
        this(keyStoreFile, keyStoreSecret, dnName, false);
    }

    public AuthCryptoProvider(String keyStoreFile, String keyStoreSecret, String dnName, boolean rejectNoneAlg) throws KeyStoreException {
        this(keyStoreFile, keyStoreSecret, dnName, rejectNoneAlg, AppConfiguration.DEFAULT_KEY_SELECTION_STRATEGY);
    }

    public AuthCryptoProvider(String keyStoreFile, String keyStoreSecret, String dnName, boolean rejectNoneAlg, KeySelectionStrategy keySelectionStrategy) throws KeyStoreException {
        this.rejectNoneAlg = rejectNoneAlg;
        this.keySelectionStrategy = keySelectionStrategy != null ? keySelectionStrategy : AppConfiguration.DEFAULT_KEY_SELECTION_STRATEGY;
        if (!Util.isNullOrEmpty(keyStoreFile) && !Util.isNullOrEmpty(keyStoreSecret)) {
            this.keyStoreFile = keyStoreFile;
            this.keyStoreSecret = keyStoreSecret;
            this.dnName = dnName;
            keyStore = KeyStore.getInstance("PKCS12");
            try {
                File f = new File(keyStoreFile);
                if (!f.exists()) {
                    keyStore.load(null, keyStoreSecret.toCharArray());
                    store();
                }
                load();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private void store() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        try (FileOutputStream fos = new FileOutputStream(keyStoreFile)) {
            keyStore.store(fos, keyStoreSecret.toCharArray());
        }
    }

    public void load() throws IOException, NoSuchAlgorithmException, CertificateException {
        try (InputStream is = new FileInputStream(keyStoreFile)) {
            keyStore.load(is, keyStoreSecret.toCharArray());
            LOG.debug("Loaded keys from JKS.");
            LOG.trace("Loaded keys:" + getKeys());
        }
    }

    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    public String getKeyStoreSecret() {
        return keyStoreSecret;
    }

    public String getDnName() {
        return dnName;
    }

    @Override
    public JSONObject generateKey(Algorithm algorithm, Long expirationTime) throws CryptoProviderException {
        return generateKey(algorithm, expirationTime, 2048);
    }

    @Override
    public JSONObject generateKey(Algorithm algorithm, Long expirationTime, int keyLength, KeyOpsType keyOpsType) throws CryptoProviderException {
        if (algorithm == null) {
            throw new IllegalArgumentException("The signature algorithm parameter cannot be null");
        }
        JSONObject jsonObject = null;
        try {
            Use algUse = algorithm.getUse();
            if (algUse == Use.SIGNATURE) {
                jsonObject = generateKeySignature(algorithm, expirationTime, keyLength, keyOpsType);
            } else if (algUse == Use.ENCRYPTION) {
                jsonObject = generateKeyEncryption(algorithm, expirationTime, keyLength, keyOpsType);
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | OperatorCreationException
                | CertificateException | KeyStoreException | IOException e) {
            throw new CryptoProviderException(e);
        }
        return jsonObject;
    }

    @Override
    public JSONObject generateKey(Algorithm algorithm, Long expirationTime, int keyLength) throws CryptoProviderException {
        return generateKey(algorithm, expirationTime, keyLength, KeyOpsType.CONNECT);
    }

    private static String getKidSuffix(Algorithm algorithm) {
        return "_" + algorithm.getUse().getParamName().toLowerCase() + "_" + algorithm.getParamName().toLowerCase();
    }

    public String getAliasByAlgorithmForDeletion(Algorithm algorithm, String newAlias, KeyOpsType keyOpsType) throws KeyStoreException {
        for (String alias : Collections.list(keyStore.aliases())) {

            if (newAlias.equals(alias)) { // skip newly created alias or ssa keys
                continue;
            }

            if (alias.startsWith(keyOpsType.getValue()) && alias.endsWith(getKidSuffix(algorithm))) {
                return alias;
            }
        }
        return null;
    }

    @Override
    public boolean containsKey(String keyId) {
        try {
            if (StringUtils.isBlank(keyId)) {
                return false;
            }

            return keyStore.getKey(keyId, keyStoreSecret.toCharArray()) != null;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String sign(String signingInput, String alias, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws CryptoProviderException {
        try {
            if (signatureAlgorithm == SignatureAlgorithm.NONE) {
                return "";
            } else if (AlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily())) {
                SecretKey secretKey = new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), signatureAlgorithm.getAlgorithm());
                Mac mac = Mac.getInstance(signatureAlgorithm.getAlgorithm());
                mac.init(secretKey);
                byte[] sig = mac.doFinal(signingInput.getBytes());
                return Base64Util.base64urlencode(sig);
            } else { // EC, ED or RSA
                PrivateKey privateKey = getPrivateKey(alias);
                if (privateKey == null) {
                    final String error = "Failed to find private key by kid: " + alias + ", signatureAlgorithm: " + signatureAlgorithm
                            + "(check whether web keys JSON in persistence corresponds to keystore file), keySelectionStrategy: "
                            + keySelectionStrategy;
                    LOG.error(error);
                    throw new IllegalStateException(error);
                }

                Signature signer = Signature.getInstance(signatureAlgorithm.getAlgorithm(), "BC");
                signer.initSign(privateKey);
                signer.update(signingInput.getBytes());

                byte[] signature = signer.sign();
                if (AlgorithmFamily.EC.equals(signatureAlgorithm.getFamily())) {
                    int signatureLenght = ECDSA.getSignatureByteArrayLength(signatureAlgorithm.getJwsAlgorithm());
                    signature = ECDSA.transcodeSignatureToConcat(signature, signatureLenght);
                }

                return Base64Util.base64urlencode(signature);
            }

        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException | JOSEException e) {
            throw new CryptoProviderException(e);
        }
    }

    @Override
    public boolean verifySignature(String signingInput, String encodedSignature, String alias, JSONObject jwks, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws CryptoProviderException {
        if (rejectNoneAlg && signatureAlgorithm == SignatureAlgorithm.NONE) {
            LOG.trace("None algorithm is forbidden by `rejectJwtWithNoneAlg` property.");
            return false;
        }
        if (signatureAlgorithm == SignatureAlgorithm.NONE) {
            return Util.isNullOrEmpty(encodedSignature);
        } else if (AlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily())) {
            String expectedSignature = sign(signingInput, null, sharedSecret, signatureAlgorithm);
            return expectedSignature.equals(encodedSignature);
        } else { // EC, ED or RSA
            return verifySignatureEcEdRSA(signingInput, encodedSignature, alias, jwks, signatureAlgorithm);
        }
    }

    @Override
    public boolean deleteKey(String alias) throws CryptoProviderException {
        try {
            keyStore.deleteEntry(alias);
        } catch (KeyStoreException e) {
            throw new CryptoProviderException(e);
        }
        try (FileOutputStream stream = new FileOutputStream(keyStoreFile)) {
            keyStore.store(stream, keyStoreSecret.toCharArray());
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new CryptoProviderException(e);
        }
        return true;
    }

    @Override
    public PublicKey getPublicKey(String alias) throws CryptoProviderException {
        if (Util.isNullOrEmpty(alias) || keyStore == null) {
            return null;
        }
        try {
            java.security.cert.Certificate certificate = keyStore.getCertificate(alias);
            if (certificate == null) {
                return null;
            }
            checkKeyExpiration(alias);
            return certificate.getPublicKey();
        } catch (KeyStoreException e) {
            throw new CryptoProviderException(e);
        }
    }

    @Override
    public String getKeyId(JSONWebKeySet jsonWebKeySet, Algorithm algorithm, Use use, KeyOpsType keyOpsType) throws CryptoProviderException {
        if (algorithm == null || AlgorithmFamily.HMAC.equals(algorithm.getFamily())) {
            return null;
        }
        try {
            String kid = null;
            final List<JSONWebKey> keys = jsonWebKeySet.getKeys();
            LOG.trace("WebKeys:" + keys.stream().map(JSONWebKey::getKid).collect(Collectors.toList()));
            LOG.trace("KeyStoreKeys:" + getKeys());

            List<JSONWebKey> keysByAlgAndUse = new ArrayList<>();

            for (JSONWebKey key : keys) {
                boolean keyOpsCondition = keyOpsType == null || (key.getKeyOpsType() == null || key.getKeyOpsType().contains(keyOpsType));
                if (algorithm == key.getAlg() && (use == null || use == key.getUse()) && keyOpsCondition) {
                    kid = key.getKid();
                    Key keyFromStore;
                    keyFromStore = keyStore.getKey(kid, keyStoreSecret.toCharArray());
                    if (keyFromStore != null) {
                        keysByAlgAndUse.add(key);
                    }
                }
            }

            if (keysByAlgAndUse.isEmpty()) {
                LOG.trace("kid is not in keystore, algorithm: {}" + algorithm + ", kid: " + kid + ", keyStorePath:" + keyStoreFile + ", keyOpsType: " + keyOpsType + ", use: " + use);
                return kid;
            }

            final JSONWebKey selectedKey = keySelectionStrategy.select(keysByAlgAndUse);
            final String selectedKid = selectedKey != null ? selectedKey.getKid() : null;
            LOG.trace("Selected kid: " + selectedKid + ", keySelection Strategy: " + keySelectionStrategy);

            return selectedKid;

        } catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new CryptoProviderException(e);
        }
    }

    @Override
    public PrivateKey getPrivateKey(String alias) throws CryptoProviderException {
        if (Util.isNullOrEmpty(alias)) {
            return null;
        }
        try {
            Key key = keyStore.getKey(alias, keyStoreSecret.toCharArray());
            if (key == null) {
                return null;
            }

            PrivateKey privateKey = (PrivateKey) key;

            checkKeyExpiration(alias);

            return privateKey;
        } catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new CryptoProviderException(e);
        }
    }

    public X509Certificate generateV3Certificate(KeyPair keyPair, String issuer, String signatureAlgorithm, Long expirationTime) throws CertIOException, OperatorCreationException, CertificateException {
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

        ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).setProvider("BC").build(privateKey);
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
    }

    @Override
    public List<String> getKeys() {
        try {
            return Collections.list(this.keyStore.aliases());
        } catch (KeyStoreException e) {
            LOG.error(e.getMessage(), e);
            return Lists.newArrayList();
        }
    }

    public SignatureAlgorithm getSignatureAlgorithm(String alias) throws KeyStoreException {
        Certificate[] chain = keyStore.getCertificateChain(alias);
        if ((chain == null) || chain.length == 0) {
            return null;
        }

        X509Certificate cert = (X509Certificate) chain[0];
        return CertUtils.getSignatureAlgorithm(cert);
    }

    private void checkKeyExpiration(String alias) {
        try {
            Date expirationDate = ((X509Certificate) keyStore.getCertificate(alias)).getNotAfter();
            checkKeyExpiration(alias, expirationDate.getTime());
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    private JSONObject generateKeySignature(Algorithm algorithm, Long expirationTime, int keyLength, KeyOpsType keyOpsType)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, OperatorCreationException,
            CertificateException, KeyStoreException, IOException {

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm.getParamName());
        if (signatureAlgorithm == null) {
            algorithm = Algorithm.ES384;
            signatureAlgorithm = SignatureAlgorithm.ES384;
        }
        KeyPairGenerator keyGen = null;
        final AlgorithmFamily algorithmFamily = algorithm.getFamily();
        switch (algorithmFamily) {
            case RSA:
                keyGen = KeyPairGenerator.getInstance(algorithmFamily.toString(), "BC");
                keyGen.initialize(keyLength, new SecureRandom());
                break;
            case EC:
                ECGenParameterSpec eccgen = new ECGenParameterSpec(signatureAlgorithm.getCurve().getAlias());
                keyGen = KeyPairGenerator.getInstance(algorithmFamily.toString(), "BC");
                keyGen.initialize(eccgen, new SecureRandom());
                break;
            case ED:
                EdDSAParameterSpec edSpec = new EdDSAParameterSpec(signatureAlgorithm.getCurve().getAlias());
                keyGen = KeyPairGenerator.getInstance(signatureAlgorithm.getName(), "BC");
                keyGen.initialize(edSpec, new SecureRandom());
                break;
            default:
                throw new IllegalStateException("The provided signature algorithm parameter is not supported: algorithmFamily = " + algorithmFamily);

        }
        return getJson(algorithm, keyGen, signatureAlgorithm.getAlgorithm(), expirationTime, keyOpsType);
    }

    private JSONObject generateKeyEncryption(Algorithm algorithm, Long expirationTime, int keyLength, KeyOpsType keyOpsType) throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidAlgorithmParameterException, OperatorCreationException, CertificateException, KeyStoreException, IOException {

        KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.fromName(algorithm.getParamName());
        if (keyEncryptionAlgorithm == null) {
            algorithm = Algorithm.RS256;
            keyEncryptionAlgorithm = KeyEncryptionAlgorithm.RSA1_5;
        }
        KeyPairGenerator keyGen = null;
        String signatureAlgorithm = null;
        final AlgorithmFamily algorithmFamily = algorithm.getFamily();
        switch (algorithmFamily) {
            case RSA:
                keyGen = KeyPairGenerator.getInstance(algorithmFamily.toString(), "BC");
                keyGen.initialize(keyLength, new SecureRandom());
                signatureAlgorithm = "SHA256WITHRSA";
                break;
            case EC:
                ECGenParameterSpec eccgen = new ECGenParameterSpec(keyEncryptionAlgorithm.getCurve().getAlias());
                keyGen = KeyPairGenerator.getInstance(algorithmFamily.toString(), "BC");
                keyGen.initialize(eccgen, new SecureRandom());
                signatureAlgorithm = "SHA256WITHECDSA";
                break;
            default:
                throw new IllegalStateException(
                        "The provided key encryption algorithm parameter is not supported: algorithmFamily = " + algorithmFamily);

        }
        return getJson(algorithm, keyGen, signatureAlgorithm, expirationTime, keyOpsType);
    }

    private String getKid(Algorithm algorithm, KeyOpsType keyOpsType) {
        if (keyOpsType == null)
            keyOpsType = KeyOpsType.CONNECT;
        return keyOpsType.getValue() + "_" + UUID.randomUUID().toString() + getKidSuffix(algorithm);
    }

    private JSONObject getJson(final Algorithm algorithm, final KeyPairGenerator keyGen, final String signatureAlgorithmStr, final Long expirationTime, KeyOpsType keyOpsType) throws NoSuchAlgorithmException,
            OperatorCreationException, CertificateException, KeyStoreException, IOException {

        // Generate the key
        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey pk = keyPair.getPrivate();

        // Java API requires a certificate chain
        X509Certificate cert = generateV3Certificate(keyPair, dnName, signatureAlgorithmStr, expirationTime);

        X509Certificate[] chain = new X509Certificate[1];
        chain[0] = cert;

        String alias = getKid(algorithm, keyOpsType);
        keyStore.setKeyEntry(alias, pk, keyStoreSecret.toCharArray(), chain);

        final String oldAliasByAlgorithm = getAliasByAlgorithmForDeletion(algorithm, alias, keyOpsType);
        if (StringUtils.isNotBlank(oldAliasByAlgorithm)) {
            keyStore.deleteEntry(oldAliasByAlgorithm);
            LOG.trace("New key: " + alias + ", deleted key: " + oldAliasByAlgorithm);
        }

        try (FileOutputStream stream = new FileOutputStream(keyStoreFile)) {
            keyStore.store(stream, keyStoreSecret.toCharArray());
        }

        final PublicKey publicKey = keyPair.getPublic();

        Use use = algorithm.getUse();

        JSONObject jsonObject = new JSONObject();

        algorithm.fill(jsonObject);

        jsonObject.put(JWKParameter.KEY_ID, alias);
        jsonObject.put(JWKParameter.EXPIRATION_TIME, expirationTime);
        if (publicKey instanceof RSAPublicKey) {
            RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
            jsonObject.put(JWKParameter.MODULUS, Base64Util.base64urlencodeUnsignedBigInt(rsaPublicKey.getModulus()));
            jsonObject.put(JWKParameter.EXPONENT, Base64Util.base64urlencodeUnsignedBigInt(rsaPublicKey.getPublicExponent()));
        } else if (publicKey instanceof ECPublicKey) {
            ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
            if (use == Use.SIGNATURE) {
                SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm.getParamName());
                jsonObject.put(JWKParameter.CURVE, signatureAlgorithm.getCurve().getName());
            } else if (use == Use.ENCRYPTION) {
                KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.fromName(algorithm.getParamName());
                jsonObject.put(JWKParameter.CURVE, keyEncryptionAlgorithm.getCurve().getName());
            }
            jsonObject.put(JWKParameter.X, Base64Util.base64urlencodeUnsignedBigInt(ecPublicKey.getW().getAffineX()));
            jsonObject.put(JWKParameter.Y, Base64Util.base64urlencodeUnsignedBigInt(ecPublicKey.getW().getAffineY()));
        } else if (use == Use.SIGNATURE && publicKey instanceof EdDSAPublicKey) {
            EdDSAPublicKey edDSAPublicKey = (EdDSAPublicKey) publicKey;
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm.getParamName());
            jsonObject.put(JWKParameter.CURVE, signatureAlgorithm.getCurve().getName());
            jsonObject.put(JWKParameter.X, Base64Util.base64urlencode(edDSAPublicKey.getEncoded()));
            // EdDSA keys (EdDSAPublicKey, EDDSAPrivateKey) don't use BigInteger, but only byte[], 
            // so Base64Util.base64urlencode, but not Base64Util.base64urlencodeUnsignedBigInt is used.
        }

        JSONArray x5c = new JSONArray();
        x5c.put(Base64.encodeBase64String(cert.getEncoded()));
        jsonObject.put(JWKParameter.CERTIFICATE_CHAIN, x5c);

        return jsonObject;
    }

    private boolean verifySignatureEcEdRSA(String signingInput, String encodedSignature, String alias, JSONObject jwks, SignatureAlgorithm signatureAlgorithm) {
        PublicKey publicKey = null;
        try {
            if (jwks == null) {
                publicKey = getPublicKey(alias);
            } else {
                publicKey = getPublicKey(alias, jwks, signatureAlgorithm.getAlg());
            }
            if (publicKey == null) {
                return false;
            }
            return verifySignatureEcEdRSA(signingInput, encodedSignature, signatureAlgorithm, publicKey);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    private boolean verifySignatureEcEdRSA(String signingInput, String encodedSignature, SignatureAlgorithm signatureAlgorithm, PublicKey publicKey) throws JOSEException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        byte[] signature = Base64Util.base64urldecode(encodedSignature);
        byte[] signatureDer = signature;
        if (AlgorithmFamily.EC.equals(signatureAlgorithm.getFamily())) {
            signatureDer = ECDSA.transcodeSignatureToDER(signatureDer);
        }
        Signature verifier = Signature.getInstance(signatureAlgorithm.getAlgorithm(), "BC");
        verifier.initVerify(publicKey);
        verifier.update(signingInput.getBytes());
        try {
            return verifier.verify(signatureDer);
        } catch (SignatureException e) {
            return verifier.verify(signature);
        }
    }

}

