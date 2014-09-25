package org.xdi.oxd.licenser.server.ldap;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

@LdapEntry
@LdapObjectClass(values = {"top", "oxLicenseCustomer"})
public class LdapCustomer {

    @LdapDN
    private String dn;
    @LdapAttribute(name = "customerId")
    private String id;
    @LdapAttribute(name = "licenseId")
    private String licenseId;
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
    @LdapAttribute(name = "oxLicense")
    private List<String> licenses;

    public List<String> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<String> licenses) {
        this.licenses = licenses;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
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

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LdapCustomer");
        sb.append("{clientPrivateKey='").append(clientPrivateKey).append('\'');
        sb.append(", dn='").append(dn).append('\'');
        sb.append(", licenseId='").append(licenseId).append('\'');
        sb.append(", licensePassword='").append(licensePassword).append('\'');
        sb.append(", publicPassword='").append(publicPassword).append('\'');
        sb.append(", privatePassword='").append(privatePassword).append('\'');
        sb.append(", clientPublicKey='").append(clientPublicKey).append('\'');
        sb.append(", publicKey='").append(publicKey).append('\'');
        sb.append(", privateKey='").append(privateKey).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public String getName() {
        return name;
    }
}
