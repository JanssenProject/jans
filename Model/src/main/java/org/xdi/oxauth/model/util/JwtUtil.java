/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.util;

import static org.xdi.oxauth.model.jwk.JWKParameter.D;
import static org.xdi.oxauth.model.jwk.JWKParameter.EXPONENT;
import static org.xdi.oxauth.model.jwk.JWKParameter.JSON_WEB_KEY_SET;
import static org.xdi.oxauth.model.jwk.JWKParameter.JWKS_ALGORITHM;
import static org.xdi.oxauth.model.jwk.JWKParameter.JWKS_KEY_ID;
import static org.xdi.oxauth.model.jwk.JWKParameter.KEY_ID;
import static org.xdi.oxauth.model.jwk.JWKParameter.MODULUS;
import static org.xdi.oxauth.model.jwk.JWKParameter.PRIVATE_KEY;
import static org.xdi.oxauth.model.jwk.JWKParameter.PRIVATE_MODULUS;
import static org.xdi.oxauth.model.jwk.JWKParameter.PRIVATE_EXPONENT;
import static org.xdi.oxauth.model.jwk.JWKParameter.PUBLIC_KEY;
import static org.xdi.oxauth.model.jwk.JWKParameter.PUBLIC_MODULUS;
import static org.xdi.oxauth.model.jwk.JWKParameter.PUBLIC_EXPONENT;
import static org.xdi.oxauth.model.jwk.JWKParameter.X;
import static org.xdi.oxauth.model.jwk.JWKParameter.X5C;
import static org.xdi.oxauth.model.jwk.JWKParameter.Y;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.HttpMethod;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.openssl.PEMReader;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.xdi.oxauth.model.crypto.Certificate;
import org.xdi.oxauth.model.crypto.signature.ECDSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.util.StringHelper;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version 0.9 May 18, 2015
 */
public class JwtUtil {

    private static final Logger log = Logger.getLogger(JwtUtil.class);

    public static byte[] unsignedToBytes(int[] plaintextUnsignedBytes) {
        byte[] bytes = new byte[plaintextUnsignedBytes.length];

        for (int i = 0; i < plaintextUnsignedBytes.length; i++) {
            bytes[i] = (byte) plaintextUnsignedBytes[i];
        }

        return bytes;
    }

    public static String encodeJwt(JSONObject jsonHeader, JSONObject jsonClaim,
                                   SignatureAlgorithm algorithm) {
        if (jsonHeader != null && jsonClaim != null && algorithm == SignatureAlgorithm.NONE) {
            return encodeJwt(jsonHeader, jsonClaim, algorithm, null, null, null);
        }
        return null;
    }

    public static String encodeJwt(JSONObject jsonHeader, JSONObject jsonClaim,
                                   SignatureAlgorithm algorithm, String sharedKey) {
        if (jsonHeader != null && jsonClaim != null && algorithm != null && sharedKey != null) {
            return encodeJwt(jsonHeader, jsonClaim, algorithm, sharedKey, null, null);
        }
        return null;
    }

    public static String encodeJwt(JSONObject jsonHeader, JSONObject jsonClaim,
                                   SignatureAlgorithm algorithm, RSAPrivateKey privateKey) {
        if (jsonHeader != null && jsonClaim != null && algorithm != null && privateKey != null) {
            return encodeJwt(jsonHeader, jsonClaim, algorithm, null, privateKey, null);
        }
        return null;
    }

    public static String encodeJwt(JSONObject jsonHeader, JSONObject jsonClaim,
                                   SignatureAlgorithm algorithm, ECDSAPrivateKey privateKey) {
        if (jsonHeader != null && jsonClaim != null && algorithm != null && privateKey != null) {
            return encodeJwt(jsonHeader, jsonClaim, algorithm, null, null, privateKey);
        }
        return null;
    }

