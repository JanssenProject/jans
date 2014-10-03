package org.xdi.oxd.license.client.js;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

import java.io.Serializable;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

@LdapEntry
@LdapObjectClass(values = {"top", "oxLicenseCustomer"})
public class LdapCustomer implements Serializable {

    @LdapDN
    private String dn;
    @LdapAttribute(name = "customerId")
    private String id;
    @LdapAttribute(name = "licenseId")
    private List<String> licenseIdDN;
    @LdapAttribute(name = "oxName")
    private String name;
    @LdapAttribute(name = "oxLicenseCrypt")
    private String licenseCryptDN;

    public String getLicenseCryptDN() {
        return licenseCryptDN;
    }

    public void setLicenseCryptDN(String licenseCryptDN) {
        this.licenseCryptDN = licenseCryptDN;
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


    public List<String> getLicenseIdDN() {
        return licenseIdDN;
    }

    public void setLicenseIdDN(List<String> licenseIdDN) {
        this.licenseIdDN = licenseIdDN;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LdapCustomer");
        sb.append("{licenseCryptDN='").append(licenseCryptDN).append('\'');
        sb.append(", dn='").append(dn).append('\'');
        sb.append(", licenseIdDN='").append(licenseIdDN).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public String getName() {
        return name;
    }
}
