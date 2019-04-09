/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.operation.auth;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.digest.Crypt;
import org.gluu.util.StringHelper;
import org.gluu.util.security.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Perform authentication and password encryption
 */
public final class PasswordEncryptionHelper {

    private static final Logger LOG = LoggerFactory.getLogger(PasswordEncryptionHelper.class);

    private static final byte[] CRYPT_SALT_CHARS = StringHelper.getBytesUtf8("./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");

    private PasswordEncryptionHelper() {
    }

    /**
     * Get the algorithm from the stored password
     */
    public static PasswordEncryptionMethod findAlgorithm(String credentials) {
        return findAlgorithm(StringHelper.getBytesUtf8(credentials));
    }

    /**
     * Get the algorithm from the stored password
     */
    public static PasswordEncryptionMethod findAlgorithm(byte[] credentials) {
        if ((credentials == null) || (credentials.length == 0)) {
            return null;
        }

        if (credentials[0] == '{') {
            // Get the algorithm
            int pos = 1;

            while (pos < credentials.length) {
                if (credentials[pos] == '}') {
                    break;
                }

                pos++;
            }

            if (pos < credentials.length) {
                if (pos == 1) {
                    // We don't have an algorithm : return the credentials as is
                    return null;
                }

                String algorithm = StringHelper.toLowerCase(StringHelper.utf8ToString(credentials, 1, pos - 1));

                // Support for crypt additional encryption algorithms (e.g.
                // {crypt}$1$salt$ez2vlPGdaLYkJam5pWs/Y1)
                if (credentials.length > pos + 3 && credentials[pos + 1] == '$' && Character.isDigit(credentials[pos + 2])) {
                    if (credentials[pos + 3] == '$') {
                        algorithm += StringHelper.utf8ToString(credentials, pos + 1, 3);
                    } else if (credentials.length > pos + 4 && credentials[pos + 4] == '$') {
                        algorithm += StringHelper.utf8ToString(credentials, pos + 1, 4);
                    }
                }

                return PasswordEncryptionMethod.getMethod(algorithm);
            } else {
                // We don't have an algorithm
                return null;
            }
        } else {
            // No '{algo}' part
            return null;
        }
    }

    /**
     * @see #createStoragePassword(byte[], PasswordEncryptionMethod)
     */
    public static String createStoragePassword(String credentials, PasswordEncryptionMethod algorithm) {
        byte[] resultBytes = createStoragePassword(StringHelper.getBytesUtf8(credentials), algorithm);

        return StringHelper.utf8ToString(resultBytes);
    }

    /**
     * Create a hashed password in a format that can be stored in the server. If the
     * specified algorithm requires a salt then a random salt of 8 byte size is used
     */
    public static byte[] createStoragePassword(byte[] credentials, PasswordEncryptionMethod algorithm) {
        // Check plain text password
        if (algorithm == null) {
            return credentials;
        }

        byte[] salt;

        switch (algorithm) {
        case HASH_METHOD_SSHA:
        case HASH_METHOD_SSHA256:
        case HASH_METHOD_SSHA384:
        case HASH_METHOD_SSHA512:
        case HASH_METHOD_SMD5:
            // Use 8 byte salt always except for "crypt" which needs 2 byte salt
            salt = new byte[8];
            new SecureRandom().nextBytes(salt);
            break;

        case HASH_METHOD_PKCS5S2:
            // Use 16 byte salt for PKCS5S2
            salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            break;

        case HASH_METHOD_CRYPT:
            salt = generateCryptSalt(2);
            break;

        case HASH_METHOD_CRYPT_MD5:
        case HASH_METHOD_CRYPT_SHA256:
        case HASH_METHOD_CRYPT_SHA512:
            salt = generateCryptSalt(8);
            break;

        case HASH_METHOD_CRYPT_BCRYPT:
        case HASH_METHOD_CRYPT_BCRYPT_B:
            salt = StringHelper.getBytesUtf8(BCrypt.genSalt());
            break;

        default:
            salt = null;
        }

        byte[] hashedPassword = encryptPassword(credentials, algorithm, salt);
        StringBuilder sb = new StringBuilder();

        sb.append('{').append(StringHelper.toUpperCase(algorithm.getPrefix())).append('}');

        if (algorithm == PasswordEncryptionMethod.HASH_METHOD_CRYPT || algorithm == PasswordEncryptionMethod.HASH_METHOD_CRYPT_BCRYPT) {
            sb.append(StringHelper.utf8ToString(salt));
            sb.append(StringHelper.utf8ToString(hashedPassword));
        } else if (algorithm == PasswordEncryptionMethod.HASH_METHOD_CRYPT_MD5 || algorithm == PasswordEncryptionMethod.HASH_METHOD_CRYPT_SHA256
                || algorithm == PasswordEncryptionMethod.HASH_METHOD_CRYPT_SHA512) {
            sb.append(algorithm.getSubPrefix());
            sb.append(StringHelper.utf8ToString(salt));
            sb.append('$');
            sb.append(StringHelper.utf8ToString(hashedPassword));
        } else if (salt != null) {
            byte[] hashedPasswordWithSaltBytes = new byte[hashedPassword.length + salt.length];

            if (algorithm == PasswordEncryptionMethod.HASH_METHOD_PKCS5S2) {
                merge(hashedPasswordWithSaltBytes, salt, hashedPassword);
            } else {
                merge(hashedPasswordWithSaltBytes, hashedPassword, salt);
            }

            sb.append(String.valueOf(Base64.getEncoder().encodeToString(hashedPasswordWithSaltBytes)));
        } else {
            sb.append(String.valueOf(Base64.getEncoder().encodeToString(hashedPassword)));
        }

        return StringHelper.getBytesUtf8(sb.toString());
    }

