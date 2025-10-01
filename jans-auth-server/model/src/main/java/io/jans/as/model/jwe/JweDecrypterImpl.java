/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwe;

import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.crypto.factories.DefaultJWEDecrypterFactory;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.SignedJWT;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJweException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.util.security.SecurityProviderUtility;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.util.Arrays;

/**
 * @author Javier Rojas Blum
 * @version November 20, 2018
 */
public class JweDecrypterImpl extends AbstractJweDecrypter {

    private static final DefaultJWEDecrypterFactory DECRYPTER_FACTORY = new DefaultJWEDecrypterFactory();

    private PrivateKey privateKey;
    private byte[] sharedSymmetricKey;
    private boolean fapi;

    public boolean isFapi() {
        return fapi;
    }
    public void setFapi(boolean fapi) {
        this.fapi = fapi;
    }

    public JweDecrypterImpl(byte[] sharedSymmetricKey) {
        if (sharedSymmetricKey != null) {
            this.sharedSymmetricKey = sharedSymmetricKey.clone();
        }
    }

    public JweDecrypterImpl(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public Jwe decrypt(String encryptedJwe) throws InvalidJweException {
        try {
            String[] jweParts = encryptedJwe.split("\\.");
            if (jweParts.length != 5) {
                throw new InvalidJwtException("Invalid JWS format.");
            }

            String encodedHeader = jweParts[0];
            String encodedEncryptedKey = jweParts[1];
            String encodedInitializationVector = jweParts[2];
            String encodedCipherText = jweParts[3];
            String encodedIntegrityValue = jweParts[4];

            Jwe jwe = new Jwe();
            jwe.setEncodedHeader(encodedHeader);
            jwe.setEncodedEncryptedKey(encodedEncryptedKey);
            jwe.setEncodedInitializationVector(encodedInitializationVector);
            jwe.setEncodedCiphertext(encodedCipherText);
            jwe.setEncodedIntegrityValue(encodedIntegrityValue);
            jwe.setHeader(new JwtHeader(encodedHeader));

            EncryptedJWT encryptedJwt = EncryptedJWT.parse(encryptedJwe);

            setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm.fromName(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM)));
            setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm.fromName(jwe.getHeader().getClaimAsString(JwtHeaderName.ENCRYPTION_METHOD)));

            final KeyEncryptionAlgorithm keyEncryptionAlgorithm = getKeyEncryptionAlgorithm();
            Key encriptionKey = null;
            if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA1_5 || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.RSA_OAEP) {
                encriptionKey = privateKey;
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
                encriptionKey = new SecretKeySpec(sharedSymmetricKey, 0, sharedSymmetricKey.length, "AES");
            } else {
                throw new InvalidJweException("The key encryption algorithm is not supported");
            }

            JWEDecrypter decrypter = DECRYPTER_FACTORY.createJWEDecrypter(encryptedJwt.getHeader(), encriptionKey);
            decrypter.getJCAContext().setProvider(SecurityProviderUtility.getBCProvider());
            encryptedJwt.decrypt(decrypter);

            final SignedJWT signedJWT = encryptedJwt.getPayload().toSignedJWT();
            if (signedJWT != null) {
                final Jwt jwt = Jwt.parse(signedJWT.serialize());
                jwe.setSignedJWTPayload(jwt);
                jwe.setClaims(jwt.getClaims());
            } else {
                final String base64encodedPayload = encryptedJwt.getPayload().toString();
                validateNestedJwt(base64encodedPayload);
                jwe.setClaims(new JwtClaims(base64encodedPayload));
            }

            return jwe;
        } catch (Exception e) {
            throw new InvalidJweException(e);
        }
    }

    private void validateNestedJwt(String base64encodedPayload) throws InvalidJwtException {
        final Jwt jwt = Jwt.parseSilently(base64encodedPayload);
        if (jwt != null && jwt.getHeader().getSignatureAlgorithm() == SignatureAlgorithm.NONE && isFapi()) {
            throw new InvalidJwtException("The None algorithm in nested JWT is not allowed for FAPI");
        }
    }
}
