/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util.security;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Base64;

import io.jans.util.StringHelper;
import io.jans.util.Util;
import io.jans.util.exception.EncryptionException;

/**
 * Encryption algorithms
 *
 * @author ssudala
 * @author Sergey Manoylo
 * @version 2021-10-21
 */
public class StringEncrypter {

    // lazy init via static holder
    private static class Holder {
        static final StringEncrypter INSTANCE = new StringEncrypter();
    }

    /**
     * Default encryption key
     */
    public static final String DEFAULT_ENCRYPTION_KEY = "This is a fairly long phrase used to encrypt";
    
    public static final String DEF_CRYPTO_PROVIDER = "BC";

    public static final String DEF_NONE = "NONE";

    /**
     * Encryption schemes
     */
    public static final String DEF_DES_ENCRYPTION_SCHEME = "DES";
    public static final String DEF_DES_EDE_ENCRYPTION_SCHEME = "DESede";
    public static final String DEF_AES_ENCRYPTION_SCHEME = "AES";

    /**
     * Encryption modes
     */
    public static final String DEF_DES_CBC_ENCRYPTION_MODE = "DES/CBC/PKCS5Padding";            // iv should be used
    public static final String DEF_DES_ECB_ENCRYPTION_MODE = "DES/ECB/PKCS5Padding";            // iv shouldn't be used

    public static final String DEF_DES_EDE_CBC_ENCRYPTION_MODE = "DESede/CBC/PKCS5Padding";     // iv should be used
    public static final String DEF_DES_EDE_ECB_ENCRYPTION_MODE = "DESede/ECB/PKCS5Padding";     // iv shouldn't be used

    public static final String DEF_AES_CBC_ENCRYPTION_MODE = "AES/CBC/PKCS5Padding";            // iv should be used
    public static final String DEF_AES_ECB_ENCRYPTION_MODE = "AES/ECB/PKCS5Padding";            // iv shouldn't be used
    public static final String DEF_AES_GCM_ENCRYPTION_MODE = "AES/GCM/NoPadding";               // iv/nonce should be used

    /**
     * Key length
     */
    public static final String DEF_56 = "56";
    public static final String DEF_112 = "112";
    public static final String DEF_168 = "168";

    public static final String DEF_128 = "128"; 
    public static final String DEF_192 = "192";
    public static final String DEF_256 = "256";
    
    public static final int DEF_IV_LEN_DES = 8;
    public static final int DEF_IV_LEN_AES = 16;
    
    public static final int DEF_RND_STR_LEN = 24;    

    /**
     * Encrypting schemes (AES / DES / 3DES)
     * 
     * @author Sergey Manoylo
     * @version 2021-10-20
     */
    public enum Scheme {

        NONE(-1),

        DES(0),
        DES_EDE(1),
        AES(2);

        private final int schemeVal;

        private Scheme(final int schemeVal) {
            this.schemeVal = schemeVal;
        }

        public int getScheme() {
            return schemeVal;
        }

        @Override
        public String toString() {
            switch (this) {
            case DES: {
                return DEF_DES_ENCRYPTION_SCHEME;
            }
            case DES_EDE: {
                return DEF_DES_EDE_ENCRYPTION_SCHEME;
            }
            case AES: {
                return DEF_AES_ENCRYPTION_SCHEME;
            }
            default: {
                return DEF_NONE;
            }
            }
        }

        public static Scheme fromString(final String schemeValStr) {
            if (DEF_DES_ENCRYPTION_SCHEME.equals(schemeValStr)) {
                return Scheme.DES;
            } else if (DEF_DES_EDE_ENCRYPTION_SCHEME.equals(schemeValStr)) {
                return Scheme.DES_EDE;
            } else if (DEF_AES_ENCRYPTION_SCHEME.equals(schemeValStr)) {
                return Scheme.AES;
            } else {
                return NONE;
            }
        }

        public static Scheme fromScheme(int schemeVal) {
            for (Scheme v : values()) {
                if (v.schemeVal == schemeVal) {
                    return v;
                }
            }
            return NONE;
        }
    }

    /**
     * Encrypting Modes (CBC / ECB / GCM + Padding / No Padding),
     * including definitions:
     * 
     *    - usage IV (init vector)
     *    - length of IV (init vector)
     * 
     * @author Sergey Manoylo
     * @version 2021-10-20
     */
    public enum Mode {

        NONE(-1, false, 0),