    /**
     * Compare the credentials
     */
    public static boolean compareCredentials(String receivedCredentials, String storedCredentials) {
        return compareCredentials(StringHelper.getBytesUtf8(receivedCredentials), StringHelper.getBytesUtf8(storedCredentials));
    }

    /**
     * Compare the credentials
     */
    public static boolean compareCredentials(byte[] receivedCredentials, byte[] storedCredentials) {
        PasswordEncryptionMethod algorithm = findAlgorithm(storedCredentials);

        if (algorithm != null) {
            // Get the encrypted part of the stored password
            PasswordDetails passwordDetails = splitCredentials(storedCredentials);

            // Reuse the saltedPassword information to construct the encrypted password given by the user
            byte[] userPassword = encryptPassword(receivedCredentials, passwordDetails.getAlgorithm(), passwordDetails.getSalt());

            return compareBytes(userPassword, passwordDetails.getPassword());
        } else {
            return compareBytes(receivedCredentials, storedCredentials);
        }
    }

    /**
     * Compare two byte[] in a constant time. This is necessary because using an
     * Array.equals() is not Timing attack safe ([1], [2] and [3]), a breach that
     * can be exploited to break some hashes.
     *
     * [1] https://en.wikipedia.org/wiki/Timing_attack [2]
     * http://rdist.root.org/2009/05/28/timing-attack-in-google-keyczar-library/ [3]
     * https://cryptocoding.net/index.php/Coding_rules
     */
    private static boolean compareBytes(byte[] provided, byte[] stored) {
        if (stored == null) {
            return provided == null;
        } else if (provided == null) {
            return false;
        }

        // Now, compare the two passwords, using a constant time method
        if (stored.length != provided.length) {
            return false;
        }

        // Loop on *every* byte in both passwords, and at the end, if one char at least is different, return false
        int result = 0;

        for (int i = 0; i < stored.length; i++) {
            // If both bytes are equal, xor will be == 0, otherwise it will be != 0 and it will be result
            result |= (stored[i] ^ provided[i]);
        }

        return result == 0;
    }

