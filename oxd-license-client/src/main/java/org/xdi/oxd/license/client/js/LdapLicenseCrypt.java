package org.xdi.oxd.license.client.js;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 01/10/2014
 */

@LdapEntry
@LdapObjectClass(values = {"top", "oxLicenseCrypt"})
public class LdapLicenseCrypt implements Serializable {

    @LdapDN
    private String dn;
    @LdapAttribute(name = "uniqueIdentifier")
    private String id;
    @LdapAttribute(name = "oxLicensePassword")
    private String licensePassword;
    @LdapAttribute(name = "oxPublicPassword")
    private String publicPassword;
    @LdapAttribute(name = "oxPrivatePassword")
    private String privatePassword;
    @LdapAttribute(name = "oxClientPrivateKey")
    private String clientPrivateKey;
    @LdapAttribute(name = "oxClientPublicKey")
    private String clientPublicKey;
    @LdapAttribute(name = "oxPublicKey")
    private String publicKey;
    @LdapAttribute(name = "oxPrivateKey")
    private String privateKey;
    @LdapAttribute(name = "oxName")
    private String name;

    public String getId() {
        return id;
    }

    public LdapLicenseCrypt setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public LdapLicenseCrypt setName(String name) {
        this.name = name;
        return this;
    }

    public String getDn() {
        return dn;
    }

    public LdapLicenseCrypt setDn(String dn) {
        this.dn = dn;
        return this;
    }

    public String getClientPrivateKey() {
        return clientPrivateKey;
    }

    public LdapLicenseCrypt setClientPrivateKey(String clientPrivateKey) {
        this.clientPrivateKey = clientPrivateKey;
        return this;
    }

    public String getClientPublicKey() {
        return clientPublicKey;
    }

    public LdapLicenseCrypt setClientPublicKey(String clientPublicKey) {
        this.clientPublicKey = clientPublicKey;
        return this;
    }

    public String getLicensePassword() {
        return licensePassword;
    }

    public LdapLicenseCrypt setLicensePassword(String licensePassword) {
        this.licensePassword = licensePassword;
        return this;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public LdapLicenseCrypt setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public String getPrivatePassword() {
        return privatePassword;
    }

    public LdapLicenseCrypt setPrivatePassword(String privatePassword) {
        this.privatePassword = privatePassword;
        return this;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public LdapLicenseCrypt setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    public String getPublicPassword() {
        return publicPassword;
    }

    public LdapLicenseCrypt setPublicPassword(String publicPassword) {
        this.publicPassword = publicPassword;
        return this;
    }

}