        DES_CBC_ENCRYPTION_MODE(0, true, DEF_IV_LEN_DES),           //  mode: "DES/CBC/PKCS5Padding", iv - 8 bytes
        DES_ECB_ENCRYPTION_MODE(1, false, 0),                       //  mode: "DES/ECB/PKCS5Padding", iv isn't used

        DES_EDE_CBC_ENCRYPTION_MODE(2, true, DEF_IV_LEN_DES),       //  mode: "DESede/CBC/PKCS5Padding", iv - 8 bytes 
        DES_EDE_ECB_ENCRYPTION_MODE(3, false, 0),                   //  mode: "DESede/ECB/PKCS5Padding", iv isn't used

        AES_CBC_ENCRYPTION_MODE(4, true, DEF_IV_LEN_AES),           //  mode:  "AES/CBC/PKCS5Padding", iv - 16 bytes
        AES_ECB_ENCRYPTION_MODE(5, false, 0 ),                      //  mode:  "AES/CBC/PKCS5Padding", iv - 16 bytes
        AES_GCM_ENCRYPTION_MODE(6, true, DEF_IV_LEN_AES);

        private final int modeVal;
        private final boolean useIv;
        private final int ivLength;     // length of Init Vector (IV) in bytes

        private Mode(final int modeVal, final boolean useIv, final int ivLength) {
            this.modeVal = modeVal;
            this.useIv= useIv;
            this.ivLength = ivLength;
        }

        public int getMode() {
            return modeVal;
        }

        public boolean getUseIv() {
            return useIv;
        }

        public int getIvLength() {
            return ivLength;
        }

        @Override
        public String toString() {
            switch (this) {
            case DES_CBC_ENCRYPTION_MODE: {
                return DEF_DES_CBC_ENCRYPTION_MODE;
            }
            case DES_ECB_ENCRYPTION_MODE: {
                return DEF_DES_ECB_ENCRYPTION_MODE;
            }
            case DES_EDE_CBC_ENCRYPTION_MODE: {
                return DEF_DES_EDE_CBC_ENCRYPTION_MODE;
            }
            case DES_EDE_ECB_ENCRYPTION_MODE: {
                return DEF_DES_EDE_ECB_ENCRYPTION_MODE;
            }
            case AES_GCM_ENCRYPTION_MODE: {
                return DEF_AES_GCM_ENCRYPTION_MODE;
            }
            case AES_CBC_ENCRYPTION_MODE: {
                return DEF_AES_CBC_ENCRYPTION_MODE;
            }
            case AES_ECB_ENCRYPTION_MODE: {
                return DEF_AES_ECB_ENCRYPTION_MODE;
            }
            default: {
                return DEF_NONE;
            }
            }
        }

        public static Mode fromString(final String modeValStr) {
            if (DEF_DES_CBC_ENCRYPTION_MODE.equals(modeValStr)) {
                return Mode.DES_CBC_ENCRYPTION_MODE;
            } else if (DEF_DES_ECB_ENCRYPTION_MODE.equals(modeValStr)) {
                return Mode.DES_ECB_ENCRYPTION_MODE;
            } else if (DEF_DES_EDE_CBC_ENCRYPTION_MODE.equals(modeValStr)) {
                return Mode.DES_EDE_CBC_ENCRYPTION_MODE;
            } else if (DEF_DES_EDE_ECB_ENCRYPTION_MODE.equals(modeValStr)) {
                return Mode.DES_EDE_ECB_ENCRYPTION_MODE;
            } else if (DEF_AES_GCM_ENCRYPTION_MODE.equals(modeValStr)) {
                return Mode.AES_GCM_ENCRYPTION_MODE;
            } else if (DEF_AES_CBC_ENCRYPTION_MODE.equals(modeValStr)) {
                return Mode.AES_CBC_ENCRYPTION_MODE;
            } else if (DEF_AES_ECB_ENCRYPTION_MODE.equals(modeValStr)) {
                return Mode.AES_ECB_ENCRYPTION_MODE;
            } else {
                return NONE;
            }
        }

        public static Mode fromMode(int modeVal) {
            for (Mode v : values()) {
                if (v.modeVal == modeVal) {
                    return v;
                }
            }
            return NONE;
        }
    }

    /**
     * Key Length (bits)
     * 
     * @author Sergey Manoylo
     * @version 2021-10-19
     */
    public enum KeyLength {

        NONE(-1),

        KL56(56),   // DES 56 bits

        KL112(112), // 3DES / DESede 112 bits
        KL168(168), // 3DES / DESede 168 bits

