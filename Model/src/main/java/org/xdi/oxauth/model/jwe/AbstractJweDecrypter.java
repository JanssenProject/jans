/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwe;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.JwtClaims;
import org.xdi.oxauth.model.jwt.JwtHeader;
import org.xdi.oxauth.model.jwt.JwtHeaderName;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;

/**
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public abstract class AbstractJweDecrypter implements JweDecrypter {

    private KeyEncryptionAlgorithm keyEncryptionAlgorithm;
    private BlockEncryptionAlgorithm blockEncryptionAlgorithm;

    public KeyEncryptionAlgorithm getKeyEncryptionAlgorithm() {
        return keyEncryptionAlgorithm;
    }

    public void setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm keyEncryptionAlgorithm) {
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
    }

    public BlockEncryptionAlgorithm getBlockEncryptionAlgorithm() {
        return blockEncryptionAlgorithm;
    }

    public void setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm blockEncryptionAlgorithm) {
        this.blockEncryptionAlgorithm = blockEncryptionAlgorithm;
    }

    /*@Override
    public Jwe decrypt(String encryptedJwe) throws InvalidJweException {
        try {
            if (StringUtils.isBlank(encryptedJwe)) {
                return null;
            }

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

            keyEncryptionAlgorithm = KeyEncryptionAlgorithm.fromName(
                    jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
            blockEncryptionAlgorithm = BlockEncryptionAlgorithm.fromName(
                    jwe.getHeader().getClaimAsString(JwtHeaderName.ENCRYPTION_METHOD));

            byte[] contentMasterKey = decryptEncryptionKey(encodedEncryptedKey);
            byte[] initializationVector = Base64Util.base64urldecode(encodedInitializationVector);
            byte[] authenticationTag = Base64Util.base64urldecode(encodedIntegrityValue);
            byte[] additionalAuthenticatedData = jwe.getAdditionalAuthenticatedData().getBytes(Util.UTF8_STRING_ENCODING);

            String plainText = decryptCipherText(encodedCipherText, contentMasterKey, initializationVector,
                    authenticationTag, additionalAuthenticatedData);
            jwe.setClaims(new JwtClaims(plainText));

            return jwe;
        } catch (InvalidJwtException e) {
            throw new InvalidJweException(e);
        } catch (UnsupportedEncodingException e) {
            throw new InvalidJweException(e);
        }
    }*/

    public abstract byte[] decryptEncryptionKey(String encodedEncryptedKey) throws InvalidJweException;

    public abstract String decryptCipherText(String encodedCipherText, byte[] contentMasterKey,
                                             byte[] initializationVector, byte[] authenticationTag,
                                             byte[] additionalAuthenticatedData) throws InvalidJweException;
}