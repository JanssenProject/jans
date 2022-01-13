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
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.AESEncrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.crypto.PasswordBasedEncrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.exception.InvalidJweException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.util.Base64Util;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.digests.TigerDigest;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;

/**
 * 
 * 
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public class JweEncrypterImpl extends AbstractJweEncrypter {

    private PublicKey publicKey;                // Public Asymmetric Key.
    private ECKey ecKey;

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

    public JweEncrypterImpl(KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm, byte[] sharedSymmetricKey) {
        super(keyEncryptionAlgorithm, blockEncryptionAlgorithm);
        if (sharedSymmetricKey != null) {
            this.sharedSymmetricKey = sharedSymmetricKey.clone();
        }
    }

    public JweEncrypterImpl(KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm, String sharedSymmetricPassword) {
        super(keyEncryptionAlgorithm, blockEncryptionAlgorithm);
        this.sharedSymmetricPassword = sharedSymmetricPassword;
    }

    public JweEncrypterImpl(KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm, PublicKey publicKey) {
        super(keyEncryptionAlgorithm, blockEncryptionAlgorithm);
        this.publicKey = publicKey;
    }

    public JweEncrypterImpl(KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm, ECKey ecKey) {
        super(keyEncryptionAlgorithm, blockEncryptionAlgorithm);
        this.ecKey = ecKey;
    }

    public JWEEncrypter createJweEncrypter() throws JOSEException, InvalidJweException, NoSuchAlgorithmException {
        final KeyEncryptionAlgorithm keyEncryptionAlgorithm = getKeyEncryptionAlgorithm();
        if (keyEncryptionAlgorithm == null) {
            throw new InvalidJweException("KeyEncryptionAlgorithm isn't defined");
        }
        final BlockEncryptionAlgorithm blockEncryptionAlgorithm = getBlockEncryptionAlgorithm();
        if (blockEncryptionAlgorithm == null) {
            throw new InvalidJweException("BlockEncryptionAlgorithm isn't defined");
        }
        final AlgorithmFamily algorithmFamily = keyEncryptionAlgorithm.getFamily();
        if (algorithmFamily == AlgorithmFamily.RSA) {
            return new RSAEncrypter(new RSAKey.Builder((RSAPublicKey) publicKey).build());
        } else if (algorithmFamily == AlgorithmFamily.EC) {
            return new ECDHEncrypter(new ECKey.Builder(ecKey).build());
        } else if (algorithmFamily == AlgorithmFamily.AES || algorithmFamily == AlgorithmFamily.DIR) {
            if (sharedSymmetricKey == null) {
                throw new InvalidJweException("The shared symmetric key is null");
            }
            return createJweEncrypterAlgFamilyAesDir();
        } else if (algorithmFamily == AlgorithmFamily.PASSW) {
            if (sharedSymmetricPassword == null) {
                throw new InvalidJweException("The shared symmetric password is null");
            }
            return new PasswordBasedEncrypter(sharedSymmetricPassword, 16, 8192);
        } else {
            throw new InvalidJweException("Wrong AlgorithmFamily value: algorithmFamily = " + algorithmFamily);
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

    private JWEEncrypter createJweEncrypterAlgFamilyAesDir() throws InvalidJweException, KeyLengthException {
        final KeyEncryptionAlgorithm keyEncryptionAlgorithm = getKeyEncryptionAlgorithm();
        final BlockEncryptionAlgorithm blockEncryptionAlgorithm = getBlockEncryptionAlgorithm();
        final AlgorithmFamily algorithmFamily = keyEncryptionAlgorithm.getFamily();
        int keyLength = 0;
        switch(keyEncryptionAlgorithm) {
        case A128KW:
        case A128GCMKW: {
            keyLength = 16;
            if (sharedSymmetricKey.length != keyLength) {
                Digest hashCalc = new MD5Digest(); // hash length == 128 bits
                hashCalc.update(sharedSymmetricKey, 0, sharedSymmetricKey.length);
                sharedSymmetricKey = new byte[hashCalc.getDigestSize()];
                hashCalc.doFinal(sharedSymmetricKey, 0);
            }
            break;
        }
        case A192KW:
        case A192GCMKW: {
            keyLength = 24;
            if (sharedSymmetricKey.length != keyLength) {
                Digest hashCalc = new TigerDigest(); // hash length == 192 bits
                hashCalc.update(sharedSymmetricKey, 0, sharedSymmetricKey.length);
                sharedSymmetricKey = new byte[hashCalc.getDigestSize()];
                hashCalc.doFinal(sharedSymmetricKey, 0);
            }
            break;
        }
        case A256KW:
        case A256GCMKW: {
            keyLength = 32;
            if (sharedSymmetricKey.length != keyLength) {
                Digest hashCalc = new SHA256Digest(); // hash length == 256 bits
                hashCalc.update(sharedSymmetricKey, 0, sharedSymmetricKey.length);
                sharedSymmetricKey = new byte[hashCalc.getDigestSize()];
                hashCalc.doFinal(sharedSymmetricKey, 0);
            }
            break;
        }
        case DIR: {
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
            break;
        }
        default: {
            throw new InvalidJweException(String.format("Wrong value of the key encryption algorithm: %s", keyEncryptionAlgorithm.toString()));
        }
        }
        if (algorithmFamily == AlgorithmFamily.AES) {
            return new AESEncrypter(sharedSymmetricKey);
        } else { // if algorithmFamily == AlgorithmFamily.DIR
            return new DirectEncrypter(sharedSymmetricKey);
        }
     }
}