        KL128(128), // AES 128 bits
        KL192(192), // AES 192 bits
        KL256(256); // AES 256 bits

        private final int keyLengthVal;

        private KeyLength(final int keyKength) {
            this.keyLengthVal = keyKength;
        }

        public int getKeyLength() {
            return keyLengthVal;
        }

        public static KeyLength fromString(final String keyLength) {
            if (DEF_56.equals(keyLength)) {
                return KeyLength.KL56;
            } else if (DEF_112.equals(keyLength)) {
                return KeyLength.KL112;
            } else if (DEF_168.equals(keyLength)) {
                return KeyLength.KL168;
            } else if (DEF_128.equals(keyLength)) {
                return KeyLength.KL128;
            } else if (DEF_192.equals(keyLength)) {
                return KeyLength.KL192;
            } else if (DEF_256.equals(keyLength)) {
                return KeyLength.KL256;
            } else {
                return NONE;
            }
        }

        @Override
        public String toString() {
            if (keyLengthVal < 0) {
                return DEF_NONE;
            } else {
                return String.valueOf(keyLengthVal);
            }
        }

        public static KeyLength fromLength(int length) {
            for (KeyLength v : values()) {
                if (v.keyLengthVal == length) {
                    return v;
                }
            }
            return NONE;
        }
    }

    protected Scheme encScheme;         // encryption scheme

    protected Mode encMode;             // encryption mode

    protected KeyLength encKeyLength;   // key length (56, 112, 168, 128, 192, 256)

    protected String encPassw;          // password, that is used for generation the key
    
    private String encSalt;             // salt (randomization param), that is used for generation the key

    protected SecretKey secretKey;      // secret key, used during encryption/decryption, generated, during initializing the object

    public static StringEncrypter defaultInstance() throws EncryptionException {
        return Holder.INSTANCE;
    }

    public static StringEncrypter instance(final String encPassw) throws EncryptionException  {
        return new StringEncrypter(DEF_DES_EDE_ENCRYPTION_SCHEME, encPassw);
    }

    public static StringEncrypter instance(final String encPassw, final String encSalt, final String encAlg) throws EncryptionException  {
        return new StringEncrypter(encPassw, encSalt, encAlg);
    }
    
    /**
     * 
     */
    public StringEncrypter() { }

    /**
     * Constructor specifying scheme and key
     *
     * @param encScheme
     *            Encryption scheme to use
     * @param encPassw
     *            Encryption password to use
     * @throws io.jans.util.security.EncryptionException 
     * @throws Exception 
     */
    public StringEncrypter(final String encScheme, final String encPassw) throws EncryptionException {
        this(encScheme, null, null, encPassw,  null);
    }
    
    /**
     * Constructor specifying scheme and key
     * 
     * @param encScheme
     *              Encryption scheme to use (DES, DESede, AES)
     * @param encMode
     *              Encryption mode to use (AES/CBC/PKCS5Padding, AES/ECB/PKCS5Padding)
     * @param keyLengthVal
     *              Encryption keyLength (56, 168, 128, 192, 256)
     * @param encPassw
     *              Encryption key to use
     * @param encSalt
     *              Salt for randomizing encrypted key 
     * @throws EncryptionException 
     * @throws Exception 
     */
    public StringEncrypter(final String encPassw, final String encSalt, final String encAlg) throws EncryptionException {
        init(encPassw, encSalt, encAlg);
    }

    /**
     * Constructor specifying scheme and key
     * 
     * @param encScheme
     *              Encryption scheme to use (DES, DESede, AES)
     * @param encMode
     *              Encryption mode to use (AES/CBC/PKCS5Padding, AES/ECB/PKCS5Padding)
     * @param encKeyLength
     *              Encryption keyLength (56, 168, 128, 192, 256)
     * @param encPassw
     *              Encryption key to use
     * @param encSalt
     *              Salt for randomizing encrypted key 
     * @throws io.jans.util.security.EncryptionException 
     */
    public StringEncrypter(final String encScheme, final String encMode, final String encKeyLength, final String encPassw, final String encSalt) throws EncryptionException {
        init(encScheme, encMode, encKeyLength, encPassw, encSalt);
    }

