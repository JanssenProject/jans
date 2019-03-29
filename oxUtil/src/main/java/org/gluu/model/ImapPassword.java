package org.gluu.model;

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
