/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto;

import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.EDDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.util.StringUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.StringWriter;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Certificate, uses RSA, EcDSA, EdDSA.
 * 
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public class Certificate {

    private final SignatureAlgorithm signatureAlgorithm;
    private final X509Certificate x509Certificate;

    /**
     * Constructor.
     * 
     * @param signatureAlgorithm Signature algorithm (RS256, RS384, RS512, ES256, ES256K, ES384, ES512, PS256, PS384, PS512, EDDSA/Ed25519). 
     * @param x509Certificate X509 certificate.
     */
    public Certificate(SignatureAlgorithm signatureAlgorithm, X509Certificate x509Certificate) {
        this.signatureAlgorithm = signatureAlgorithm;
        this.x509Certificate = x509Certificate;
    }

    /**
     * Returns Public Key from X509 Certificate.
     * 
     * @return Public Key from X509 Certificate.
     */
    public PublicKey getPublicKey() {
        if(x509Certificate == null) {
            return null; 
        }
        PublicKey publicKey = null;
        if (x509Certificate.getPublicKey() instanceof BCRSAPublicKey) {
            BCRSAPublicKey jcersaPublicKey = (BCRSAPublicKey) x509Certificate.getPublicKey();

            publicKey = new RSAPublicKey(jcersaPublicKey.getModulus(), jcersaPublicKey.getPublicExponent());
        } else if (x509Certificate.getPublicKey() instanceof BCECPublicKey) {
            BCECPublicKey jceecPublicKey = (BCECPublicKey) x509Certificate.getPublicKey();

            publicKey = new ECDSAPublicKey(signatureAlgorithm, jceecPublicKey.getQ().getXCoord().toBigInteger(),
                    jceecPublicKey.getQ().getYCoord().toBigInteger());
        } else if (x509Certificate.getPublicKey() instanceof BCEdDSAPublicKey) {
            BCEdDSAPublicKey jceedPublicKey = (BCEdDSAPublicKey) x509Certificate.getPublicKey();

            publicKey = new EDDSAPublicKey(signatureAlgorithm, jceedPublicKey.getEncoded());
        }
        return publicKey;
    }

    /**
     * Returns RSA Public Key from X509 Certificate.
     * 
     * @return RSA Public Key from X509 Certificate.
     */
    public RSAPublicKey getRsaPublicKey() {
        if(x509Certificate == null) {
            return null;
        }
        RSAPublicKey rsaPublicKey = null;
        if (x509Certificate.getPublicKey() instanceof BCRSAPublicKey) {
            BCRSAPublicKey publicKey = (BCRSAPublicKey) x509Certificate.getPublicKey();
            rsaPublicKey = new RSAPublicKey(publicKey.getModulus(), publicKey.getPublicExponent());
        } else if (x509Certificate.getPublicKey() instanceof java.security.interfaces.RSAPublicKey) {
            java.security.interfaces.RSAPublicKey publicKey = (java.security.interfaces.RSAPublicKey) x509Certificate
                    .getPublicKey();
            rsaPublicKey = new RSAPublicKey(publicKey.getModulus(), publicKey.getPublicExponent());
        }
        return rsaPublicKey;
    }

    /**
     * Returns ECDSA Public Key from X509 Certificate.
     * 
     * @return ECDSA Public Key from X509 Certificate.
     */
    public ECDSAPublicKey getEcdsaPublicKey() {
        if(x509Certificate == null) {
            return null;
        }
        ECDSAPublicKey ecdsaPublicKey = null;
        if (x509Certificate.getPublicKey() instanceof BCECPublicKey) {
            BCECPublicKey publicKey = (BCECPublicKey) x509Certificate.getPublicKey();
            ecdsaPublicKey = new ECDSAPublicKey(signatureAlgorithm, publicKey.getQ().getXCoord().toBigInteger(),
                    publicKey.getQ().getYCoord().toBigInteger());
        } else if (x509Certificate.getPublicKey() instanceof java.security.interfaces.ECPublicKey) {
            java.security.interfaces.ECPublicKey publicKey = (java.security.interfaces.ECPublicKey) x509Certificate
                    .getPublicKey();
            ecdsaPublicKey = new ECDSAPublicKey(signatureAlgorithm, publicKey.getW().getAffineX(),
                    publicKey.getW().getAffineY());
        }
        return ecdsaPublicKey;
    }

    /**
     * Returns EDDSA Public Key from X509 Certificate.
     *
     * @return EDDSA Public Key from X509 Certificate.
     */
    public EDDSAPublicKey getEddsaPublicKey() {
        EDDSAPublicKey eddsaPublicKey = null;
        if (x509Certificate != null && x509Certificate.getPublicKey() instanceof BCEdDSAPublicKey) {
            BCEdDSAPublicKey publicKey = (BCEdDSAPublicKey) x509Certificate.getPublicKey();
            eddsaPublicKey = new EDDSAPublicKey(signatureAlgorithm, publicKey.getEncoded());
        }
        return eddsaPublicKey;
    }

    public JSONArray toJSONArray() throws JSONException {
        String cert = toString();

        cert = cert.replace("\n", "");
        cert = cert.replace("-----BEGIN CERTIFICATE-----", "");
        cert = cert.replace("-----END CERTIFICATE-----", "");

        return new JSONArray(Arrays.asList(cert));
    }

    @Override
    public String toString() {
        try {
            StringWriter stringWriter = new StringWriter();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
                pemWriter.writeObject(x509Certificate);
                pemWriter.flush();
                return stringWriter.toString();
            }
        } catch (Exception e) {
            return StringUtils.EMPTY_STRING;
        }
    }

}