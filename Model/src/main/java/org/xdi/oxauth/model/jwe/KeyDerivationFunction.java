/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwe;

import org.apache.commons.lang.ArrayUtils;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.exception.InvalidParameterException;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;

/**
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public class KeyDerivationFunction {

    public static byte[] generateCek(byte[] cmk, BlockEncryptionAlgorithm blockEncryptionAlgorithm)
            throws UnsupportedEncodingException, NoSuchProviderException, NoSuchAlgorithmException, InvalidParameterException {
        if (cmk == null) {
            throw new InvalidParameterException("The content master key (CMK) is null");
        }
        if (blockEncryptionAlgorithm == null) {
            throw new InvalidParameterException("The block encryption algorithm is null");
        }
        if (blockEncryptionAlgorithm != BlockEncryptionAlgorithm.A128CBC_PLUS_HS256
                && blockEncryptionAlgorithm != BlockEncryptionAlgorithm.A256CBC_PLUS_HS512) {
            throw new InvalidParameterException("The block encryption algorithm is not supported");
        }

        byte[] round1 = Base64Util.unsignedToBytes(new int[]{0, 0, 0, 1});
        byte[] outputBitSize = null;
        if (blockEncryptionAlgorithm != BlockEncryptionAlgorithm.A128CBC_PLUS_HS256) {
            outputBitSize = Base64Util.unsignedToBytes(new int[]{0, 0, 0, 128});
        } else { //A256CBC_PLUS_HS512
            outputBitSize = Base64Util.unsignedToBytes(new int[]{0, 0, 1, 0});
        }
        byte[] encValue = blockEncryptionAlgorithm.getName().getBytes(Util.UTF8_STRING_ENCODING);
        byte[] epu = Base64Util.unsignedToBytes(new int[]{0, 0, 0, 0});
        byte[] epv = Base64Util.unsignedToBytes(new int[]{0, 0, 0, 0});
        byte[] label = "Encryption".getBytes(Util.UTF8_STRING_ENCODING);
        byte[] round1Input = ArrayUtils.addAll(round1, cmk);
        round1Input = ArrayUtils.addAll(round1Input, outputBitSize);
        round1Input = ArrayUtils.addAll(round1Input, encValue);
        round1Input = ArrayUtils.addAll(round1Input, epu);
        round1Input = ArrayUtils.addAll(round1Input, epv);
        round1Input = ArrayUtils.addAll(round1Input, label);

        MessageDigest mda = MessageDigest.getInstance(blockEncryptionAlgorithm.getMessageDiggestAlgorithm(), "BC");
        byte[] round1Hash = mda.digest(round1Input);
        byte[] cek = Arrays.copyOf(round1Hash, blockEncryptionAlgorithm.getCekLength() / 8);

        return cek;
    }

    public static byte[] generateCik(byte[] cmk, BlockEncryptionAlgorithm blockEncryptionAlgorithm)
            throws UnsupportedEncodingException, NoSuchProviderException, NoSuchAlgorithmException, InvalidParameterException {
        if (cmk == null) {
            throw new InvalidParameterException("The content master key (CMK) is null");
        }
        if (blockEncryptionAlgorithm == null) {
            throw new InvalidParameterException("The block encryption algorithm is null");
        }
        if (blockEncryptionAlgorithm != BlockEncryptionAlgorithm.A128CBC_PLUS_HS256
                && blockEncryptionAlgorithm != BlockEncryptionAlgorithm.A256CBC_PLUS_HS512) {
            throw new InvalidParameterException("The block encryption algorithm is not supported");
        }

        byte[] round1 = Base64Util.unsignedToBytes(new int[]{0, 0, 0, 1});
        byte[] outputBitSize = null;
        if (blockEncryptionAlgorithm != BlockEncryptionAlgorithm.A128CBC_PLUS_HS256) {
            outputBitSize = Base64Util.unsignedToBytes(new int[]{0, 0, 1, 0});
        } else { //A256CBC_PLUS_HS512
            outputBitSize = Base64Util.unsignedToBytes(new int[]{0, 0, 2, 0});
        }
        byte[] encValue = blockEncryptionAlgorithm.getName().getBytes(Util.UTF8_STRING_ENCODING);
        byte[] epu = Base64Util.unsignedToBytes(new int[]{0, 0, 0, 0});
        byte[] epv = Base64Util.unsignedToBytes(new int[]{0, 0, 0, 0});
        byte[] label = "Integrity".getBytes(Util.UTF8_STRING_ENCODING);
        byte[] round1Input = ArrayUtils.addAll(round1, cmk);
        round1Input = ArrayUtils.addAll(round1Input, outputBitSize);
        round1Input = ArrayUtils.addAll(round1Input, encValue);
        round1Input = ArrayUtils.addAll(round1Input, epu);
        round1Input = ArrayUtils.addAll(round1Input, epv);
        round1Input = ArrayUtils.addAll(round1Input, label);

        MessageDigest mda = MessageDigest.getInstance(blockEncryptionAlgorithm.getMessageDiggestAlgorithm(), "BC");
        byte[] cik = mda.digest(round1Input);

        return cik;
    }
}