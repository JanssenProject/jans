/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto.signature;

import com.google.common.base.Strings;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

/**
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
@Deprecated
public class ECSigner extends AbstractSigner {

    private ECDSAPrivateKey ecdsaPrivateKey;
    private ECDSAPublicKey ecdsaPublicKey;

    @Deprecated
    public ECSigner(SignatureAlgorithm signatureAlgorithm, ECDSAPrivateKey ecdsaPrivateKey) throws Exception {
        super(signatureAlgorithm);

        if (ecdsaPrivateKey == null) {
            throw new Exception("Invalid EC private key");
        }

        this.ecdsaPrivateKey = ecdsaPrivateKey;
    }

    @Deprecated
    public ECSigner(SignatureAlgorithm signatureAlgorithm, ECDSAPublicKey ecdsaPublicKey) throws Exception {
        super(signatureAlgorithm);

        if (ecdsaPublicKey == null) {
            throw new Exception("Invalid EC private key");
        }

        this.ecdsaPublicKey = ecdsaPublicKey;
    }

    @Deprecated
    private ECSigner(SignatureAlgorithm signatureAlgorithm) throws Exception {
        super(signatureAlgorithm);

        if (signatureAlgorithm == null || !SignatureAlgorithmFamily.EC.equals(signatureAlgorithm.getFamily())) {
            throw new Exception("Invalid signature algorithm");
        }
    }

    @Deprecated
    @Override
    public String sign(String signingInput) throws Exception {
        if (Strings.isNullOrEmpty(signingInput)) {
            throw new Exception("Invalid signing input");
        }

        try {
            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(getSignatureAlgorithm().getCurve().getName());
            ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(ecdsaPrivateKey.getD(), ecSpec);

            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            Signature signature = Signature.getInstance(getSignatureAlgorithm().getAlgorithm(), "BC");
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes(Util.UTF8_STRING_ENCODING));

            return JwtUtil.base64urlencode(signature.sign());
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("There was a problem in EC signing", e);
        } catch (UnsupportedEncodingException e) {
            throw new Exception("There was a problem in EC signing", e);
        } catch (SignatureException e) {
            throw new Exception("There was a problem in EC signing", e);
        } catch (NoSuchProviderException e) {
            throw new Exception("There was a problem in EC signing", e);
        } catch (InvalidKeyException e) {
            throw new Exception("There was a problem in EC signing", e);
        } catch (InvalidKeySpecException e) {
            throw new Exception("There was a problem in EC signing", e);
        }
    }

    @Deprecated
    @Override
    public boolean verifySignature(String signingInput, String signature) throws Exception {
        if (Strings.isNullOrEmpty(signingInput)) {
            return false;
        }
        if (Strings.isNullOrEmpty(signature)) {
            return false;
        }

        try {
            byte[] sigBytes = JwtUtil.base64urldecode(signature);
            byte[] sigInBytes = signingInput.getBytes(Util.UTF8_STRING_ENCODING);

            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(getSignatureAlgorithm().getCurve().getName());
            BigInteger q = ((ECCurve.Fp) ecSpec.getCurve()).getQ();
            ECFieldElement xFieldElement = new ECFieldElement.Fp(q, ecdsaPublicKey.getX());
            ECFieldElement yFieldElement = new ECFieldElement.Fp(q, ecdsaPublicKey.getY());
            ECPoint pointQ = new ECPoint.Fp(ecSpec.getCurve(), xFieldElement, yFieldElement);
            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            Signature sig = Signature.getInstance(getSignatureAlgorithm().getAlgorithm(), "BC");
            sig.initVerify(publicKey);
            sig.update(sigInBytes);
            return sig.verify(sigBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("There was a problem in EC verifier", e);
        } catch (UnsupportedEncodingException e) {
            throw new Exception("There was a problem in EC verifier", e);
        } catch (SignatureException e) {
            throw new Exception("There was a problem in EC verifier", e);
        } catch (NoSuchProviderException e) {
            throw new Exception("There was a problem in EC verifier", e);
        } catch (InvalidKeyException e) {
            throw new Exception("There was a problem in EC verifier", e);
        } catch (InvalidKeySpecException e) {
            throw new Exception("There was a problem in EC verifier", e);
        }
    }
}
