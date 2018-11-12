/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwe;

import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Pair;
import org.xdi.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

/**
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public abstract class AbstractJweEncrypter implements JweEncrypter {

    private KeyEncryptionAlgorithm keyEncryptionAlgorithm;
    private BlockEncryptionAlgorithm blockEncryptionAlgorithm;

    protected AbstractJweEncrypter(KeyEncryptionAlgorithm keyEncryptionAlgorithm, BlockEncryptionAlgorithm blockEncryptionAlgorithm) {
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
        this.blockEncryptionAlgorithm = blockEncryptionAlgorithm;
    }

    public KeyEncryptionAlgorithm getKeyEncryptionAlgorithm() {
        return keyEncryptionAlgorithm;
    }

    public BlockEncryptionAlgorithm getBlockEncryptionAlgorithm() {
        return blockEncryptionAlgorithm;
    }

    /*@Override
    public Jwe encrypt(Jwe jwe) throws InvalidJweException {
        try {
            jwe.setEncodedHeader(jwe.getHeader().toBase64JsonObject());

            byte[] contentMasterKey = new byte[blockEncryptionAlgorithm.getCmkLength() / 8];
            SecureRandom random = new SecureRandom();
            random.nextBytes(contentMasterKey);

            String encodedEncryptedKey = generateEncryptedKey(contentMasterKey);
            jwe.setEncodedEncryptedKey(encodedEncryptedKey);

            byte[] initializationVector = new byte[blockEncryptionAlgorithm.getInitVectorLength() / 8];
            random.nextBytes(initializationVector);
            String encodedInitializationVector = Base64Util.base64urlencode(initializationVector);
            jwe.setEncodedInitializationVector(encodedInitializationVector);

            Pair<String, String> result = generateCipherTextAndIntegrityValue(contentMasterKey, initializationVector,
                    jwe.getAdditionalAuthenticatedData().getBytes(Util.UTF8_STRING_ENCODING),
                    jwe.getClaims().toBase64JsonObject().getBytes(Util.UTF8_STRING_ENCODING));
            jwe.setEncodedCiphertext(result.getFirst());
            jwe.setEncodedIntegrityValue(result.getSecond());

            return jwe;
        } catch (InvalidJwtException e) {
            throw new InvalidJweException(e);
        } catch (UnsupportedEncodingException e) {
            throw new InvalidJweException(e);
        }
    }*/

    public abstract String generateEncryptedKey(byte[] contentMasterKey) throws InvalidJweException;

    public abstract Pair<String, String> generateCipherTextAndIntegrityValue(
            byte[] contentMasterKey, byte[] initializationVector, byte[] additionalAuthenticatedData, byte[] plainText)
            throws InvalidJweException;
}