    /**
     * Initializing function
     * 
     * @param encPassw
     *              Encryption key to use
     * @param encSalt
     *              Salt for randomizing encrypted key 
     * @param encAlg
     *              algorithm
     *                      in format AES:AES/CBC/PKCS5Padding:256
     *              or
     *                      in format DES
     * @throws EncryptionException 
     * @throws Exception 
     */
    public void init(final String encPassw, final String encSalt, final String encAlg) throws EncryptionException {
        if(encAlg == null || encAlg.length() == 0) {
            init(DEF_DES_EDE_ENCRYPTION_SCHEME, DEF_DES_EDE_ECB_ENCRYPTION_MODE, DEF_168, encSalt, null);
            return;
        }
        final String [] subAlgs = encAlg.split(":");
        if(subAlgs.length == 1 && subAlgs[0].equals(DEF_DES_ENCRYPTION_SCHEME)) {
            init(subAlgs[0], DEF_DES_ECB_ENCRYPTION_MODE, DEF_56, encPassw, encSalt);
        } else if (subAlgs.length == 1 && subAlgs[0].equals(DEF_DES_EDE_ENCRYPTION_SCHEME)) {
            init(subAlgs[0], DEF_DES_EDE_ECB_ENCRYPTION_MODE, DEF_168, encPassw, encSalt);
        } else if (subAlgs.length == 1 && subAlgs[0].equals(DEF_AES_ENCRYPTION_SCHEME)) {
            init(subAlgs[0], DEF_AES_GCM_ENCRYPTION_MODE, DEF_256, encPassw, encSalt);
        } else if (subAlgs.length == 3) {
            init(subAlgs[0], subAlgs[1], subAlgs[2], encPassw, encSalt);
        } else {
            throw new EncryptionException("wrong alg value: alg = " + encAlg);
        }
    }

    /**
     * Initializing function
     * 
     * @param encScheme
     *              Encryption scheme to use (DES, DESede, AES)
     * @param encMode
     *              Encryption mode to use (AES/CBC/PKCS5Padding, AES/ECB/PKCS5Padding)
     * @param encKeyLength
     *              Encryption keyLength (128, 192, 256)
     * @param encPassw
     *              Encryption key to use
     * @param encSalt
     *              Salt for randomizing encrypted key 
     * @throws EncryptionException
     */
    public void init(final String encSchemeStr, final String encModeStr, final String encKeyLengthStr, final String encPassw, final String encSalt) throws EncryptionException {

        this.encScheme = Scheme.fromString(encSchemeStr);
        this.encMode = Mode.fromString(encModeStr);
        this.encKeyLength = KeyLength.fromString(encKeyLengthStr);

        if(this.encScheme == null || this.encScheme == Scheme.NONE) {
            this.encScheme = Scheme.DES_EDE;
        }
        if(this.encMode == null || this.encMode == Mode.NONE) {
            this.encMode = Mode.DES_EDE_ECB_ENCRYPTION_MODE;
        }
        if(this.encKeyLength == null || this.encKeyLength == KeyLength.NONE) {
            this.encKeyLength = KeyLength.KL168;
        }

        this.encPassw = encPassw;
        this.encSalt = encSalt;
        if(this.encScheme == Scheme.DES || this.encScheme == Scheme.DES_EDE) {
            initDesScheme();
        } else if (this.encScheme == Scheme.AES) {
            initAesScheme();
        } else {
            throw new EncryptionException("wrong value of encScheme: " + this.encScheme);
        }
    }
    
    /**
     * Returns (getter) encScheme value
     * 
     * @return
     */
    public Scheme getEncScheme() {
        return encScheme;
    }

    /**
     * Returns (getter) encMode value
     * 
     * @return
     */
    public Mode getEncMode() {
        return encMode;
    }

    /**
     * Returns (getter) encKeyLength value
     * 
     * @return
     */
    public KeyLength getEncKeyLength() {
        return encKeyLength;
    }

    /**
     * Returns (getter) encPassw value
     * 
     * @return
     */
    public String getEncPassw() {
        return encPassw;
    }

    /**
     * Returns (getter) encSalt value
     * 
     * @return
     */
    public String getEncSalt() {
        return encSalt;
    }

    /**
     * Encrypt a string
     *
     * @param inString
     *          String to encrypt
     * @return Encrypted string (using scheme and key specified at construction)
     * @throws EncryptionException 
     */
    public String encrypt(final String inString) throws EncryptionException {
        return encrypt(inString, false);
    }
    
