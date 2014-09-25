package org.xdi.oxd.license.admin.shared;

import java.io.Serializable;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class Customer implements Serializable {

    private String dn;
    private String id;
    private String name;
    private String privateKey;
    private String publicKey;
    private String privatePassword;
    private String publicPassword;
    private String licensePassword;
    private String clientPrivateKey;
    private String clientPublicKey;
    private List<License> licenses;

    public List<License> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    public Customer() {
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientPrivateKey() {
        return clientPrivateKey;
    }

    public Customer setClientPrivateKey(String clientPrivateKey) {
        this.clientPrivateKey = clientPrivateKey;
        return this;
    }

    public String getClientPublicKey() {
        return clientPublicKey;
    }

    public Customer setClientPublicKey(String clientPublicKey) {
        this.clientPublicKey = clientPublicKey;
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
