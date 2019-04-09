/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.operation.auth;

/**
 * Store informations about an password
 */
public class PasswordDetails {
    private final PasswordEncryptionMethod algorithm;
    private final byte[] salt;
    private final byte[] password;

    /**
     * Creates a new PasswordDetails instance
     */
    public PasswordDetails(PasswordEncryptionMethod algorithm, byte[] salt, byte[] password) {
        this.algorithm = algorithm;
        this.salt = salt;
        this.password = password;
    }

    /**
     * The hash algorithm used to hash the password, null for plain text passwords
     */
    public PasswordEncryptionMethod getAlgorithm() {
        return algorithm;
    }

    /**
     * The salt used to hash the password, null if no salt was used
     */
    public byte[] getSalt() {
        return salt;
    }

    /**
     * The hashed or plain text password
     */
    public byte[] getPassword() {
        return password;
    }

}
