/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto;

import static org.xdi.oxauth.model.jwk.JWKParameter.ALGORITHM;
import static org.xdi.oxauth.model.jwk.JWKParameter.CURVE;
import static org.xdi.oxauth.model.jwk.JWKParameter.EXPIRATION_TIME;
import static org.xdi.oxauth.model.jwk.JWKParameter.EXPONENT;
import static org.xdi.oxauth.model.jwk.JWKParameter.JSON_WEB_KEY_SET;
import static org.xdi.oxauth.model.jwk.JWKParameter.KEY_ID;
import static org.xdi.oxauth.model.jwk.JWKParameter.KEY_TYPE;
import static org.xdi.oxauth.model.jwk.JWKParameter.KEY_USE;
import static org.xdi.oxauth.model.jwk.JWKParameter.MODULUS;
import static org.xdi.oxauth.model.jwk.JWKParameter.PUBLIC_KEY;
import static org.xdi.oxauth.model.jwk.JWKParameter.X;
import static org.xdi.oxauth.model.jwk.JWKParameter.Y;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
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
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.Extension;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithmFamily;
import org.xdi.oxauth.model.jwk.Use;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;
import org.xdi.util.StringHelper;

import sun.security.rsa.RSAPublicKeyImpl;
import org.bouncycastle.asn1.x509.KeyPurposeId;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version June 21, 2016
 */
public class OxAuthCryptoProvider extends AbstractCryptoProvider {

    private static final Logger LOG = Logger.getLogger(OxAuthCryptoProvider.class);

    private KeyStore keyStore;
    private String keyStoreFile;
    private String keyStoreSecret;
    private String dnName;

    public OxAuthCryptoProvider() throws Exception {
        this(null, null, null);
    }

