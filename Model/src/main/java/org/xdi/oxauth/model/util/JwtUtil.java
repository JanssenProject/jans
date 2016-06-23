/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.util;

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
import org.bouncycastle.openssl.PEMParser;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.xdi.oxauth.model.crypto.Certificate;
import org.xdi.oxauth.model.crypto.signature.*;
import org.xdi.util.StringHelper;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Set;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version June 15, 2016
 */
public class JwtUtil {

    private static final Logger log = Logger.getLogger(JwtUtil.class);

    public static String base64urlencodeUnsignedBigInt(BigInteger bigInteger) {
        byte[] array = bigInteger.toByteArray();
        if (array[0] == 0) {
            byte[] tmp = new byte[array.length - 1];
            System.arraycopy(array, 1, tmp, 0, tmp.length);
            array = tmp;
        }

        return JwtUtil.base64urlencode(array);
    }

    public static byte[] unsignedToBytes(int[] plaintextUnsignedBytes) {
        byte[] bytes = new byte[plaintextUnsignedBytes.length];

        for (int i = 0; i < plaintextUnsignedBytes.length; i++) {
            bytes[i] = (byte) plaintextUnsignedBytes[i];
        }

        return bytes;
    }

    @Deprecated
    public static String encodeJwt(JSONObject jsonHeader, JSONObject jsonClaim,
                                   SignatureAlgorithm algorithm) {
        if (jsonHeader != null && jsonClaim != null && algorithm == SignatureAlgorithm.NONE) {
            return encodeJwt(jsonHeader, jsonClaim, algorithm, null, null, null);
        }
        return null;
    }

    @Deprecated
    public static String encodeJwt(JSONObject jsonHeader, JSONObject jsonClaim,
                                   SignatureAlgorithm algorithm, String sharedKey) {
        if (jsonHeader != null && jsonClaim != null && algorithm != null && sharedKey != null) {
            return encodeJwt(jsonHeader, jsonClaim, algorithm, sharedKey, null, null);
        }
        return null;
    }

    @Deprecated
    public static String encodeJwt(JSONObject jsonHeader, JSONObject jsonClaim,
                                   SignatureAlgorithm algorithm, RSAPrivateKey privateKey) {
        if (jsonHeader != null && jsonClaim != null && algorithm != null && privateKey != null) {
            return encodeJwt(jsonHeader, jsonClaim, algorithm, null, privateKey, null);
        }
        return null;
    }

    @Deprecated
    public static String encodeJwt(JSONObject jsonHeader, JSONObject jsonClaim,
                                   SignatureAlgorithm algorithm, ECDSAPrivateKey privateKey) {
        if (jsonHeader != null && jsonClaim != null && algorithm != null && privateKey != null) {
            return encodeJwt(jsonHeader, jsonClaim, algorithm, null, null, privateKey);
        }
        return null;
    }

    @Deprecated
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

