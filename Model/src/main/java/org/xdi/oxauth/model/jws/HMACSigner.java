/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jws;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.StringUtils;
import org.xdi.oxauth.model.util.Util;

/**
 * @author Javier Rojas Blum Date: 11.12.2012
 */
public class HMACSigner extends AbstractJwsSigner {

    private String sharedSecret;

    public HMACSigner(SignatureAlgorithm signatureAlgorithm, String sharedSecret) {
        super(signatureAlgorithm);
        this.sharedSecret = sharedSecret;
    }

    @Override
    public String generateSignature(String signingInput) throws SignatureException {
        if (getSignatureAlgorithm() == null) {
            throw new SignatureException("The signature algorithm is null");
        }
        if (sharedSecret == null) {
            throw new SignatureException("The shared secret is null");
        }
        if (signingInput == null) {
            throw new SignatureException("The signing input is null");
        }

        String algorithm;
        switch (getSignatureAlgorithm()) {
            case HS256:
                algorithm = "HMACSHA256";
                break;
            case HS384:
                algorithm = "HMACSHA384";
                break;
            case HS512:
                algorithm = "HMACSHA512";
                break;
            default:
                throw new SignatureException("Unsupported signature algorithm");
        }

        try {
            SecretKey secretKey = new SecretKeySpec(sharedSecret.getBytes(Util.UTF8_STRING_ENCODING), algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(secretKey);
            byte[] sig = mac.doFinal(signingInput.getBytes(Util.UTF8_STRING_ENCODING));
            return JwtUtil.base64urlencode(sig);
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException(e);
        } catch (InvalidKeyException e) {
            throw new SignatureException(e);
        } catch (UnsupportedEncodingException e) {
            throw new SignatureException(e);
        } catch (Exception e) {
            throw new SignatureException(e);
        }
    }

    @Override
    public boolean validateSignature(String signingInput, String signature) throws SignatureException {
        String expectedSignature = generateSignature(signingInput);
        return StringUtils.nullToEmpty(signature).equals(StringUtils.nullToEmpty(expectedSignature));
    }
}