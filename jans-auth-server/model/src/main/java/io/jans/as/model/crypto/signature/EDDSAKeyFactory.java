/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */
package io.jans.as.model.crypto.signature;

import io.jans.as.model.crypto.Certificate;
import io.jans.as.model.crypto.KeyFactory;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.bouncycastle.jcajce.spec.EdDSAParameterSpec;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Factory to create asymmetric Public and Private Keys for the Edwards Curve
 * Digital Signature Algorithm (EDDSA)
 *
 * @author Sergey Manoylo
 * @version July 23, 2021
 */
public class EDDSAKeyFactory extends KeyFactory<EDDSAPrivateKey, EDDSAPublicKey> {

    protected static final byte[] Ed448Prefix = Hex.decode("3043300506032b6571033a00");
    protected static final byte[] Ed25519Prefix = Hex.decode("302a300506032b6570032100");

    private final SignatureAlgorithm signatureAlgorithm;

    private final KeyPair keyPair;
    private final EDDSAPrivateKey eddsaPrivateKey;
    private final EDDSAPublicKey eddsaPublicKey;
    private Certificate certificate;

    /**
     * Constructor
     *
     * @param signatureAlgorithm
     * @param dnName
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws OperatorCreationException
     * @throws CertificateException
     */
    public EDDSAKeyFactory(final SignatureAlgorithm signatureAlgorithm, final String dnName) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, OperatorCreationException, CertificateException {
        if (signatureAlgorithm == null) {
            throw new InvalidParameterException("The signature algorithm cannot be null");
        }
        if (!AlgorithmFamily.ED.equals(signatureAlgorithm.getFamily())) {
            throw new InvalidParameterException("Wrong value of the family of the SignatureAlgorithm");
        }
        this.signatureAlgorithm = signatureAlgorithm;

        EdDSAParameterSpec edSpec = new EdDSAParameterSpec(signatureAlgorithm.getCurve().getName());

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(signatureAlgorithm.getName(), DEF_BC);
        keyGen.initialize(edSpec, new SecureRandom());

        this.keyPair = keyGen.generateKeyPair();

        BCEdDSAPrivateKey privateKey = (BCEdDSAPrivateKey) keyPair.getPrivate();
        BCEdDSAPublicKey publicKey = (BCEdDSAPublicKey) keyPair.getPublic();

        byte[] privateKeyData = privateKey.getEncoded();
        byte[] publicKeyData = publicKey.getEncoded();

        this.eddsaPrivateKey = new EDDSAPrivateKey(signatureAlgorithm, privateKeyData, publicKeyData);
        this.eddsaPublicKey = new EDDSAPublicKey(signatureAlgorithm, publicKeyData);

        if (StringUtils.isNotBlank(dnName)) {
            // Create certificate
            GregorianCalendar startDate = new GregorianCalendar(); // time from which certificate is valid
            GregorianCalendar expiryDate = new GregorianCalendar(); // time after which certificate is not valid
            expiryDate.add(Calendar.YEAR, 1);
            BigInteger serialNumber = new BigInteger(1024, new SecureRandom()); // serial number for certificate
            X500Name name = new X500Name(dnName);
            JcaX509v1CertificateBuilder certGen = new JcaX509v1CertificateBuilder(name, serialNumber,
                    startDate.getTime(), expiryDate.getTime(), name, publicKey);
            X509CertificateHolder certHolder = certGen
                    .build(new JcaContentSignerBuilder(signatureAlgorithm.getAlgorithm()).setProvider(DEF_BC)
                            .build(keyPair.getPrivate()));
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(DEF_BC).getCertificate(certHolder);
            this.certificate = new Certificate(signatureAlgorithm, cert);
        }
    }