    private static String encodeJwt(JSONObject jsonHeader, JSONObject jsonClaim,
                                    SignatureAlgorithm algorithm, String sharedKey,
                                    RSAPrivateKey rsaPrivateKey,
                                    ECDSAPrivateKey ecdsaPrivateKey) {
        String signature = "";

        String header = jsonHeader.toString();
        String claim = jsonClaim.toString();

        try {
            header = base64urlencode(header.getBytes(Util.UTF8_STRING_ENCODING));
            claim = base64urlencode(claim.getBytes(Util.UTF8_STRING_ENCODING));

            String signingInput = header + "." + claim;
            byte[] sign = null;

            switch (algorithm) {
                case NONE:
                    break;
                case HS256:
                    sign = getSignatureHS256(signingInput.getBytes(Util.UTF8_STRING_ENCODING),
                            sharedKey.getBytes(Util.UTF8_STRING_ENCODING));
                    break;
                case HS384:
                    sign = getSignatureHS384(signingInput.getBytes(Util.UTF8_STRING_ENCODING),
                            sharedKey.getBytes(Util.UTF8_STRING_ENCODING));
                    break;
                case HS512:
                    sign = getSignatureHS512(signingInput.getBytes(Util.UTF8_STRING_ENCODING),
                            sharedKey.getBytes(Util.UTF8_STRING_ENCODING));
                    break;
                case RS256:
                    sign = getSignatureRS256(signingInput.getBytes(Util.UTF8_STRING_ENCODING), rsaPrivateKey);
                    break;
                case RS384:
                    sign = getSignatureRS384(signingInput.getBytes(Util.UTF8_STRING_ENCODING), rsaPrivateKey);
                    break;
                case RS512:
                    sign = getSignatureRS512(signingInput.getBytes(Util.UTF8_STRING_ENCODING), rsaPrivateKey);
                    break;
                case ES256:
                    sign = getSignatureES256(signingInput.getBytes(Util.UTF8_STRING_ENCODING), ecdsaPrivateKey);
                    break;
                case ES384:
                    sign = getSignatureES384(signingInput.getBytes(Util.UTF8_STRING_ENCODING), ecdsaPrivateKey);
                    break;
                case ES512:
                    sign = getSignatureES512(signingInput.getBytes(Util.UTF8_STRING_ENCODING), ecdsaPrivateKey);
                    break;
                default:
                    throw new UnsupportedOperationException("Algorithm not supported");
            }

            if (sign != null) {
                signature = base64urlencode(sign);
            }
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
        } catch (InvalidKeyException e) {
            log.error(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
        } catch (InvalidKeySpecException e) {
            log.error(e.getMessage(), e);
        } catch (NoSuchProviderException e) {
            log.error(e.getMessage(), e);
        } catch (SignatureException e) {
            log.error(e.getMessage(), e);
        }

        final StringBuilder builder = new StringBuilder();
        builder.append(header).append('.').append(claim).append('.').append(signature);
        return builder.toString();
    }

    public static boolean verifySignatureHS256(byte[] signingInput, byte[] sigBytes, String hsKey) throws IllegalBlockSizeException, IOException, InvalidKeyException, NoSuchProviderException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException {
        boolean validSignature = false;

        validSignature = Arrays.equals(sigBytes, getSignatureHS256(signingInput, hsKey.getBytes(Util.UTF8_STRING_ENCODING)));

        return validSignature;
    }

    public static boolean verifySignatureHS384(byte[] signingInput, byte[] sigBytes, String hsKey) throws IllegalBlockSizeException, IOException, InvalidKeyException, NoSuchProviderException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException {
        boolean validSignature = false;

        validSignature = Arrays.equals(sigBytes, getSignatureHS384(signingInput, hsKey.getBytes(Util.UTF8_STRING_ENCODING)));

        return validSignature;
    }

    public static boolean verifySignatureHS512(byte[] signingInput, byte[] sigBytes, String hsKey) throws IllegalBlockSizeException, IOException, InvalidKeyException, NoSuchProviderException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException {
        boolean validSignature = false;

        validSignature = Arrays.equals(sigBytes, getSignatureHS512(signingInput, hsKey.getBytes(Util.UTF8_STRING_ENCODING)));

        return validSignature;
    }

    public static String base64urlencode(byte[] arg) {
    	return Base64Util.base64urlencode(arg);
    }

    public static byte[] base64urldecode(String arg) throws IllegalArgumentException {
    	return Base64Util.base64urldecode(arg);
    }

    public static void printAlgorithmsAndProviders() {
        Set<String> algorithms = Security.getAlgorithms("Signature");
        for (String algorithm : algorithms) {
            log.trace("Algorithm (Signature): " + algorithm);
        }
        algorithms = Security.getAlgorithms("MessageDigest");
        for (String algorithm : algorithms) {
            log.trace("Algorithm (MessageDigest): " + algorithm);
        }
        algorithms = Security.getAlgorithms("Cipher");
        for (String algorithm : algorithms) {
            log.trace("Algorithm (Cipher): " + algorithm);
        }
        algorithms = Security.getAlgorithms("Mac");
        for (String algorithm : algorithms) {
            log.trace("Algorithm (Mac): " + algorithm);
        }
        algorithms = Security.getAlgorithms("KeyStore");
        for (String algorithm : algorithms) {
            log.trace("Algorithm (KeyStore): " + algorithm);
        }
        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            log.trace("Provider: " + provider.getName());
        }
    }

