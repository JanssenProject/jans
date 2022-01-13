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
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.exception.InvalidJweException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.util.SecurityProviderUtility;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.digests.TigerDigest;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.PrivateKey;

/**
 * 
 * 
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public class JweDecrypterImpl extends AbstractJweDecrypter {

    private static final DefaultJWEDecrypterFactory DECRYPTER_FACTORY = new DefaultJWEDecrypterFactory();

    private PrivateKey privateKey;              // Private Asymmetric Key.
    
    private byte[] sharedSymmetricKey;          // Shared Symmetric Key (byte []).
    private String sharedSymmetricPassword;     // Shared Symmetric Password (String).
    
    // Note: Shared Key and Shared Password should be separated (distinguishable),
    // so, sharedKey is a Byte Array and sharedPassword is a String.
    
    // Shared Symmetric Key (sharedKey) is used, when are used follow KeyEncryptionAlgorithm values:
    // A128KW
    // A192KW
    // A256KW
    // A128GCMKW
    // A192GCMKW
    // A256GCMKW
    // DIR

    // Shared Symmetric Password (sharedPassword) is used, when are used follow KeyEncryptionAlgorithm values:
    // PBES2_HS256_PLUS_A128KW
    // PBES2_HS384_PLUS_A192KW
    // PBES2_HS512_PLUS_A256KW

    // PrivateKey is used,  when are used follow KeyEncryptionAlgorithm values:
    // RSA1_5
    // RSA_OAEP
    // RSA_OAEP_256
    // ECDH_ES
    // ECDH_ES_PLUS_A128KW
    // ECDH_ES_PLUS_A192KW
    // ECDH_ES_PLUS_A256KW

    /**
     * Constructor.
     * 
     * @param sharedSymmetricKey Shared Symmetric Key (byte []).
     */
    public JweDecrypterImpl(byte[] sharedSymmetricKey) {
        if (sharedSymmetricKey != null) {
            this.sharedSymmetricKey = sharedSymmetricKey.clone();
        }
    }

    /**
     * Constructor.
     * 
     * @param sharedSymmetricPassword Shared Symmetric Password (String).
     */
    public JweDecrypterImpl(String sharedSymmetricPassword) {
        this.sharedSymmetricPassword = sharedSymmetricPassword;
    }

    /**
     * Constructor.
     * 
     * @param privateKey Private Asymmetric Key.
     */
    public JweDecrypterImpl(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * 
     * 
     * @param encryptedJwe
     * @return
     * @throws InvalidJweException
     */
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
            setBlockEncryptionAlgorithm(
                    BlockEncryptionAlgorithm.fromName(jwe.getHeader().getClaimAsString(JwtHeaderName.ENCRYPTION_METHOD)));

            final KeyEncryptionAlgorithm keyEncryptionAlgorithm = getKeyEncryptionAlgorithm();
            if (keyEncryptionAlgorithm == null) {
                throw new InvalidJweException("KeyEncryptionAlgorithm isn't defined");
            }
            final BlockEncryptionAlgorithm blockEncryptionAlgorithm = getBlockEncryptionAlgorithm();
            if (blockEncryptionAlgorithm == null) {
                throw new InvalidJweException("BlockEncryptionAlgorithm isn't defined");
            }
            Key encriptionKey = null;
            final AlgorithmFamily algorithmFamily = keyEncryptionAlgorithm.getFamily();
            if (algorithmFamily == AlgorithmFamily.RSA || algorithmFamily == AlgorithmFamily.EC) {
                encriptionKey = privateKey;
            } else if (algorithmFamily == AlgorithmFamily.AES || algorithmFamily == AlgorithmFamily.DIR) {
                if (sharedSymmetricKey == null) {
                    throw new InvalidJweException("The shared symmetric key is null");
                }
                int keyLength = 0;
                if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A128KW || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A128GCMKW) {
                    keyLength = 16;
                    if (sharedSymmetricKey.length != keyLength) {
                        Digest hashCalc = new MD5Digest(); // hash length == 128 bits
                        hashCalc.update(sharedSymmetricKey, 0, sharedSymmetricKey.length);
                        sharedSymmetricKey = new byte[hashCalc.getDigestSize()];
                        hashCalc.doFinal(sharedSymmetricKey, 0);
                    }
                } else if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A192KW
                        || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A192GCMKW) {
                    keyLength = 24;
                    if (sharedSymmetricKey.length != keyLength) {
                        Digest hashCalc = new TigerDigest(); // hash length == 192 bits
                        hashCalc.update(sharedSymmetricKey, 0, sharedSymmetricKey.length);
                        sharedSymmetricKey = new byte[hashCalc.getDigestSize()];
                        hashCalc.doFinal(sharedSymmetricKey, 0);
                    }
                } else if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A256KW
                        || keyEncryptionAlgorithm == KeyEncryptionAlgorithm.A256GCMKW) {
                    keyLength = 32;
                    if (sharedSymmetricKey.length != keyLength) {
                        Digest hashCalc = new SHA256Digest(); // hash length == 256 bits
                        hashCalc.update(sharedSymmetricKey, 0, sharedSymmetricKey.length);
                        sharedSymmetricKey = new byte[hashCalc.getDigestSize()];
                        hashCalc.doFinal(sharedSymmetricKey, 0);
                    }
                } else if (keyEncryptionAlgorithm == KeyEncryptionAlgorithm.DIR) {
                    keyLength = blockEncryptionAlgorithm.getCmkLength() / 8; // 128, 192, 256, 384, 512
                    if (sharedSymmetricKey.length != keyLength) {
                        Digest hashCalc = null;
                        switch (keyLength) {
                        case 16: {
                            hashCalc = new MD5Digest(); // hash length == 128 bits
                            break;
                        }
                        case 24: {
                            hashCalc = new TigerDigest(); // hash length == 192 bits
                            break;
                        }
                        case 32: {
                            hashCalc = new SHA256Digest(); // hash length == 256 bits
                            break;
                        }
                        case 48: {
                            hashCalc = new SHA384Digest(); // hash length == 384 bits
                            break;
                        }
                        case 64: {
                            hashCalc = new SHA512Digest(); // hash length == 512 bits
                            break;
                        }
                        default: {
                            throw new InvalidJweException(String.format("Wrong value of the key length: %d", keyLength));
                        }
                        }
                        hashCalc.update(sharedSymmetricKey, 0, sharedSymmetricKey.length);
                        sharedSymmetricKey = new byte[hashCalc.getDigestSize()];
                        hashCalc.doFinal(sharedSymmetricKey, 0);
                    }
                } else {
                    throw new InvalidJweException(
                            String.format("Wrong value of the key encryption algorithm: %s", keyEncryptionAlgorithm.toString()));
                }
                encriptionKey = new SecretKeySpec(sharedSymmetricKey, 0, sharedSymmetricKey.length, "AES");
            } else if (algorithmFamily == AlgorithmFamily.PASSW) {
                encriptionKey = new SecretKeySpec(sharedSymmetricPassword.getBytes(), 0, sharedSymmetricPassword.length(), "AES");
            } else {
                throw new InvalidJweException("wrong AlgorithmFamily value");
            }
            JWEDecrypter decrypter = DECRYPTER_FACTORY.createJWEDecrypter(encryptedJwt.getHeader(), encriptionKey);
            decrypter.getJCAContext().setProvider(SecurityProviderUtility.getInstance());
            encryptedJwt.decrypt(decrypter);
            final SignedJWT signedJWT = encryptedJwt.getPayload().toSignedJWT();
            if (signedJWT != null) {
                final Jwt jwt = Jwt.parse(signedJWT.serialize());
                jwe.setSignedJWTPayload(jwt);
                jwe.setClaims(jwt.getClaims());
            } else {
                final String base64encodedPayload = encryptedJwt.getPayload().toString();
                jwe.setClaims(new JwtClaims(base64encodedPayload));
            }
            return jwe;
        } catch (Exception e) {
            throw new InvalidJweException(e);
        }
    }
}