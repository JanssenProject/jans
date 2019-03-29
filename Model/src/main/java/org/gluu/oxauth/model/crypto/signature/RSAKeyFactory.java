/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.crypto.signature;

import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateCrtKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.gluu.oxauth.model.crypto.Certificate;
import org.gluu.oxauth.model.crypto.KeyFactory;
import org.gluu.oxauth.model.jwk.JSONWebKey;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

/**
 * Factory to create asymmetric Public and Private Keys for the RSA algorithm
 *
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
@Deprecated
public class RSAKeyFactory extends KeyFactory<RSAPrivateKey, RSAPublicKey> {

    private RSAPrivateKey rsaPrivateKey;
    private RSAPublicKey rsaPublicKey;
    private Certificate certificate;

    @Deprecated
    public RSAKeyFactory(SignatureAlgorithm signatureAlgorithm, String dnName)
            throws InvalidParameterException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException,
            InvalidKeyException, CertificateEncodingException {
        if (signatureAlgorithm == null) {
            throw new InvalidParameterException("The signature algorithm cannot be null");
        }

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyGen.initialize(2048, new SecureRandom());

        KeyPair keyPair = keyGen.generateKeyPair();

        BCRSAPrivateCrtKey jcersaPrivateCrtKey = (BCRSAPrivateCrtKey) keyPair.getPrivate();
        BCRSAPublicKey jcersaPublicKey = (BCRSAPublicKey) keyPair.getPublic();

        rsaPrivateKey = new RSAPrivateKey(jcersaPrivateCrtKey.getModulus(),
                jcersaPrivateCrtKey.getPrivateExponent());

        rsaPublicKey = new RSAPublicKey(jcersaPublicKey.getModulus(),
                jcersaPublicKey.getPublicExponent());

        if (StringUtils.isNotBlank(dnName)) {
            // Create certificate
            GregorianCalendar startDate = new GregorianCalendar(); // time from which certificate is valid
            GregorianCalendar expiryDate = new GregorianCalendar(); // time after which certificate is not valid
            expiryDate.add(Calendar.YEAR, 1);
            BigInteger serialNumber = new BigInteger(1024, new Random()); // serial number for certificate

            X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
            X500Principal principal = new X500Principal(dnName);

            certGen.setSerialNumber(serialNumber);
            certGen.setIssuerDN(principal);
            certGen.setNotBefore(startDate.getTime());
            certGen.setNotAfter(expiryDate.getTime());
            certGen.setSubjectDN(principal); // note: same as issuer
            certGen.setPublicKey(keyPair.getPublic());
            certGen.setSignatureAlgorithm(signatureAlgorithm.getAlgorithm());

            X509Certificate x509Certificate = certGen.generate(jcersaPrivateCrtKey, "BC");
            certificate = new Certificate(signatureAlgorithm, x509Certificate);
        }
    }

    @Deprecated
    public RSAKeyFactory(JSONWebKey p_key) {
        if (p_key == null) {
            throw new IllegalArgumentException("Key value must not be null.");
        }

        rsaPrivateKey = new RSAPrivateKey(
                p_key.getN(),
                p_key.getE());
        rsaPublicKey = new RSAPublicKey(
                p_key.getN(),
                p_key.getE());
        certificate = null;
    }

    public static RSAKeyFactory valueOf(JSONWebKey p_key) {
        return new RSAKeyFactory(p_key);
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
        return rsaPrivateKey;
    }

    @Override
    public RSAPublicKey getPublicKey() {
        return rsaPublicKey;
    }

    @Override
    public Certificate getCertificate() {
        return certificate;
    }
}