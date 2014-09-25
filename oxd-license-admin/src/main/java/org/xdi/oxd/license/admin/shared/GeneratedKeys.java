package org.xdi.oxd.license.admin.shared;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 25/09/2014
 */

public class GeneratedKeys implements Serializable {

    private String privateKey;
    private String publicKey;
    private String clientPrivateKey;
    private String clientPublicKey;
    private String privatePassword;
    private String publicPassword;
    private String licensePassword;

    public String getClientPrivateKey() {
        return clientPrivateKey;
    }

    public GeneratedKeys setClientPrivateKey(String clientPrivateKey) {
        this.clientPrivateKey = clientPrivateKey;
        return this;
    }

    public String getClientPublicKey() {
        return clientPublicKey;
    }

    public GeneratedKeys setClientPublicKey(String clientPublicKey) {
        this.clientPublicKey = clientPublicKey;
        return this;
    }

    public String getLicensePassword() {
        return licensePassword;
    }

    public GeneratedKeys setLicensePassword(String licensePassword) {
        this.licensePassword = licensePassword;
        return this;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public GeneratedKeys setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public String getPrivatePassword() {
        return privatePassword;
    }

    public GeneratedKeys setPrivatePassword(String privatePassword) {
        this.privatePassword = privatePassword;
        return this;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public GeneratedKeys setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    public String getPublicPassword() {
        return publicPassword;
    }

    public GeneratedKeys setPublicPassword(String publicPassword) {
        this.publicPassword = publicPassword;
        return this;
    }
}