    public OxAuthCryptoProvider(String keyStoreFile, String keyStoreSecret, String dnName) throws Exception {
        if (!Util.isNullOrEmpty(keyStoreFile) && !Util.isNullOrEmpty(keyStoreSecret) /* && !Util.isNullOrEmpty(dnName) */) {
            this.keyStoreFile = keyStoreFile;
            this.keyStoreSecret = keyStoreSecret;
            this.dnName = dnName;

            keyStore = KeyStore.getInstance("JKS");
            try {
                File f = new File(keyStoreFile);
                if (!f.exists()) {
                    keyStore.load(null, keyStoreSecret.toCharArray());
                    FileOutputStream fos = new FileOutputStream(keyStoreFile);
                    keyStore.store(fos, keyStoreSecret.toCharArray());
                    fos.close();
                }
                final InputStream is = new FileInputStream(keyStoreFile);
                keyStore.load(is, keyStoreSecret.toCharArray());
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public JSONObject generateKey(SignatureAlgorithm signatureAlgorithm, Long expirationTime) throws Exception {

        KeyPairGenerator keyGen = null;

        if (signatureAlgorithm == null) {
            throw new RuntimeException("The signature algorithm parameter cannot be null");
        } else if (SignatureAlgorithmFamily.RSA.equals(signatureAlgorithm.getFamily())) {
            keyGen = KeyPairGenerator.getInstance(signatureAlgorithm.getFamily());
            keyGen.initialize(2048, new SecureRandom());
        } else if (SignatureAlgorithmFamily.EC.equals(signatureAlgorithm.getFamily())) {
            ECGenParameterSpec eccgen = new ECGenParameterSpec(signatureAlgorithm.getCurve().getAlias());
            keyGen = KeyPairGenerator.getInstance(signatureAlgorithm.getFamily());
            keyGen.initialize(eccgen, new SecureRandom());
        } else {
            throw new RuntimeException("The provided signature algorithm parameter is not supported");
        }

        // Generate the key
        KeyPair keyPair = keyGen.generateKeyPair();
        java.security.PrivateKey pk = keyPair.getPrivate();

        // Java API requires a certificate chain
        X509Certificate cert = generateV3Certificate(keyPair, dnName, signatureAlgorithm.getAlgorithm(), expirationTime);
        X509Certificate[] chain = new X509Certificate[1];
        chain[0] = cert;

        String alias = UUID.randomUUID().toString();

        keyStore.setKeyEntry(alias, pk, keyStoreSecret.toCharArray(), chain);
        FileOutputStream stream = new FileOutputStream(keyStoreFile);
        keyStore.store(stream, keyStoreSecret.toCharArray());

        PublicKey publicKey = keyPair.getPublic();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_TYPE, signatureAlgorithm.getFamily());
        jsonObject.put(KEY_ID, alias);
        jsonObject.put(KEY_USE, Use.SIGNATURE);
        jsonObject.put(ALGORITHM, signatureAlgorithm.getName());
        jsonObject.put(EXPIRATION_TIME, expirationTime);
        if (publicKey instanceof RSAPublicKey) {
            RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
            jsonObject.put(MODULUS, JwtUtil.base64urlencodeUnsignedBigInt(rsaPublicKey.getModulus()));
            jsonObject.put(EXPONENT, JwtUtil.base64urlencodeUnsignedBigInt(rsaPublicKey.getPublicExponent()));
        } else if (publicKey instanceof ECPublicKey) {
            ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
            jsonObject.put(CURVE, signatureAlgorithm.getCurve());
            jsonObject.put(X, JwtUtil.base64urlencodeUnsignedBigInt(ecPublicKey.getW().getAffineX()));
            jsonObject.put(Y, JwtUtil.base64urlencodeUnsignedBigInt(ecPublicKey.getW().getAffineY()));
        }

        return jsonObject;
    }

    @Override
    public String sign(String signingInput, String alias, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws Exception {
        if (signatureAlgorithm == SignatureAlgorithm.NONE) {
            return "";
        } else if (SignatureAlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily())) {
            SecretKey secretKey = new SecretKeySpec(sharedSecret.getBytes(Util.UTF8_STRING_ENCODING), signatureAlgorithm.getAlgorithm());
            Mac mac = Mac.getInstance(signatureAlgorithm.getAlgorithm());
            mac.init(secretKey);
            byte[] sig = mac.doFinal(signingInput.getBytes());
            return JwtUtil.base64urlencode(sig);
        } else { // EC or RSA
            PrivateKey privateKey = getPrivateKey(alias);

            Signature signature = Signature.getInstance(signatureAlgorithm.getAlgorithm());
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes());

            return JwtUtil.base64urlencode(signature.sign());
        }
    }

    @Override
    public boolean verifySignature(String signingInput, String encodedSignature, String alias, JSONObject jwks, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws Exception {
        boolean verified = false;

        if (signatureAlgorithm == SignatureAlgorithm.NONE) {
            return Util.isNullOrEmpty(encodedSignature);
        } else if (SignatureAlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily())) {
            String expectedSignature = sign(signingInput, null, sharedSecret, signatureAlgorithm);
            return expectedSignature.equals(encodedSignature);
        } else { // EC or RSA
            PublicKey publicKey = null;

            try {
                if (jwks == null) {
                    publicKey = getPublicKey(alias);
                } else {
                    publicKey = getPublicKey(alias, jwks);
                }
                if (publicKey == null) {
                    return false;
                }

                byte[] signature = JwtUtil.base64urldecode(encodedSignature);

                Signature verifier = Signature.getInstance(signatureAlgorithm.getAlgorithm());
                verifier.initVerify(publicKey);
                verifier.update(signingInput.getBytes());
                verified = verifier.verify(signature);
            } catch (NoSuchAlgorithmException e) {
                LOG.error(e.getMessage(), e);
                verified = false;
            } catch (SignatureException e) {
                LOG.error(e.getMessage(), e);
                verified = false;
            } catch (InvalidKeyException e) {
                LOG.error(e.getMessage(), e);
                verified = false;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                verified = false;
            }
        }

        return verified;
    }

	private String getJWKSValue(JSONObject jwks, String node) throws JSONException {
		try {
			return jwks.getString(node);
		} catch (Exception ex) {
			JSONObject publicKey = jwks.getJSONObject(PUBLIC_KEY);
			return publicKey.getString(node);
		}
	}

    @Override
    public boolean deleteKey(String alias) throws Exception {
        keyStore.deleteEntry(alias);
        FileOutputStream stream = new FileOutputStream(keyStoreFile);
        keyStore.store(stream, keyStoreSecret.toCharArray());
        return true;
    }

    public PublicKey getPublicKey(String alias, JSONObject jwks) throws Exception {
        PublicKey publicKey = null;

        JSONArray webKeys = jwks.getJSONArray(JSON_WEB_KEY_SET);
        for (int i = 0; i < webKeys.length(); i++) {
            JSONObject key = webKeys.getJSONObject(i);
            if (alias.equals(key.getString(KEY_ID))) {
                SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(key.getString(ALGORITHM));
                if (signatureAlgorithm != null) {
                    if (signatureAlgorithm.getFamily().equals(SignatureAlgorithmFamily.RSA)) {
                        publicKey = new RSAPublicKeyImpl(
                                new BigInteger(1, JwtUtil.base64urldecode(key.getString(MODULUS))),
                                new BigInteger(1, JwtUtil.base64urldecode(key.getString(EXPONENT))));
                    } else if (signatureAlgorithm.getFamily().equals(SignatureAlgorithmFamily.EC)) {
                        AlgorithmParameters parameters = AlgorithmParameters.getInstance(SignatureAlgorithmFamily.EC);
                        parameters.init(new ECGenParameterSpec(signatureAlgorithm.getCurve().getAlias()));
                        ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);

                        publicKey = KeyFactory.getInstance(SignatureAlgorithmFamily.EC).generatePublic(new ECPublicKeySpec(
                                new ECPoint(
                                        new BigInteger(1, JwtUtil.base64urldecode(key.getString(X))),
                                        new BigInteger(1, JwtUtil.base64urldecode(key.getString(Y)))
                                ), ecParameters));
                    }
                }
            }
        }

        return publicKey;
    }