    /**
     * Encrypts the given credentials based on the algorithm name and optional salt
     */
    private static byte[] encryptPassword(byte[] credentials, PasswordEncryptionMethod algorithm, byte[] salt) {
        switch (algorithm) {
        case HASH_METHOD_SHA:
        case HASH_METHOD_SSHA:
            return digest(PasswordEncryptionMethod.HASH_METHOD_SHA, credentials, salt);

        case HASH_METHOD_SHA256:
        case HASH_METHOD_SSHA256:
            return digest(PasswordEncryptionMethod.HASH_METHOD_SHA256, credentials, salt);

        case HASH_METHOD_SHA384:
        case HASH_METHOD_SSHA384:
            return digest(PasswordEncryptionMethod.HASH_METHOD_SHA384, credentials, salt);

        case HASH_METHOD_SHA512:
        case HASH_METHOD_SSHA512:
            return digest(PasswordEncryptionMethod.HASH_METHOD_SHA512, credentials, salt);

        case HASH_METHOD_MD5:
        case HASH_METHOD_SMD5:
            return digest(PasswordEncryptionMethod.HASH_METHOD_MD5, credentials, salt);

        case HASH_METHOD_CRYPT:
            String saltWithCrypted = Crypt.crypt(StringHelper.utf8ToString(credentials), StringHelper.utf8ToString(salt));
            String crypted = saltWithCrypted.substring(2);
            return StringHelper.getBytesUtf8(crypted);

        case HASH_METHOD_CRYPT_MD5:
        case HASH_METHOD_CRYPT_SHA256:
        case HASH_METHOD_CRYPT_SHA512:
            String saltWithCrypted2 = Crypt.crypt(StringHelper.utf8ToString(credentials), algorithm.getSubPrefix() + StringHelper.utf8ToString(salt));
            String crypted2 = saltWithCrypted2.substring(saltWithCrypted2.lastIndexOf('$') + 1);
            return StringHelper.getBytesUtf8(crypted2);

        case HASH_METHOD_CRYPT_BCRYPT:
        case HASH_METHOD_CRYPT_BCRYPT_B:
            String crypted3 = BCrypt.hashPw(StringHelper.utf8ToString(credentials), StringHelper.utf8ToString(salt));
            return StringHelper.getBytesUtf8(crypted3.substring(crypted3.length() - 31));

        case HASH_METHOD_PKCS5S2:
            return generatePbkdf2Hash(credentials, algorithm, salt);

        default:
            return credentials;
        }
    }

    /**
     * Compute the hashed password given an algorithm, the credentials and an optional salt.
     */
    private static byte[] digest(PasswordEncryptionMethod algorithm, byte[] password, byte[] salt) {
        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance(algorithm.getAlgorithm());
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }

