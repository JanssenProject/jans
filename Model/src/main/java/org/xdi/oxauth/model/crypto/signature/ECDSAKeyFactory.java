package org.xdi.oxauth.model.crypto.signature;

import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.JCEECPrivateKey;
import org.bouncycastle.jce.provider.JCEECPublicKey;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.xdi.oxauth.model.crypto.Certificate;
import org.xdi.oxauth.model.crypto.KeyFactory;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

/**
 * Factory to create asymmetric Public and Private Keys for the Elliptic Curve Digital Signature Algorithm (ECDSA)
 *
 * @author Javier Rojas Blum Date: 10.22.2012
 */
public class ECDSAKeyFactory extends KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> {

    private ECDSAPrivateKey ecdsaPrivateKey;
    private ECDSAPublicKey ecdsaPublicKey;
    private Certificate certificate;

    public ECDSAKeyFactory(SignatureAlgorithm signatureAlgorithm, String dnName)
            throws InvalidParameterException, NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, SignatureException, InvalidKeyException, CertificateEncodingException {
        if (signatureAlgorithm == null) {
            throw new InvalidParameterException("The signature algorithm cannot be null");
        }

        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(signatureAlgorithm.getCurve());

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyGen.initialize(ecSpec, new SecureRandom());

        KeyPair keyPair = keyGen.generateKeyPair();
        JCEECPrivateKey privateKeySpec = (JCEECPrivateKey) keyPair.getPrivate();
        JCEECPublicKey publicKeySpec = (JCEECPublicKey) keyPair.getPublic();

        BigInteger x = publicKeySpec.getQ().getX().toBigInteger();
        BigInteger y = publicKeySpec.getQ().getY().toBigInteger();
        BigInteger d = privateKeySpec.getD();

        ecdsaPrivateKey = new ECDSAPrivateKey(d);
        ecdsaPublicKey = new ECDSAPublicKey(signatureAlgorithm, x, y);

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
            certGen.setSignatureAlgorithm("SHA256WITHECDSA");

            X509Certificate x509Certificate = certGen.generate(privateKeySpec, "BC");
            certificate = new Certificate(signatureAlgorithm, x509Certificate);
        }
    }

    @Override
    public ECDSAPrivateKey getPrivateKey() {
        return ecdsaPrivateKey;
    }

    @Override
    public ECDSAPublicKey getPublicKey() {
        return ecdsaPublicKey;
    }

    @Override
    public Certificate getCertificate() {
        return certificate;
    }
}