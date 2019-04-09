/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.persist.operation.auth;

/**
 * Password encryption methods
 */
public enum PasswordEncryptionMethod {
    /** The SHA encryption method */
    HASH_METHOD_SHA("SHA", "SHA", "sha", 20),

    /** The Salted SHA encryption method */
    HASH_METHOD_SSHA("SSHA", "SHA", "ssha", 20),

    /** The SHA-256 encryption method */
    HASH_METHOD_SHA256("SHA-256", "SHA-256", "sha256", 32),

    /** The salted SHA-256 encryption method */
    HASH_METHOD_SSHA256("SSHA-256", "SHA-256", "ssha256", 32),

    /** The SHA-384 encryption method */
    HASH_METHOD_SHA384("SHA-384", "SHA-384", "sha384", 48),

    /** The salted SHA-384 encryption method */
    HASH_METHOD_SSHA384("SSHA-384", "SHA-384", "ssha384", 48),

    /** The SHA-512 encryption method */
    HASH_METHOD_SHA512("SHA-512", "SHA-512", "sha512", 64),

    /** The salted SHA-512 encryption method */
    HASH_METHOD_SSHA512("SSHA-512", "SHA-512", "ssha512", 64),

    /** The MD5 encryption method */
    HASH_METHOD_MD5("MD5", "MD5", "md5", 16),

    /** The Salter MD5 encryption method */
    HASH_METHOD_SMD5("SMD5", "MD5", "smd5", 16),

    /** The crypt encryption method */
    HASH_METHOD_CRYPT("CRYPT", "CRYPT", "crypt", 11),

    /** The crypt (MD5) encryption method */
    HASH_METHOD_CRYPT_MD5("CRYPT-MD5", "MD5", "crypt", "$1$", 22),

    /** The crypt (SHA-256) encryption method */
    HASH_METHOD_CRYPT_SHA256("CRYPT-SHA-256", "SHA-256", "crypt", "$5$", 43),

    /** The crypt (SHA-512) encryption method */
    HASH_METHOD_CRYPT_SHA512("CRYPT-SHA-512", "SHA-512", "crypt", "$6$", 86),

    /** The BCrypt encryption method */
    HASH_METHOD_CRYPT_BCRYPT("CRYPT-BCRYPT", "BCRYPT", "crypt", "$2a$", 31),

    /** The BCrypt encryption method */
    HASH_METHOD_CRYPT_BCRYPT_B("CRYPT-BCRYPT", "BCRYPT", "bcrypt", "$2b$", 31),

    /** The PBKDF2-based encryption method */
    HASH_METHOD_PKCS5S2("PKCS5S2", "PBKDF2WithHmacSHA1", "PKCS5S2", 32);

    /** The associated name */
    private final String name;

    /** The associated algorithm */
    private final String algorithm;

    /** The associated prefix */
    private final String prefix;

    /** The optional sub-prefix */
    private final String subPrefix;

    /** Hash length */
    private final int hashLength;

    PasswordEncryptionMethod(String name, String algorithm, String prefix, int hashLength) {
        this(name, algorithm, prefix, "", hashLength);
    }

    PasswordEncryptionMethod(String name, String algorithm, String prefix, String subPrefix, int hashLength) {
        this.name = name;
        this.algorithm = algorithm;
        this.prefix = prefix;
        this.subPrefix = subPrefix;
        this.hashLength = hashLength;
    }

    public String getName() {
        return name;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSubPrefix() {
        return subPrefix;
    }

    public final int getHashLength() {
        return hashLength;
    }

    /**
     * Get the associated constant from a string
     */
    public static PasswordEncryptionMethod getMethod(String algorithm) {
        if (matches(algorithm, HASH_METHOD_SHA)) {
            return HASH_METHOD_SHA;
        }

        if (matches(algorithm, HASH_METHOD_SSHA)) {
            return HASH_METHOD_SSHA;
        }
        if (matches(algorithm, HASH_METHOD_MD5)) {
            return HASH_METHOD_MD5;
        }

        if (matches(algorithm, HASH_METHOD_SMD5)) {
            return HASH_METHOD_SMD5;
        }

        if (matches(algorithm, HASH_METHOD_CRYPT)) {
            return HASH_METHOD_CRYPT;
        }

        if (matches(algorithm, HASH_METHOD_CRYPT_MD5)) {
            return HASH_METHOD_CRYPT_MD5;
        }

        if (matches(algorithm, HASH_METHOD_CRYPT_SHA256)) {
            return HASH_METHOD_CRYPT_SHA256;
        }

        if (matches(algorithm, HASH_METHOD_CRYPT_SHA512)) {
            return HASH_METHOD_CRYPT_SHA512;
        }

        if (matches(algorithm, HASH_METHOD_CRYPT_BCRYPT)) {
            return HASH_METHOD_CRYPT_BCRYPT;
        }

        if (matches(algorithm, HASH_METHOD_CRYPT_BCRYPT_B)) {
            return HASH_METHOD_CRYPT_BCRYPT_B;
        }

        if (matches(algorithm, HASH_METHOD_SHA256)) {
            return HASH_METHOD_SHA256;
        }

        if (matches(algorithm, HASH_METHOD_SSHA256)) {
            return HASH_METHOD_SSHA256;
        }

        if (matches(algorithm, HASH_METHOD_SHA384)) {
            return HASH_METHOD_SHA384;
        }

        if (matches(algorithm, HASH_METHOD_SSHA384)) {
            return HASH_METHOD_SSHA384;
        }

        if (matches(algorithm, HASH_METHOD_SHA512)) {
            return HASH_METHOD_SHA512;
        }

        if (matches(algorithm, HASH_METHOD_SSHA512)) {
            return HASH_METHOD_SSHA512;
        }

        if (matches(algorithm, HASH_METHOD_PKCS5S2)) {
            return HASH_METHOD_PKCS5S2;
        }

        return null;
    }

    private static boolean matches(String algorithm, PasswordEncryptionMethod constant) {
        return constant.name.equalsIgnoreCase(algorithm) || (constant.prefix + constant.subPrefix).equalsIgnoreCase(algorithm);
    }

}