    public static byte[] getMessageDigestSHA256(String data)
            throws NoSuchProviderException, NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest mda = MessageDigest.getInstance("SHA-256", "BC");
        return mda.digest(data.getBytes(Util.UTF8_STRING_ENCODING));
    }

    public static byte[] getMessageDigestSHA384(String data)
            throws NoSuchProviderException, NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest mda = MessageDigest.getInstance("SHA-384", "BC");
        return mda.digest(data.getBytes(Util.UTF8_STRING_ENCODING));
    }

    public static byte[] getMessageDigestSHA512(String data)
            throws NoSuchProviderException, NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest mda = MessageDigest.getInstance("SHA-512", "BC");
        return mda.digest(data.getBytes(Util.UTF8_STRING_ENCODING));
    }

    public static byte[] getSignatureHS256(byte[] signingInput, byte[] key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKey secretKey = new SecretKeySpec(key, "HMACSHA256");
        Mac mac = Mac.getInstance("HMACSHA256");
        mac.init(secretKey);
        return mac.doFinal(signingInput);
    }

    public static byte[] getSignatureHS384(byte[] signingInput, byte[] key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKey secretKey = new SecretKeySpec(key, "HMACSHA384");
        Mac mac = Mac.getInstance("HMACSHA384");
        mac.init(secretKey);
        return mac.doFinal(signingInput);
    }

    public static byte[] getSignatureHS512(byte[] signingInput, byte[] key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKey secretKey = new SecretKeySpec(key, "HMACSHA512");
        Mac mac = Mac.getInstance("HMACSHA512");
        mac.init(secretKey);
        return mac.doFinal(signingInput);
    }

    public static KeyPair generateRsaKey() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyGen.initialize(2048, new SecureRandom());

        return keyGen.generateKeyPair();
    }

    public static byte[] getSignatureRS256(byte[] signingInput, RSAPrivateKey rsaPrivateKey)
            throws SignatureException, InvalidKeyException, NoSuchProviderException, InvalidKeySpecException, NoSuchAlgorithmException {
        RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(
                rsaPrivateKey.getModulus(),
                rsaPrivateKey.getPrivateExponent());

        KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
        PrivateKey privateKey = keyFactory.generatePrivate(rsaPrivateKeySpec);

        Signature signature = Signature.getInstance("SHA256withRSA", "BC");
        signature.initSign(privateKey);
        signature.update(signingInput);

        return signature.sign();
    }

    public static boolean verifySignatureRS256(byte[] signingInput, byte[] sigBytes, RSAPublicKey rsaPublicKey) throws IllegalBlockSizeException, IOException, InvalidKeyException, NoSuchProviderException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException {
        RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(
                rsaPublicKey.getModulus(),
                rsaPublicKey.getPublicExponent());

        KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
        PublicKey publicKey = keyFactory.generatePublic(rsaPublicKeySpec);

        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        byte[] decSig = cipher.doFinal(sigBytes);
        ASN1InputStream aIn = new ASN1InputStream(decSig);
        try {
            ASN1Sequence seq = (ASN1Sequence) aIn.readObject();

            MessageDigest hash = MessageDigest.getInstance("SHA-256", "BC");
            hash.update(signingInput);

            ASN1OctetString sigHash = (ASN1OctetString) seq.getObjectAt(1);
            return MessageDigest.isEqual(hash.digest(), sigHash.getOctets());
        } finally {
            IOUtils.closeQuietly(aIn);
        }
    }

    public static boolean verifySignatureRS256(byte[] signingInput, byte[] sigBytes, X509Certificate cert) throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        PublicKey publicKey = cert.getPublicKey();

        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        byte[] decSig = cipher.doFinal(sigBytes);
        ASN1InputStream aIn = new ASN1InputStream(decSig);
        try {
            ASN1Sequence seq = (ASN1Sequence) aIn.readObject();

            MessageDigest hash = MessageDigest.getInstance("SHA-256", "BC");
            hash.update(signingInput);

            ASN1OctetString sigHash = (ASN1OctetString) seq.getObjectAt(1);
            return MessageDigest.isEqual(hash.digest(), sigHash.getOctets());
        } finally {
            IOUtils.closeQuietly(aIn);
        }
    }

    public static byte[] getSignatureRS384(byte[] signingInput, RSAPrivateKey rsaPrivateKey)
            throws SignatureException, InvalidKeyException, NoSuchProviderException, InvalidKeySpecException, NoSuchAlgorithmException {
        RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(
                rsaPrivateKey.getModulus(),
                rsaPrivateKey.getPrivateExponent());

        KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
        PrivateKey privateKey = keyFactory.generatePrivate(rsaPrivateKeySpec);

        Signature signature = Signature.getInstance("SHA384withRSA", "BC");
        signature.initSign(privateKey);
        signature.update(signingInput);

        return signature.sign();
    }

    public static boolean verifySignatureRS384(byte[] signingInput, byte[] sigBytes, RSAPublicKey rsaPublicKey) throws IllegalBlockSizeException, IOException, InvalidKeyException, NoSuchProviderException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException {
        RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(
                rsaPublicKey.getModulus(),
                rsaPublicKey.getPublicExponent());

        KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
        PublicKey publicKey = keyFactory.generatePublic(rsaPublicKeySpec);

        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        byte[] decSig = cipher.doFinal(sigBytes);
        ASN1InputStream aIn = new ASN1InputStream(decSig);
        try {
            ASN1Sequence seq = (ASN1Sequence) aIn.readObject();

            MessageDigest hash = MessageDigest.getInstance("SHA-384", "BC");
            hash.update(signingInput);

            ASN1OctetString sigHash = (ASN1OctetString) seq.getObjectAt(1);
            return MessageDigest.isEqual(hash.digest(), sigHash.getOctets());
        } finally {
            IOUtils.closeQuietly(aIn);
        }
    }

    public static boolean verifySignatureRS384(byte[] signingInput, byte[] sigBytes, X509Certificate cert) throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        PublicKey publicKey = cert.getPublicKey();

        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        byte[] decSig = cipher.doFinal(sigBytes);
        ASN1InputStream aIn = new ASN1InputStream(decSig);
        try {
            ASN1Sequence seq = (ASN1Sequence) aIn.readObject();

            MessageDigest hash = MessageDigest.getInstance("SHA-384", "BC");
            hash.update(signingInput);

            ASN1OctetString sigHash = (ASN1OctetString) seq.getObjectAt(1);
            return MessageDigest.isEqual(hash.digest(), sigHash.getOctets());
        } finally {
            IOUtils.closeQuietly(aIn);
        }
    }

    public static byte[] getSignatureRS512(byte[] signingInput, RSAPrivateKey rsaPrivateKey)
            throws SignatureException, InvalidKeyException, NoSuchProviderException, InvalidKeySpecException, NoSuchAlgorithmException {
        RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(
                rsaPrivateKey.getModulus(),
                rsaPrivateKey.getPrivateExponent());

        KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
        PrivateKey privateKey = keyFactory.generatePrivate(rsaPrivateKeySpec);

        Signature signature = Signature.getInstance("SHA512withRSA", "BC");
        signature.initSign(privateKey);
        signature.update(signingInput);

        return signature.sign();
    }

    public static boolean verifySignatureRS512(byte[] signingInput, byte[] sigBytes, RSAPublicKey rsaPublicKey) throws IllegalBlockSizeException, IOException, InvalidKeyException, NoSuchProviderException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException {
        RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(
                rsaPublicKey.getModulus(),
                rsaPublicKey.getPublicExponent());

        KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
        PublicKey publicKey = keyFactory.generatePublic(rsaPublicKeySpec);

        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        byte[] decSig = cipher.doFinal(sigBytes);
        ASN1InputStream aIn = new ASN1InputStream(decSig);
        try {
            ASN1Sequence seq = (ASN1Sequence) aIn.readObject();

            MessageDigest hash = MessageDigest.getInstance("SHA-512", "BC");
            hash.update(signingInput);

            ASN1OctetString sigHash = (ASN1OctetString) seq.getObjectAt(1);
            return MessageDigest.isEqual(hash.digest(), sigHash.getOctets());
        } finally {
            IOUtils.closeQuietly(aIn);
        }
    }

    public static boolean verifySignatureRS512(byte[] signingInput, byte[] sigBytes, X509Certificate cert) throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        PublicKey publicKey = cert.getPublicKey();

        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        byte[] decSig = cipher.doFinal(sigBytes);
        ASN1InputStream aIn = new ASN1InputStream(decSig);
        try {
            ASN1Sequence seq = (ASN1Sequence) aIn.readObject();

            MessageDigest hash = MessageDigest.getInstance("SHA-512", "BC");
            hash.update(signingInput);

            ASN1OctetString sigHash = (ASN1OctetString) seq.getObjectAt(1);
            return MessageDigest.isEqual(hash.digest(), sigHash.getOctets());
        } finally {
            IOUtils.closeQuietly(aIn);
        }
    }

    public static KeyPair generateKeyES256() throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyGen.initialize(ecSpec, new SecureRandom());

        return keyGen.generateKeyPair();
    }

    public static KeyPair generateKeyES384() throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-384");

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyGen.initialize(ecSpec, new SecureRandom());

        return keyGen.generateKeyPair();
    }

    public static KeyPair generateKeyES512() throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-521");

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyGen.initialize(ecSpec, new SecureRandom());

        return keyGen.generateKeyPair();
    }

    public static byte[] getSignatureES256(byte[] signingInput, ECDSAPrivateKey ecdsaPrivateKey)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException,
            SignatureException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(ecdsaPrivateKey.getD(), ecSpec);

        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        Signature signature = Signature.getInstance("SHA256WITHECDSA", "BC");
        signature.initSign(privateKey);
        signature.update(signingInput);

        return signature.sign();
    }

    public static byte[] getSignatureES384(byte[] signingInput, ECDSAPrivateKey ecdsaPrivateKey)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException,
            SignatureException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-384");
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(ecdsaPrivateKey.getD(), ecSpec);

        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        Signature signature = Signature.getInstance("SHA384WITHECDSA", "BC");
        signature.initSign(privateKey);
        signature.update(signingInput);

        return signature.sign();
    }

    public static byte[] getSignatureES512(byte[] signingInput, ECDSAPrivateKey ecdsaPrivateKey)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException,
            SignatureException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-521");
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(ecdsaPrivateKey.getD(), ecSpec);

        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        Signature signature = Signature.getInstance("SHA512WITHECDSA", "BC");
        signature.initSign(privateKey);
        signature.update(signingInput);

        return signature.sign();
    }

    public static boolean verifySignatureES256(byte[] signingInput, byte[] sigBytes, ECDSAPublicKey ecdsaPublicKey)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException, SignatureException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");
        BigInteger q = ((ECCurve.Fp) ecSpec.getCurve()).getQ();
        ECFieldElement xFieldElement = new ECFieldElement.Fp(q, ecdsaPublicKey.getX());
        ECFieldElement yFieldElement = new ECFieldElement.Fp(q, ecdsaPublicKey.getY());
        ECPoint pointQ = new ECPoint.Fp(ecSpec.getCurve(), xFieldElement, yFieldElement);
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        Signature signature = Signature.getInstance("SHA256WITHECDSA", "BC");
        signature.initVerify(publicKey);
        signature.update(signingInput);
        return signature.verify(sigBytes);
    }

    public static boolean verifySignatureES256(byte[] signingInput, byte[] sigBytes, X509Certificate cert)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        PublicKey publicKey = cert.getPublicKey();

        Signature signature = Signature.getInstance("SHA256WITHECDSA", "BC");
        signature.initVerify(publicKey);
        signature.update(signingInput);
        return signature.verify(sigBytes);
    }

    public static boolean verifySignatureES384(byte[] signingInput, byte[] sigBytes, ECDSAPublicKey ecdsaPublicKey)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException, SignatureException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-384");
        BigInteger q = ((ECCurve.Fp) ecSpec.getCurve()).getQ();
        ECFieldElement xFieldElement = new ECFieldElement.Fp(q, ecdsaPublicKey.getX());
        ECFieldElement yFieldElement = new ECFieldElement.Fp(q, ecdsaPublicKey.getY());
        ECPoint pointQ = new ECPoint.Fp(ecSpec.getCurve(), xFieldElement, yFieldElement);
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        Signature signature = Signature.getInstance("SHA384WITHECDSA", "BC");
        signature.initVerify(publicKey);
        signature.update(signingInput);
        return signature.verify(sigBytes);
    }

    public static boolean verifySignatureES384(byte[] signingInput, byte[] sigBytes, X509Certificate cert)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        PublicKey publicKey = cert.getPublicKey();

        Signature signature = Signature.getInstance("SHA384WITHECDSA", "BC");
        signature.initVerify(publicKey);
        signature.update(signingInput);
        return signature.verify(sigBytes);
    }

    public static boolean verifySignatureES512(byte[] signingInput, byte[] sigBytes, ECDSAPublicKey ecdsaPublicKey)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException, SignatureException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-521");
        BigInteger q = ((ECCurve.Fp) ecSpec.getCurve()).getQ();
        ECFieldElement xFieldElement = new ECFieldElement.Fp(q, ecdsaPublicKey.getX());
        ECFieldElement yFieldElement = new ECFieldElement.Fp(q, ecdsaPublicKey.getY());
        ECPoint pointQ = new ECPoint.Fp(ecSpec.getCurve(), xFieldElement, yFieldElement);
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        Signature signature = Signature.getInstance("SHA512WITHECDSA", "BC");
        signature.initVerify(publicKey);
        signature.update(signingInput);
        return signature.verify(sigBytes);
    }

    public static boolean verifySignatureES512(byte[] signingInput, byte[] sigBytes, X509Certificate cert)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        PublicKey publicKey = cert.getPublicKey();

        Signature signature = Signature.getInstance("SHA512WITHECDSA", "BC");
        signature.initVerify(publicKey);
        signature.update(signingInput);
        return signature.verify(sigBytes);
    }

    @Deprecated
    public static org.xdi.oxauth.model.crypto.PublicKey getPublicKey(
            String jwksUri, String jwks, SignatureAlgorithm signatureAlgorithm, String keyId) {
    	org.xdi.oxauth.model.crypto.PublicKey publicKey = null;

    	// TODO: Temporary solution. Testing jwks is in old format!!!
    	try {
			publicKey = getPublicKeyOldImpl(jwksUri, jwks, signatureAlgorithm, keyId);
		} catch (Exception ex) {}

    	if (publicKey == null) {
    		publicKey = getPublicKey(jwksUri, jwks, keyId);
    	}

    	return publicKey;
    }

    @Deprecated
	private static org.xdi.oxauth.model.crypto.PublicKey getPublicKeyOldImpl(String jwksUri, String jwks, SignatureAlgorithm signatureAlgorithm, String keyId) {
		log.debug("Retrieving JWK...");

        org.xdi.oxauth.model.crypto.PublicKey publicKey = null;

        try {
            if (StringHelper.isEmpty(jwks)) {
                ClientRequest clientRequest = new ClientRequest(jwksUri);
                clientRequest.setHttpMethod(HttpMethod.GET);
                ClientResponse<String> clientResponse = clientRequest.get(String.class);

                int status = clientResponse.getStatus();
                log.debug(String.format("Status: %n%d", status));

                if (status == 200) {
                    jwks = clientResponse.getEntity(String.class);
                    log.debug(String.format("JWK: %s", jwks));
                }
            }
            if (StringHelper.isNotEmpty(jwks)) {
                JSONObject jsonObject = new JSONObject(jwks);
                JSONArray keys = jsonObject.getJSONArray(JSON_WEB_KEY_SET);
                if (keys.length() > 0) {
                    JSONObject jsonKeyValue = null;
                    if (StringHelper.isEmpty(keyId)) {
                        jsonKeyValue = keys.getJSONObject(0);
                    } else {
                        for (int i = 0; i < keys.length(); i++) {
                            JSONObject kv = keys.getJSONObject(i);
                            if (kv.get(KEY_ID).equals(keyId)) {
                                jsonKeyValue = kv;
                                break;
                            }
                        }
                    }

                    if (jsonKeyValue == null) {
                        return null;
                    }

                    if (signatureAlgorithm == SignatureAlgorithm.RS256 || signatureAlgorithm == SignatureAlgorithm.RS384 || signatureAlgorithm == SignatureAlgorithm.RS512) {
                        //String alg = jsonKeyValue.getString(ALGORITHM);
                        //String use = jsonKeyValue.getString(KEY_USE);
                        String exp = jsonKeyValue.getString(EXPONENT);
                        String mod = jsonKeyValue.getString(MODULUS);

                        BigInteger publicExponent = new BigInteger(1, JwtUtil.base64urldecode(exp));
                        BigInteger modulus = new BigInteger(1, JwtUtil.base64urldecode(mod));

                        publicKey = new RSAPublicKey(modulus, publicExponent);
                    } else if (signatureAlgorithm == SignatureAlgorithm.ES256 || signatureAlgorithm == SignatureAlgorithm.ES384 || signatureAlgorithm == SignatureAlgorithm.ES512) {
                        //String alg = jsonKeyValue.getString(ALGORITHM);
                        //String use = jsonKeyValue.getString(KEY_USE);
                        //String crv = jsonKeyValue.getString(CURVE);
                        String xx = jsonKeyValue.getString(X);
                        String yy = jsonKeyValue.getString(Y);

                        BigInteger x = new BigInteger(1, JwtUtil.base64urldecode(xx));
                        BigInteger y = new BigInteger(1, JwtUtil.base64urldecode(yy));

                        publicKey = new ECDSAPublicKey(signatureAlgorithm, x, y);
                    }

                    if (publicKey != null && jsonKeyValue.has(X5C)) {
                        final String BEGIN = "-----BEGIN CERTIFICATE-----";
                        final String END = "-----END CERTIFICATE-----";

                        JSONArray certChain = jsonKeyValue.getJSONArray(X5C);
                        String certificateString = BEGIN + "\n" + certChain.getString(0) + "\n" + END;
                        StringReader sr = new StringReader(certificateString);
                        PEMReader pemReader = new PEMReader(sr);
                        X509Certificate cert = (X509CertificateObject) pemReader.readObject();
                        Certificate certificate = new Certificate(signatureAlgorithm, cert);
                        publicKey.setCertificate(certificate);
                    }
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return publicKey;
	}

	public static JSONObject getJsonKey(String jwksUri, String jwks, String keyId) {
		log.debug("Retrieving JWK Key...");

		JSONObject jsonKey = null;
		try {
			if (StringHelper.isEmpty(jwks)) {
				ClientRequest clientRequest = new ClientRequest(jwksUri);
				clientRequest.setHttpMethod(HttpMethod.GET);
				ClientResponse<String> clientResponse = clientRequest.get(String.class);

				int status = clientResponse.getStatus();
				log.debug(String.format("Status: %n%d", status));

				if (status == 200) {
					jwks = clientResponse.getEntity(String.class);
					log.debug(String.format("JWK: %s", jwks));
				}
			}
			if (StringHelper.isNotEmpty(jwks)) {
				JSONObject jsonObject = new JSONObject(jwks);
				JSONArray keys = jsonObject.getJSONArray(JSON_WEB_KEY_SET);
				if (keys.length() > 0) {
					if (StringHelper.isEmpty(keyId)) {
						jsonKey = keys.getJSONObject(0);
					} else {
						for (int i = 0; i < keys.length(); i++) {
							JSONObject kv = keys.getJSONObject(i);
							if (kv.getString(JWKS_KEY_ID).equals(keyId)) {
								jsonKey = kv;
								break;
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}

		return jsonKey;
	}

    public static org.xdi.oxauth.model.crypto.PublicKey getPublicKey(
            String jwksUri, String jwks, String keyId) {
        log.debug("Retrieving JWK Public Key...");

        JSONObject jsonKeyValue = getJsonKey(jwksUri, jwks, keyId);
        if (jsonKeyValue == null) {
            return null;
        }
        
        org.xdi.oxauth.model.crypto.PublicKey publicKey = null;

        try {
        	SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromName(jsonKeyValue.getString(JWKS_ALGORITHM));
            if (signatureAlgorithm == null) {
                log.error(String.format("Failed to determine key '%s' signature algorithm", keyId));
            	return null;
            }

            jsonKeyValue = jsonKeyValue.getJSONObject(PUBLIC_KEY);
            if (signatureAlgorithm == SignatureAlgorithm.RS256 || signatureAlgorithm == SignatureAlgorithm.RS384 || signatureAlgorithm == SignatureAlgorithm.RS512) {
                String exp = jsonKeyValue.getString(PUBLIC_EXPONENT);
                String mod = jsonKeyValue.getString(PUBLIC_MODULUS);

                BigInteger publicExponent = new BigInteger(1, JwtUtil.base64urldecode(exp));
                BigInteger modulus = new BigInteger(1, JwtUtil.base64urldecode(mod));

                publicKey = new RSAPublicKey(modulus, publicExponent);
            } else if (signatureAlgorithm == SignatureAlgorithm.ES256 || signatureAlgorithm == SignatureAlgorithm.ES384 || signatureAlgorithm == SignatureAlgorithm.ES512) {
                String xx = jsonKeyValue.getString(X);
                String yy = jsonKeyValue.getString(Y);

                BigInteger x = new BigInteger(1, JwtUtil.base64urldecode(xx));
                BigInteger y = new BigInteger(1, JwtUtil.base64urldecode(yy));

                publicKey = new ECDSAPublicKey(signatureAlgorithm, x, y);
            }

            if (publicKey != null && jsonKeyValue.has(X5C)) {
                final String BEGIN = "-----BEGIN CERTIFICATE-----";
                final String END = "-----END CERTIFICATE-----";

                JSONArray certChain = jsonKeyValue.getJSONArray(X5C);
                String certificateString = BEGIN + "\n" + certChain.getString(0) + "\n" + END;
                StringReader sr = new StringReader(certificateString);
                PEMReader pemReader = new PEMReader(sr);
                try {
					X509Certificate cert = (X509CertificateObject) pemReader.readObject();
					Certificate certificate = new Certificate(signatureAlgorithm, cert);
					publicKey.setCertificate(certificate);
				} finally {
	                pemReader.close();
				}
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return publicKey;
    }

    public static org.xdi.oxauth.model.crypto.PrivateKey getPrivateKey(String jwksUri, String jwks, String keyId) {
        log.debug("Retrieving JWK Private Key...");

        JSONObject jsonKeyValue = getJsonKey(jwksUri, jwks, keyId);
        if (jsonKeyValue == null) {
            return null;
        }
        
        org.xdi.oxauth.model.crypto.PrivateKey privateKey = null;

        try {
        	SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromName(jsonKeyValue.getString(JWKS_ALGORITHM));
            String resultKeyId = jsonKeyValue.getString(JWKS_KEY_ID);
            if (signatureAlgorithm == null) {
                log.error(String.format("Failed to determine key '%s' signature algorithm", resultKeyId));
            	return null;
            }

            JSONObject jsonPrivateKey = jsonKeyValue.getJSONObject(PRIVATE_KEY);
            if (signatureAlgorithm == SignatureAlgorithm.RS256 || signatureAlgorithm == SignatureAlgorithm.RS384 || signatureAlgorithm == SignatureAlgorithm.RS512) {
                String exp = jsonPrivateKey.getString(PRIVATE_EXPONENT);
                String mod = jsonPrivateKey.getString(PRIVATE_MODULUS);

                BigInteger privateExponent = new BigInteger(1, JwtUtil.base64urldecode(exp));
                BigInteger modulus = new BigInteger(1, JwtUtil.base64urldecode(mod));

                privateKey = new RSAPrivateKey(modulus, privateExponent);
            } else if (signatureAlgorithm == SignatureAlgorithm.ES256 || signatureAlgorithm == SignatureAlgorithm.ES384 || signatureAlgorithm == SignatureAlgorithm.ES512) {
                String dd = jsonPrivateKey.getString(D);

                BigInteger d = new BigInteger(1, JwtUtil.base64urldecode(dd));

                privateKey = new ECDSAPrivateKey(d);
            }
            
            if (privateKey != null) {
                privateKey.setSignatureAlgorithm(signatureAlgorithm);
                privateKey.setKeyId(resultKeyId);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        return privateKey;
    }

}
