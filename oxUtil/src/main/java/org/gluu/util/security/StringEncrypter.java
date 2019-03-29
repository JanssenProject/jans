/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.security;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.gluu.util.Util;

/**
 * Encryption algorithms
 *
 * @author ssudala
 */
public class StringEncrypter {

    private static final Logger LOG = Logger.getLogger(StringEncrypter.class);

    private final ReentrantLock lock = new ReentrantLock();

    // lazy init via static holder
    private static class Holder {
        static final StringEncrypter INSTANCE = createInstance();

        private static StringEncrypter createInstance() {
            try {
                return new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME);
            } catch (EncryptionException e) {
                LOG.error("Failed to create default StringEncrypter instance", e);
                return null;
            }
        }
    }

    public StringEncrypter() { }

    public static StringEncrypter defaultInstance() throws EncryptionException {
        return Holder.INSTANCE;
    }

    public static StringEncrypter instance(String encryptionKey) throws EncryptionException {
        return new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME, encryptionKey);
    }

    /**
     * Exception thrown from encryption failures
     *
     * @author ssudala
     */
    public static class EncryptionException extends Exception {
        /**
         * Serial UID
         */
        private static final long serialVersionUID = -7220454928814292801L;

        /**
         * Default constructor
         *
         * @param t
         *            Wrapped exception
         */
        public EncryptionException(final Throwable t) {
            super(t);
        }
    }

    /**
     * Default encryption key
     */
    public static final String DEFAULT_ENCRYPTION_KEY = "This is a fairly long phrase used to encrypt";

    /**
     * DES encryption scheme
     */
    public static final String DES_ENCRYPTION_SCHEME = "DES";

    /**
     * Desede encryption scheme
     */
    public static final String DESEDE_ENCRYPTION_SCHEME = "DESede";

    /**
     * Unicode format
     */
    private static final String UNICODE_FORMAT = "UTF8";

    /**
     * Convert a byte stream to a string
     *
     * @param bytes
     *            Byte stream
     * @return String representation
     */
    private static String bytes2String(final byte[] bytes) {
        final StringBuffer stringBuffer = new StringBuffer();
        for (final byte element : bytes) {
            stringBuffer.append((char) element);
        }
        return stringBuffer.toString();
    }

    /**
     * Cipher being used
     */
    private Cipher cipher;

    /**
     * Key factory being used
     */
    private SecretKeyFactory keyFactory;

    /**
     * Key spec being used
     */
    private KeySpec keySpec;

    private Base64 base64 = new Base64();

    /**
     * Constructor specifying scheme and key
     *
     * @param encryptionScheme
     *            Encryption scheme to use
     * @param encryptionKey
     *            Encryption key to use
     * @throws EncryptionException
     */
    public StringEncrypter(final String encryptionScheme) throws EncryptionException {
        try {
            keyFactory = SecretKeyFactory.getInstance(encryptionScheme);
            cipher = Cipher.getInstance(encryptionScheme);
        } catch (final NoSuchAlgorithmException e) {
            throw new EncryptionException(e);
        } catch (final NoSuchPaddingException e) {
            throw new EncryptionException(e);
        }
    }

    /**
     * Constructor specifying scheme and key
     *
     * @param encryptionScheme
     *            Encryption scheme to use
     * @param encryptionKey
     *            Encryption key to use
     * @throws EncryptionException
     */
    public StringEncrypter(final String encryptionScheme, final String encryptionKey) throws EncryptionException {
        if (encryptionKey == null) {
            throw new IllegalArgumentException("encryption key was null");
        }
        if (encryptionKey.trim().length() < 24) {
            throw new IllegalArgumentException("encryption key was less than 24 characters");
        }

        try {
            final byte[] keyAsBytes = encryptionKey.getBytes(StringEncrypter.UNICODE_FORMAT);

            if (encryptionScheme.equalsIgnoreCase(StringEncrypter.DESEDE_ENCRYPTION_SCHEME)) {
                keySpec = new DESedeKeySpec(keyAsBytes);
            } else if (encryptionScheme.equalsIgnoreCase(StringEncrypter.DES_ENCRYPTION_SCHEME)) {
                keySpec = new DESKeySpec(keyAsBytes);
            } else {
                throw new IllegalArgumentException("Encryption scheme not supported: " + encryptionScheme);
            }

            keyFactory = SecretKeyFactory.getInstance(encryptionScheme);
            cipher = Cipher.getInstance(encryptionScheme);
        } catch (final InvalidKeyException e) {
            throw new EncryptionException(e);
        } catch (final UnsupportedEncodingException e) {
            throw new EncryptionException(e);
        } catch (final NoSuchAlgorithmException e) {
            throw new EncryptionException(e);
        } catch (final NoSuchPaddingException e) {
            throw new EncryptionException(e);
        }
    }

    private String decrypt(final String encryptedString, KeySpec keySpec, boolean silent) throws EncryptionException {
        if (keySpec == null) {
            throw new IllegalArgumentException("keySpec was null or empty");
        }

        if ((encryptedString == null) || (encryptedString.trim().length() <= 0)) {
            throw new IllegalArgumentException("encrypted string was null or empty");
        }

        try {
            final SecretKey key = keyFactory.generateSecret(keySpec);
            cipher.init(Cipher.DECRYPT_MODE, key);

            final byte[] cleartext = base64.decode(encryptedString.getBytes(Util.UTF8));
            final byte[] ciphertext = cipher.doFinal(cleartext);

            return StringEncrypter.bytes2String(ciphertext);
        } catch (final Exception e) {
            if (silent) {
                return encryptedString;
            }

            throw new EncryptionException(e);
        }
    }

    /**
     * Decrypt a string encrypted with this encrypter
     *
     * @param encryptedString
     *            Encrypted string
     * @return Decrypted string
     * @throws EncryptionException
     */
    public String decrypt(final String encryptedString) throws EncryptionException {
        return decrypt(encryptedString, false);
    }

    public String decrypt(final String encryptedString, boolean silent) throws EncryptionException {
        lock.lock();
        try {
            return decrypt(encryptedString, keySpec, silent);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Decrypt a string encrypted with this encrypter
     *
     * @param encryptedString
     *            Encrypted string
     * @return Decrypted string
     * @throws EncryptionException
     */
    public String decrypt(final String encryptedString, String encryptionKey) throws EncryptionException {
        return decrypt(encryptedString, encryptionKey, false);
    }

    public String decrypt(final String encryptedString, String encryptionKey, boolean silent) throws EncryptionException {
        lock.lock();
        try {
            final byte[] keyAsBytes = encryptionKey.getBytes(StringEncrypter.UNICODE_FORMAT);
            String encryptionScheme = StringEncrypter.DESEDE_ENCRYPTION_SCHEME;
            KeySpec keySpec;
            if (encryptionScheme.equalsIgnoreCase(StringEncrypter.DESEDE_ENCRYPTION_SCHEME)) {
                keySpec = new DESedeKeySpec(keyAsBytes);
            } else if (encryptionScheme.equalsIgnoreCase(StringEncrypter.DES_ENCRYPTION_SCHEME)) {
                keySpec = new DESKeySpec(keyAsBytes);
            } else {
                throw new IllegalArgumentException("Encryption scheme not supported: " + encryptionScheme);
            }
            return decrypt(encryptedString, keySpec, silent);
        } catch (final Exception e) {
            throw new EncryptionException(e);
        } finally {
            lock.unlock();
        }
    }

    private String encrypt(final String unencryptedString, KeySpec keySpec) throws EncryptionException {
        if (keySpec == null) {
            throw new IllegalArgumentException("keySpec was null or empty");
        }

        if ((unencryptedString == null) || (unencryptedString.trim().length() == 0)) {
            throw new IllegalArgumentException("unencrypted string was null or empty");
        }

        try {
            final SecretKey key = keyFactory.generateSecret(keySpec);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            final byte[] cleartext = unencryptedString.getBytes(StringEncrypter.UNICODE_FORMAT);
            final byte[] ciphertext = cipher.doFinal(cleartext);

            return new String(base64.encode(ciphertext), Util.UTF8);
        } catch (final Exception e) {
            throw new EncryptionException(e);
        }
    }

    /**
     * Encrypt a string
     *
     * @param unencryptedString
     *            String to encrypt
     * @return Encrypted string (using scheme and key specified at construction)
     * @throws EncryptionException
     */
    public String encrypt(final String unencryptedString) throws EncryptionException {
        lock.lock();
        try {
            return encrypt(unencryptedString, keySpec);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Encrypt a string
     *
     * @param unencryptedString
     *            String to encrypt
     * @return Encrypted string (using scheme and key specified at construction)
     * @throws EncryptionException
     */
    public String encrypt(final String unencryptedString, String encryptionKey) throws EncryptionException {
        lock.lock();
        try {
            final byte[] keyAsBytes = encryptionKey.getBytes(StringEncrypter.UNICODE_FORMAT);
            String encryptionScheme = StringEncrypter.DESEDE_ENCRYPTION_SCHEME;
            KeySpec keySpec;
            if (encryptionScheme.equalsIgnoreCase(StringEncrypter.DESEDE_ENCRYPTION_SCHEME)) {
                keySpec = new DESedeKeySpec(keyAsBytes);
            } else if (encryptionScheme.equalsIgnoreCase(StringEncrypter.DES_ENCRYPTION_SCHEME)) {
                keySpec = new DESKeySpec(keyAsBytes);
            } else {
                throw new IllegalArgumentException("Encryption scheme not supported: " + encryptionScheme);
            }

            return encrypt(unencryptedString, keySpec);
        } catch (final Exception e) {
            throw new EncryptionException(e);
        } finally {
            lock.unlock();
        }
    }

    /*
     * private String decrypt2(final String password){ final String encryptionKey =
     * "123456789012345678901234567890"; final String encryptionScheme =
     * StringEncrypter.DESEDE_ENCRYPTION_SCHEME;
     *
     * try { final StringEncrypter encrypter = new StringEncrypter(
     * encryptionScheme, encryptionKey ); return encrypter.decrypt(password); }
     * catch (final EncryptionException e) { e.printStackTrace(); } return
     * "invalidpass"; }
     */
}
