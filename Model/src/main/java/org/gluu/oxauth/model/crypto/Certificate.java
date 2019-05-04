/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.crypto;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.gluu.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * @author Javier Rojas Blum
 * @version June 29, 2016
 */
public class Certificate {

    private SignatureAlgorithm signatureAlgorithm;
    private X509Certificate x509Certificate;

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

            publicKey = new ECDSAPublicKey(signatureAlgorithm, jceecPublicKey.getQ().getX().toBigInteger(),
                    jceecPublicKey.getQ().getY().toBigInteger());
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

            ecdsaPublicKey = new ECDSAPublicKey(signatureAlgorithm, publicKey.getQ().getX().toBigInteger(),
                    publicKey.getQ().getY().toBigInteger());
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
            JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter);
            try {
                pemWriter.writeObject(x509Certificate);
                pemWriter.flush();
                return stringWriter.toString();
            } finally {
                pemWriter.close();
            }
        } catch (IOException e) {
            return StringUtils.EMPTY_STRING;
        } catch (Exception e) {
            return StringUtils.EMPTY_STRING;
        }
    }
}