    public PublicKey getPublicKey(String alias) {
        PublicKey publicKey = null;

        try {
            if (Util.isNullOrEmpty(alias)) {
                return null;
            }

            java.security.cert.Certificate certificate = keyStore.getCertificate(alias);
            if (certificate == null) {
                return null;
            }
            publicKey = certificate.getPublicKey();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        return publicKey;
    }

    private PrivateKey getPrivateKey(String alias)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        if (Util.isNullOrEmpty(alias)) {
            return null;
        }

        Key key = keyStore.getKey(alias, keyStoreSecret.toCharArray());
        if (key == null) {
            return null;
        }
        PrivateKey privateKey = (PrivateKey) key;

        return privateKey;
    }
/*
    private X509Certificate[] generateV3Certificate(KeyPair pair, String dnName, SignatureAlgorithm signatureAlgorithm,
                                                    Long expirationTime)
            throws NoSuchAlgorithmException, CertificateEncodingException, NoSuchProviderException, InvalidKeyException,
            SignatureException {
        X500Principal principal = new X500Principal(dnName);
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

        certGen.setSerialNumber(serialNumber);
        certGen.setIssuerDN(principal);
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 10000));
        certGen.setNotAfter(new Date(expirationTime));
        certGen.setSubjectDN(principal);
        certGen.setPublicKey(pair.getPublic());
        certGen.setSignatureAlgorithm(signatureAlgorithm.getAlgorithm());

        //certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false));
        //certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        //certGen.addExtension(X509Extensions.ExtendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        //certGen.addExtension(X509Extensions.SubjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.rfc822Name, "test@test.test")));

        X509Certificate[] chain = new X509Certificate[1];
        chain[0] = certGen.generate(pair.getPrivate());

        return chain;
    }
*/    
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
		X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);

		return cert;
	}

	public List<String> getKeyAliases() throws KeyStoreException {
		return Collections.list(this.keyStore.aliases());
	}

	public SignatureAlgorithm getSignatureAlgorithm(String alias) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
		Certificate[] chain = keyStore.getCertificateChain(alias);
		if ((chain == null) || chain.length == 0) {
			return null;
		}
		
		X509Certificate cert = (X509Certificate) chain[0];

		String sighAlgName = cert.getSigAlgName();

		for (SignatureAlgorithm sa : SignatureAlgorithm.values()) {
            if (StringHelper.equalsIgnoreCase(sighAlgName, sa.getAlgorithm())) {
            	return sa;
            }
		}

		return null;
	}

}