    @Deprecated
    public static byte[] getMessageDigestSHA256(String data)
            throws NoSuchProviderException, NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest mda = MessageDigest.getInstance("SHA-256", "BC");
        return mda.digest(data.getBytes(Util.UTF8_STRING_ENCODING));
    }

    @Deprecated
    public static byte[] getMessageDigestSHA384(String data)
            throws NoSuchProviderException, NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest mda = MessageDigest.getInstance("SHA-384", "BC");
        return mda.digest(data.getBytes(Util.UTF8_STRING_ENCODING));
    }

    @Deprecated
    public static byte[] getMessageDigestSHA512(String data)
            throws NoSuchProviderException, NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest mda = MessageDigest.getInstance("SHA-512", "BC");
        return mda.digest(data.getBytes(Util.UTF8_STRING_ENCODING));
    }

    @Deprecated
    public static byte[] getSignatureHS256(byte[] signingInput, byte[] key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKey secretKey = new SecretKeySpec(key, "HMACSHA256");
        Mac mac = Mac.getInstance("HMACSHA256");
        mac.init(secretKey);
        return mac.doFinal(signingInput);
    }

    @Deprecated
    public static byte[] getSignatureHS384(byte[] signingInput, byte[] key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKey secretKey = new SecretKeySpec(key, "HMACSHA384");
        Mac mac = Mac.getInstance("HMACSHA384");
        mac.init(secretKey);
        return mac.doFinal(signingInput);
    }

    @Deprecated
    public static byte[] getSignatureHS512(byte[] signingInput, byte[] key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKey secretKey = new SecretKeySpec(key, "HMACSHA512");
        Mac mac = Mac.getInstance("HMACSHA512");
        mac.init(secretKey);
        return mac.doFinal(signingInput);
    }

    @Deprecated
    public static KeyPair generateRsaKey() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyGen.initialize(2048, new SecureRandom());

        return keyGen.generateKeyPair();
    }

    @Deprecated
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

    @Deprecated
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

    @Deprecated
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

    @Deprecated
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

    @Deprecated
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

    @Deprecated
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

    @Deprecated
    public static KeyPair generateKeyES256() throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyGen.initialize(ecSpec, new SecureRandom());

        return keyGen.generateKeyPair();
    }

    @Deprecated
    public static KeyPair generateKeyES384() throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-384");

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyGen.initialize(ecSpec, new SecureRandom());

        return keyGen.generateKeyPair();
    }

    @Deprecated
    public static KeyPair generateKeyES512() throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-521");

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyGen.initialize(ecSpec, new SecureRandom());

        return keyGen.generateKeyPair();
    }

    @Deprecated
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

    @Deprecated
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

    @Deprecated
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

    @Deprecated
    public static boolean verifySignatureES256(byte[] signingInput, byte[] sigBytes, ECDSAPublicKey ecdsaPublicKey)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException, SignatureException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");
        BigInteger q = ((org.bouncycastle.math.ec.custom.sec.SecP256R1Curve) ecSpec.getCurve()).getQ();
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

    @Deprecated
    public static boolean verifySignatureES384(byte[] signingInput, byte[] sigBytes, ECDSAPublicKey ecdsaPublicKey)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException, SignatureException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-384");
        BigInteger q = ((org.bouncycastle.math.ec.custom.sec.SecP384R1Curve) ecSpec.getCurve()).getQ();
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

    @Deprecated
    public static boolean verifySignatureES512(byte[] signingInput, byte[] sigBytes, ECDSAPublicKey ecdsaPublicKey)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException, SignatureException {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-521");
        BigInteger q = ((org.bouncycastle.math.ec.custom.sec.SecP521R1Curve) ecSpec.getCurve()).getQ();
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

    @Deprecated
    public static org.xdi.oxauth.model.crypto.PublicKey getPublicKey(
            String jwksUri, String jwks, String keyId) {
        return getPublicKey(jwksUri, jwks, null, keyId);
    }

    @Deprecated
    public static org.xdi.oxauth.model.crypto.PublicKey getPublicKey(
            String jwksUri, String jwks, SignatureAlgorithm signatureAlgorithm, String keyId) {
        log.debug("Retrieving JWK...");

        JSONObject jsonKeyValue = getJsonKey(jwksUri, jwks, keyId);

        if (jsonKeyValue == null) {
            return null;
        }

        org.xdi.oxauth.model.crypto.PublicKey publicKey = null;

        try {
            String resultKeyId = jsonKeyValue.getString(KEY_ID);
            if (signatureAlgorithm == null) {
                signatureAlgorithm = SignatureAlgorithm.fromString(jsonKeyValue.getString(ALGORITHM));
                if (signatureAlgorithm == null) {
                    log.error(String.format("Failed to determine key '%s' signature algorithm", resultKeyId));
                    return null;
                }
            }

            JSONObject jsonPublicKey = jsonKeyValue;
            if (jsonKeyValue.has(PUBLIC_KEY)) {
                // Use internal jwks.json format
                jsonPublicKey = jsonKeyValue.getJSONObject(PUBLIC_KEY);
            }

            if (signatureAlgorithm == SignatureAlgorithm.RS256 || signatureAlgorithm == SignatureAlgorithm.RS384 || signatureAlgorithm == SignatureAlgorithm.RS512) {
                //String alg = jsonKeyValue.getString(ALGORITHM);
                //String use = jsonKeyValue.getString(KEY_USE);
                String exp = jsonPublicKey.getString(EXPONENT);
                String mod = jsonPublicKey.getString(MODULUS);

                BigInteger publicExponent = new BigInteger(1, JwtUtil.base64urldecode(exp));
                BigInteger modulus = new BigInteger(1, JwtUtil.base64urldecode(mod));

                publicKey = new RSAPublicKey(modulus, publicExponent);
            } else if (signatureAlgorithm == SignatureAlgorithm.ES256 || signatureAlgorithm == SignatureAlgorithm.ES384 || signatureAlgorithm == SignatureAlgorithm.ES512) {
                //String alg = jsonKeyValue.getString(ALGORITHM);
                //String use = jsonKeyValue.getString(KEY_USE);
                //String crv = jsonKeyValue.getString(CURVE);
                String xx = jsonPublicKey.getString(X);
                String yy = jsonPublicKey.getString(Y);

                BigInteger x = new BigInteger(1, JwtUtil.base64urldecode(xx));
                BigInteger y = new BigInteger(1, JwtUtil.base64urldecode(yy));

                publicKey = new ECDSAPublicKey(signatureAlgorithm, x, y);
            }

            if (publicKey != null && jsonKeyValue.has(CERTIFICATE_CHAIN)) {
                final String BEGIN = "-----BEGIN CERTIFICATE-----";
                final String END = "-----END CERTIFICATE-----";

                JSONArray certChain = jsonKeyValue.getJSONArray(CERTIFICATE_CHAIN);
                String certificateString = BEGIN + "\n" + certChain.getString(0) + "\n" + END;
                StringReader sr = new StringReader(certificateString);
                PEMParser pemReader = new PEMParser(sr);
                X509Certificate cert = (X509CertificateObject) pemReader.readObject();
                Certificate certificate = new Certificate(signatureAlgorithm, cert);
                publicKey.setCertificate(certificate);
            }
            if (publicKey != null) {
                publicKey.setKeyId(resultKeyId);
                publicKey.setSignatureAlgorithm(signatureAlgorithm);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        return publicKey;
    }

    @Deprecated
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
                            if (kv.getString(KEY_ID).equals(keyId)) {
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

    public static JSONObject getJSONWebKeys(String jwksUri) {
        log.debug("Retrieving jwks...");

        JSONObject jwks = null;
        try {
            if (!StringHelper.isEmpty(jwksUri)) {
                ClientRequest clientRequest = new ClientRequest(jwksUri);
                clientRequest.setHttpMethod(HttpMethod.GET);
                ClientResponse<String> clientResponse = clientRequest.get(String.class);

                int status = clientResponse.getStatus();
                log.debug(String.format("Status: %n%d", status));

                if (status == 200) {
                    jwks = new JSONObject(clientResponse.getEntity(String.class));
                    log.debug(String.format("JWK: %s", jwks));
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        return jwks;
    }
}