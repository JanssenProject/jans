/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto;

import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.util.StringUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.StringWriter;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * @author Javier Rojas Blum
 * @version June 29, 2016
 */
public class Certificate {

    private final SignatureAlgorithm signatureAlgorithm;
    private final X509Certificate x509Certificate;

    public Certificate(SignatureAlgorithm signatureAlgorithm, X509Certificate x509Certificate) {
        this.signatureAlgorithm = signatureAlgorithm;
        this.x509Certificate = x509Certificate;
    }

    public PublicKey getPublicKey() {
        PublicKey publicKey = null;

        if (x509Certificate != null && x509Certificate.getPublicKey() instanceof BCRSAPublicKey) {
            BCRSAPublicKey jcersaPublicKey = (BCRSAPublicKey) x509Certificate.getPublicKey();

            publicKey = new RSAPublicKey(jcersaPublicKey.getModulus(), jcersaPublicKey.getPublicExponent());
        } else if (x509Certificate != null && x509Certificate.getPublicKey() instanceof BCECPublicKey) {
            BCECPublicKey jceecPublicKey = (BCECPublicKey) x509Certificate.getPublicKey();

            publicKey = new ECDSAPublicKey(signatureAlgorithm, jceecPublicKey.getQ().getXCoord().toBigInteger(),
                    jceecPublicKey.getQ().getYCoord().toBigInteger());
        }

        return publicKey;
    }

    public RSAPublicKey getRsaPublicKey() {
        RSAPublicKey rsaPublicKey = null;

        if (x509Certificate != null && x509Certificate.getPublicKey() instanceof BCRSAPublicKey) {
            BCRSAPublicKey publicKey = (BCRSAPublicKey) x509Certificate.getPublicKey();

            rsaPublicKey = new RSAPublicKey(publicKey.getModulus(), publicKey.getPublicExponent());
        }

        return rsaPublicKey;
    }

    public ECDSAPublicKey getEcdsaPublicKey() {
        ECDSAPublicKey ecdsaPublicKey = null;

        if (x509Certificate != null && x509Certificate.getPublicKey() instanceof BCECPublicKey) {
            BCECPublicKey publicKey = (BCECPublicKey) x509Certificate.getPublicKey();

            ecdsaPublicKey = new ECDSAPublicKey(signatureAlgorithm, publicKey.getQ().getXCoord().toBigInteger(),
                    publicKey.getQ().getYCoord().toBigInteger());
        }

        return ecdsaPublicKey;
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