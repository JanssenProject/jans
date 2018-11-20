/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwe;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.AESEncrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.RSAKey;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.JwtHeader;
import org.xdi.oxauth.model.util.Base64Util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
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
            if (sharedSymmetricKey.length != 16) { // 128 bit padding
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                sharedSymmetricKey = sha.digest(sharedSymmetricKey);
                sharedSymmetricKey = Arrays.copyOf(sharedSymmetricKey, 16);
            }

            return new AESEncrypter(sharedSymmetricKey);
        } else {
            throw new InvalidJweException("The key encryption algorithm is not supported");
        }
    }

    @Override
    public Jwe encrypt(Jwe jwe) throws InvalidJweException {
        try {
            JWEEncrypter encrypter = createJweEncrypter();

            JWEObject jweObject = new JWEObject(
                    JWEHeader.parse(jwe.getHeader().toJsonObject().toString()),
                    new Payload(Base64Util.base64urlencode(jwe.getClaims().toJsonString().getBytes("UTF-8"))));

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