    /**
     * Generates certificate X509 v3
     *
     * @param startDate
     * @param expirationDate
     * @param dnName
     * @return
     * @throws CertificateEncodingException
     * @throws InvalidKeyException
     * @throws IllegalStateException
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     */
    public Certificate generateV3Certificate(final Date startDate, final Date expirationDate, final String dnName) throws SignatureException {
        // Creating the certificate
        Certificate resCertificate = null;
        try {
            BCEdDSAPublicKey publicKey = (BCEdDSAPublicKey) keyPair.getPublic();
            BigInteger serialNumber = new BigInteger(1024, new SecureRandom()); // serial number for certificate
            X500Name name = new X500Name(dnName);
            JcaX509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(name, serialNumber, startDate,
                    expirationDate, name, publicKey);
            X509CertificateHolder certHolder = certGen
                    .build(new JcaContentSignerBuilder(signatureAlgorithm.getAlgorithm()).setProvider(DEF_BC)
                            .build(keyPair.getPrivate()));
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(DEF_BC).getCertificate(certHolder);
            resCertificate = new Certificate(signatureAlgorithm, cert);
        } catch (Exception e) {
            throw new SignatureException(e);
        }
        return resCertificate;
    }

    /**
     * Returns EDDSA Private Key
     */
    @Override
    public EDDSAPrivateKey getPrivateKey() {
        return this.eddsaPrivateKey;
    }

    /**
     * Returns EDDSA Public Key
     */
    @Override
    public EDDSAPublicKey getPublicKey() {
        return this.eddsaPublicKey;
    }

    /**
     * Returns X509 Certificate
     */
    @Override
    public Certificate getCertificate() {
        return this.certificate;
    }

    /**
     * Creates EDDSA public key from decoded array
     *
     * @param signatureAlgorithm
     * @param decodedPublicKey
     * @return
     * @throws SignatureException
     */
    public static EDDSAPublicKey createEDDSAPublicKeyFromDecodedKey(final SignatureAlgorithm signatureAlgorithm, final byte[] decodedPublicKey) throws SignatureException {
        byte[] encodedPubKey = getEncodedPubKey(signatureAlgorithm, decodedPublicKey);
        return new EDDSAPublicKey(signatureAlgorithm, encodedPubKey);
    }

    /**
     * Creates EDDSA private key from decoded array
     *
     * @param signatureAlgorithm
     * @param decodedPrivateKey
     * @param decodedPublicKey
     * @return
     * @throws SignatureException
     * @throws IOException
     */
    public static EDDSAPrivateKey createEDDSAPrivateKeyFromDecodedKey(final SignatureAlgorithm signatureAlgorithm, final byte[] decodedPrivateKey, final byte[] decodedPublicKey) throws SignatureException, IOException {
        byte[] encodedPubKey = getEncodedPubKey(signatureAlgorithm, decodedPublicKey);
        Ed25519PrivateKeyParameters privKeysParams = new Ed25519PrivateKeyParameters(decodedPrivateKey);
        PrivateKeyInfo privKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(privKeysParams, null);
        return new EDDSAPrivateKey(signatureAlgorithm, privKeyInfo.getEncoded(), encodedPubKey);
    }

    /**
     * Returns encoded EDDSA public key (from decoded public key)
     *
     * @param signatureAlgorithm
     * @param decodedPublicKey
     * @return
     * @throws SignatureException
     */
    private static byte[] getEncodedPubKey(final SignatureAlgorithm signatureAlgorithm, final byte[] decodedPublicKey) throws SignatureException {
        byte[] encodedPubKey = null;
        if (signatureAlgorithm == SignatureAlgorithm.EDDSA) {
            encodedPubKey = new byte[Ed25519Prefix.length + Ed25519PublicKeyParameters.KEY_SIZE];
            System.arraycopy(Ed25519Prefix, 0, encodedPubKey, 0, Ed25519Prefix.length);
            System.arraycopy(decodedPublicKey, 0, encodedPubKey, Ed25519Prefix.length, decodedPublicKey.length);
        }
        else {
            throw new SignatureException(String.format("Wrong type of the signature algorithm (SignatureAlgorithm): %s", signatureAlgorithm.toString()));            
        }
        return encodedPubKey;
    }

}
