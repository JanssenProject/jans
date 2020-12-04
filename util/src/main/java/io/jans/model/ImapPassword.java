/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model;

/**
 * Hold ImapPassword
 *
 * @author Shekhar L
 */

public class ImapPassword implements java.io.Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 2631677803214869609L;

    private String encryptedString;

    private String cipher;

    private String mode;

    public String getEncryptedString() {
        return encryptedString;
    }

    public void setEncryptedString(String encryptedString) {
        this.encryptedString = encryptedString;
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

}
