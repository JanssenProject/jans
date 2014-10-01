package org.xdi.oxd.licenser.server.ldap;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 01/10/2014
 */

public class LdapLicenseCrypt {

    @LdapDN
    private String dn;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getClientPrivateKey() {
        return clientPrivateKey;
    }

    public void setClientPrivateKey(String clientPrivateKey) {
        this.clientPrivateKey = clientPrivateKey;
    }

    public String getClientPublicKey() {
        return clientPublicKey;
    }

    public void setClientPublicKey(String clientPublicKey) {
        this.clientPublicKey = clientPublicKey;
    }

    public String getLicensePassword() {
        return licensePassword;
    }

    public void setLicensePassword(String licensePassword) {
        this.licensePassword = licensePassword;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPrivatePassword() {
        return privatePassword;
    }

    public void setPrivatePassword(String privatePassword) {
        this.privatePassword = privatePassword;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicPassword() {
        return publicPassword;
    }

    public void setPublicPassword(String publicPassword) {
        this.publicPassword = publicPassword;
    }

}
