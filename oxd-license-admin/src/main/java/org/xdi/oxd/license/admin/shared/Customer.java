package org.xdi.oxd.license.admin.shared;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class Customer implements Serializable {

    private String id;
    private String name;
    private String privateKey;
    private String publicKey;
    private String privatePassword;
    private String publicPassword;
    private String licensePassword;

    public Customer() {
    }

    public String getId() {
        return id;
    }

    public Customer setId(String id) {
        this.id = id;
        return this;
    }

    public String getLicensePassword() {
        return licensePassword;
    }

    public Customer setLicensePassword(String licensePassword) {
        this.licensePassword = licensePassword;
        return this;
    }

    public String getName() {
        return name;
    }

    public Customer setName(String name) {
        this.name = name;
        return this;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public Customer setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public String getPrivatePassword() {
        return privatePassword;
    }

    public Customer setPrivatePassword(String privatePassword) {
        this.privatePassword = privatePassword;
        return this;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public Customer setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    public String getPublicPassword() {
        return publicPassword;
    }

    public Customer setPublicPassword(String publicPassword) {
        this.publicPassword = publicPassword;
        return this;
    }
}