        if (salt != null) {
            digest.update(password);
            digest.update(salt);
            return digest.digest();
        } else {
            return digest.digest(password);
        }
    }

    /**
     * Decompose the stored password in an algorithm, an eventual salt and the
     * password itself.
     *
     * If the algorithm is SHA, SSHA, MD5 or SMD5, the part following the algorithm
     * is base64 encoded
     *
     * @param credentials
     *            The byte[] containing the credentials to split
     * @return The password
     */
    public static PasswordDetails splitCredentials(byte[] credentials) {
        PasswordEncryptionMethod algorithm = findAlgorithm(credentials);

        // check plain text password
        if (algorithm == null) {
            return new PasswordDetails(null, null, credentials);
        }

        int algoLength = algorithm.getPrefix().length() + 2;
        byte[] password;

        switch (algorithm) {
        case HASH_METHOD_MD5:
        case HASH_METHOD_SMD5:
            return getCredentials(credentials, algoLength, algorithm.getHashLength(), algorithm);

        case HASH_METHOD_SHA:
        case HASH_METHOD_SSHA:
            return getCredentials(credentials, algoLength, algorithm.getHashLength(), algorithm);

        case HASH_METHOD_SHA256:
        case HASH_METHOD_SSHA256:
            return getCredentials(credentials, algoLength, algorithm.getHashLength(), algorithm);

        case HASH_METHOD_SHA384:
        case HASH_METHOD_SSHA384:
            return getCredentials(credentials, algoLength, algorithm.getHashLength(), algorithm);

        case HASH_METHOD_SHA512:
        case HASH_METHOD_SSHA512:
            return getCredentials(credentials, algoLength, algorithm.getHashLength(), algorithm);

        case HASH_METHOD_PKCS5S2:
            return getPbkdf2Credentials(credentials, algoLength, algorithm);

        case HASH_METHOD_CRYPT:
            // The password is associated with a salt. Decompose it
            // in two parts, no decoding required.
            // The salt comes first, not like for SSHA and SMD5, and is 2 bytes long
            // The algorithm, salt, and password will be stored into the PasswordDetails
            // structure.
            byte[] salt = new byte[2];
            password = new byte[credentials.length - salt.length - algoLength];
            split(credentials, algoLength, salt, password);
            return new PasswordDetails(algorithm, salt, password);

        case HASH_METHOD_CRYPT_BCRYPT:
        case HASH_METHOD_CRYPT_BCRYPT_B:
            salt = Arrays.copyOfRange(credentials, algoLength, credentials.length - 31);
            password = Arrays.copyOfRange(credentials, credentials.length - 31, credentials.length);

            return new PasswordDetails(algorithm, salt, password);
        case HASH_METHOD_CRYPT_MD5:
        case HASH_METHOD_CRYPT_SHA256:
        case HASH_METHOD_CRYPT_SHA512:
            // skip $x$
            algoLength = algoLength + 3;
            return getCryptCredentials(credentials, algoLength, algorithm);

        default:
            // unknown method
            throw new IllegalArgumentException("Unknown hash algorithm " + algorithm);
        }
    }

    /**
     * Compute the credentials
     */
    private static PasswordDetails getCredentials(byte[] credentials, int algoLength, int hashLen, PasswordEncryptionMethod algorithm) {
        // The password is associated with a salt. Decompose it in two parts, after having decoded the password.
        // The salt is at the end of the credentials.
        // The algorithm, salt, and password will be stored into the PasswordDetails structure
        byte[] passwordAndSalt = Base64.getDecoder().decode(StringHelper.utf8ToString(credentials, algoLength, credentials.length - algoLength));

        int saltLength = passwordAndSalt.length - hashLen;
        byte[] salt = saltLength == 0 ? null : new byte[saltLength];
        byte[] password = new byte[hashLen];
        split(passwordAndSalt, 0, password, salt);

        return new PasswordDetails(algorithm, salt, password);
    }

    private static void split(byte[] all, int offset, byte[] left, byte[] right) {
        System.arraycopy(all, offset, left, 0, left.length);
        if (right != null) {
            System.arraycopy(all, offset + left.length, right, 0, right.length);
        }
    }

    private static void merge(byte[] all, byte[] left, byte[] right) {
        System.arraycopy(left, 0, all, 0, left.length);
        System.arraycopy(right, 0, all, left.length, right.length);
    }

    /**
     * Generates a hash based on the
     * <a href="http://en.wikipedia.org/wiki/PBKDF2">PKCS5S2 spec</a>
     */
    private static byte[] generatePbkdf2Hash(byte[] credentials, PasswordEncryptionMethod algorithm, byte[] salt) {
        try {
            SecretKeyFactory sk = SecretKeyFactory.getInstance(algorithm.getAlgorithm());
            char[] password = StringHelper.utf8ToString(credentials).toCharArray();
            KeySpec keySpec = new PBEKeySpec(password, salt, 10000, algorithm.getHashLength() * 8);
            Key key = sk.generateSecret(keySpec);
            return key.getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the credentials from a PKCS5S2 hash. The salt for PKCS5S2 hash is
     * prepended to the password
     */
    private static PasswordDetails getPbkdf2Credentials(byte[] credentials, int algoLength, PasswordEncryptionMethod algorithm) {
        // The password is associated with a salt. Decompose it in two parts, after having decoded the password.
        // The salt is at the *beginning* of the credentials, and is 16 bytes long
        // The algorithm, salt, and password will be stored into the PasswordDetails structure
        byte[] passwordAndSalt = Base64.getDecoder().decode(StringHelper.utf8ToString(credentials, algoLength, credentials.length - algoLength));

        int saltLength = passwordAndSalt.length - algorithm.getHashLength();
        byte[] salt = new byte[saltLength];
        byte[] password = new byte[algorithm.getHashLength()];

        split(passwordAndSalt, 0, salt, password);

        return new PasswordDetails(algorithm, salt, password);
    }

    private static byte[] generateCryptSalt(int length) {
        byte[] salt = new byte[length];
        SecureRandom sr = new SecureRandom();
        for (int i = 0; i < salt.length; i++) {
            salt[i] = CRYPT_SALT_CHARS[sr.nextInt(CRYPT_SALT_CHARS.length)];
        }

        return salt;
    }

    private static PasswordDetails getCryptCredentials(byte[] credentials, int algoLength, PasswordEncryptionMethod algorithm) {
        // The password is associated with a salt. Decompose it in two parts, no decoding required.
        // The salt length is dynamic, between the 2nd and 3rd '$'.
        // The algorithm, salt, and password will be stored into the PasswordDetails structure.

        // Skip {crypt}$x$
        int pos = algoLength;
        while (pos < credentials.length) {
            if (credentials[pos] == '$') {
                break;
            }

            pos++;
        }

        byte[] salt = Arrays.copyOfRange(credentials, algoLength, pos);
        byte[] password = Arrays.copyOfRange(credentials, pos + 1, credentials.length);

        return new PasswordDetails(algorithm, salt, password);
    }

}
