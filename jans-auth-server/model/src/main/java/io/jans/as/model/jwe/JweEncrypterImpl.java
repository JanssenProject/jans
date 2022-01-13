/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwe;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.AESEncrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.exception.InvalidJweException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.util.Base64Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Arrays;

/**
 * @author Javier Rojas Blum
 * @version November 20, 2018
 */
public class JweEncrypterImpl extends AbstractJweEncrypter {

    private PublicKey publicKey;
    private byte[] sharedSymmetricKey;

    public JweEncrypterImpl(KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm, byte[] sharedSymmetricKey) {
        super(keyEncryptionAlgorithm, blockEncryptionAlgorithm);
        if (sharedSymmetricKey != null) {
            this.sharedSymmetricKey = sharedSymmetricKey.clone();
        }
    }

    public JweEncrypterImpl(KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm, PublicKey publicKey) {
        super(keyEncryptionAlgorithm, blockEncryptionAlgorithm);
        this.publicKey = publicKey;
    }

    public JWEEncrypter createJweEncrypter() throws JOSEException, InvalidJweException, NoSuchAlgorithmException {
        final KeyEncryptionAlgorithm keyEncryptionAlgorithm = getKeyEncryptionAlgorithm();
        if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA1_5 || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA_OAEP) {
            return new RSAEncrypter(new RSAKey.Builder((RSAPublicKey) publicKey).build());
        } else if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A128KW || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A256KW) {
            if (sharedSymmetricKey == null) {
                throw new InvalidJweException("The shared symmetric key is null");
            }

            int keyLength = 16;
            if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A256KW) {
                keyLength = 32;
            }

            if (sharedSymmetricKey.length != keyLength) {
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                sharedSymmetricKey = sha.digest(sharedSymmetricKey);
                sharedSymmetricKey = Arrays.copyOf(sharedSymmetricKey, keyLength);
            }

            return new AESEncrypter(sharedSymmetricKey);
        } else {
            throw new InvalidJweException("The key encryption algorithm is not supported");
        }
    }

    public static Payload createPayload(Jwe jwe) throws ParseException, InvalidJwtException {
        if (jwe.getSignedJWTPayload() != null) {
            return new Payload(SignedJWT.parse(jwe.getSignedJWTPayload().toString()));
        }
        return new Payload(Base64Util.base64urlencode(jwe.getClaims().toJsonString().getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public Jwe encrypt(Jwe jwe) throws InvalidJweException {
        try {
            JWEEncrypter encrypter = createJweEncrypter();

            if (jwe.getSignedJWTPayload() != null) {
                jwe.getHeader().setContentType(JwtType.JWT);
            }
            JWEObject jweObject = new JWEObject(JWEHeader.parse(jwe.getHeader().toJsonObject().toString()), createPayload(jwe));

            jweObject.encrypt(encrypter);
            String encryptedJwe = jweObject.serialize();

            String[] jweParts = encryptedJwe.split("\\.");
            if (jweParts.length != 5) {
                throw new InvalidJwtException("Invalid JWS format.");
            }

            String encodedHeader = jweParts[0];
            String encodedEncryptedKey = jweParts[1];
            String encodedInitializationVector = jweParts[2];
            String encodedCipherText = jweParts[3];
            String encodedIntegrityValue = jweParts[4];

            jwe.setEncodedHeader(encodedHeader);
            jwe.setEncodedEncryptedKey(encodedEncryptedKey);
            jwe.setEncodedInitializationVector(encodedInitializationVector);
            jwe.setEncodedCiphertext(encodedCipherText);
            jwe.setEncodedIntegrityValue(encodedIntegrityValue);
            jwe.setHeader(new JwtHeader(encodedHeader));

            return jwe;
        } catch (Exception e) {
            throw new InvalidJweException(e);
        }
    }
}