    /**
     * Encrypt a string
     * 
     * @param inString
     *          String to encrypt
     * @param silent
     *          In a case of error, exception will not be generated and inString will be returned
     * @return
     * @throws EncryptionException
     */
    public String encrypt(final String inString, boolean silent) throws EncryptionException {
        try {
            final Cipher cipher = Cipher.getInstance(encMode.toString(), DEF_CRYPTO_PROVIDER);
            final byte[] clearText = inString.getBytes(StandardCharsets.UTF_8);
            final byte[] resEncrText;
            if(encMode.getUseIv()) {
                SecureRandom random = new SecureRandom();
                byte[] ivArray = new byte[encMode.getIvLength()];
                random.nextBytes(ivArray);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(ivArray));
                final byte[] encrText = cipher.doFinal(clearText);
                resEncrText = Arrays.copyOf(ivArray, ivArray.length + encrText.length);
                System.arraycopy(encrText, 0, resEncrText, ivArray.length, encrText.length);
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                resEncrText = cipher.doFinal(clearText);
            }
            return Base64.encodeBase64String(resEncrText);
        } catch (Exception e) {
            if (silent) {
                return inString;
            }
            throw new EncryptionException(e);
        }
    }    

    /**
     * Decrypt a string encrypted with this encrypter
     *
     * @param encString
     *          Encrypted string
     * @return Decrypted string
     * @throws EncryptionException 
     */
    public String decrypt(final String encString) throws EncryptionException {
        return decrypt(encString, false);
    }

    /**
     * Decrypt a string encrypted with this encrypter
     * 
     * @param encString
     *          Encrypted string
     * @param silent
     *          In a case of error, exception will not be generated and inString will be returned
     * @return Decrypted string
     * @throws EncryptionException
     */
    @SuppressWarnings("java:S3329")    
    public String decrypt(final String encString, boolean silent) throws EncryptionException {
        try {
            final Cipher cipher = Cipher.getInstance(encMode.toString(), DEF_CRYPTO_PROVIDER);
            final byte[] resEncryptedText;
            if(encMode.getUseIv()) {
                final byte[] encryptedText = Base64.decodeBase64(encString.getBytes(StandardCharsets.UTF_8));
                final byte[] ivArray = Arrays.copyOfRange(encryptedText, 0, encMode.getIvLength());
                resEncryptedText = Arrays.copyOfRange(encryptedText, encMode.getIvLength(), encryptedText.length);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivArray));
            } else {
                resEncryptedText =  Base64.decodeBase64(encString.getBytes(StandardCharsets.UTF_8));
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
            }
            return bytes2String(cipher.doFinal(resEncryptedText));
        } catch (final Exception e) {
            if (silent) {
                return encString;
            }
            throw new EncryptionException(e);
        }
    }
    
    /**
     * Convert a byte stream to a string
     *
     * @param bytes
     *            Byte stream
     * @return String representation
     * @throws UnsupportedEncodingException 
     */
    private static String bytes2String(final byte[] bytes) throws UnsupportedEncodingException {
        return new String(bytes, Util.UTF8);
    }

    /**
     * Initializing function (DES and 3DES / DESede schemes)
     * 
     * @throws EncryptionException
     */
    private void initDesScheme() throws EncryptionException {
        final byte[] keyAsBytes = this.encPassw.getBytes(StandardCharsets.UTF_8);
        try {
            final KeySpec keySpec;
            if(this.encScheme == Scheme.DES) {
                keySpec = new DESKeySpec(keyAsBytes);
            } else if (this.encScheme == Scheme.DES_EDE) {
                keySpec = new DESedeKeySpec(keyAsBytes);
            } else {
                throw new EncryptionException("wrong value of encScheme: " + this.encScheme);
            }
            final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(this.encScheme.toString(), DEF_CRYPTO_PROVIDER);
            this.secretKey = keyFactory.generateSecret(keySpec);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    /**
     * Initializing function (AES scheme)
     * 
     * @throws EncryptionException
     */
    private void initAesScheme() throws EncryptionException {
        try {
            if(this.encSalt == null || this.encSalt.length() == 0) {
                this.encSalt = StringHelper.getRandomString(DEF_RND_STR_LEN);
            }
            final PBEKeySpec spec = new PBEKeySpec(this.encPassw.toCharArray(), this.encSalt.getBytes(), 1000, this.encKeyLength.getKeyLength());
            final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512", DEF_CRYPTO_PROVIDER);
            SecretKey curSecretKey = keyFactory.generateSecret(spec);
            if((curSecretKey.getEncoded().length * 8) != this.encKeyLength.getKeyLength()) {
                throw new IllegalArgumentException("wrong key length: " + (curSecretKey.getEncoded().length * 8));
            }
            this.secretKey = curSecretKey;
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

}
