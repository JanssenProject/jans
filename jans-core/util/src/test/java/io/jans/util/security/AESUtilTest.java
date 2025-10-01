package io.jans.util.security;

import org.testng.annotations.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static io.jans.util.security.AESUtil.generateGcmSpec;
import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Z
 */
public class AESUtilTest {

    @Test
    public void cbc_whenEncryptAndDecrypt_shouldProduceCorrectOutput() throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException {
        String input = "textToEncrypt";
        SecretKey key = AESUtil.generateKey(128);
        IvParameterSpec ivParameterSpec = AESUtil.generateIv();

        String algorithm = "AES/CBC/PKCS5Padding";
        String cipherText = AESUtil.encrypt(algorithm, input, key, ivParameterSpec);
        String plainText = AESUtil.decrypt(algorithm, cipherText, key, ivParameterSpec);

        assertEquals(input, plainText);
    }

    @Test
    public void gcm_whenEncryptAndDecrypt_shouldProduceCorrectOutput() throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException {
        String input = "textToEncrypt";
        SecretKey key = AESUtil.generateKey(128);

        String algorithm = "AES/GCM/NoPadding";
        final GCMParameterSpec gcmSpec = generateGcmSpec();

        String cipherText = AESUtil.encrypt(algorithm, input, key, gcmSpec);
        String plainText = AESUtil.decrypt(algorithm, cipherText, key, gcmSpec);

        assertEquals(input, plainText);
    }
}
