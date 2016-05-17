/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto.signature;

import com.google.common.base.Strings;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author Javier Rojas Blum
 * @version April 22, 2016
 */
public class HMACSigner extends AbstractSigner {

    private String sharedSecret;

    public HMACSigner(SignatureAlgorithm signatureAlgorithm, String sharedSecret) throws Exception {
        super(signatureAlgorithm);

        if (signatureAlgorithm == null || !SignatureAlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily())) {
            throw new Exception("Invalid signature algorithm");
        }
        if (Strings.isNullOrEmpty(sharedSecret)) {
            throw new Exception("Invalid shared secret");
        }

        this.sharedSecret = sharedSecret;
    }

    @Override
    public String sign(String signingInput) throws Exception {
        if (Strings.isNullOrEmpty(signingInput)) {
            throw new Exception("Invalid signing input");
        }

        try {
            SecretKey secretKey = new SecretKeySpec(sharedSecret.getBytes(Util.UTF8_STRING_ENCODING), getSignatureAlgorithm().getAlgorithm());
            Mac mac = Mac.getInstance(getSignatureAlgorithm().getAlgorithm());
            mac.init(secretKey);
            byte[] sig = mac.doFinal(signingInput.getBytes(Util.UTF8_STRING_ENCODING));
            return JwtUtil.base64urlencode(sig);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("There was a problem in HMAC signing", e);
        } catch (InvalidKeyException e) {
            throw new Exception("There was a problem in HMAC signing", e);
        } catch (UnsupportedEncodingException e) {
            throw new Exception("There was a problem in HMAC signing", e);
        }
    }

    @Override
    public boolean verifySignature(String signingInput, String signature) throws Exception {
        if (Strings.isNullOrEmpty(signingInput)) {
            return false;
        }
        if (Strings.isNullOrEmpty(signature)) {
            return false;
        }

        String expectedSignature = sign(signingInput);
        return expectedSignature.equals(signature);
    }
}
