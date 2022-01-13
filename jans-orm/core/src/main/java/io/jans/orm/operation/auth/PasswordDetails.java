/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.operation.auth;

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
