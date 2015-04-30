package org.xdi.oxd.license.client.js;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

import java.io.Serializable;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/09/2014
 */

@LdapEntry
@LdapObjectClass(values = {"top", "oxLicenseId"})
public class LdapLicenseId implements Serializable {

    @LdapDN
    private String dn;
    @LdapAttribute(name = "licenseId")
    private String licenseId;
    @LdapAttribute(name = "oxLicenseMetadata")
    private String metadata;
    @LdapAttribute(name = "oxLicense")
    private List<String> licenses;
    @LdapAttribute(name = "oxLicenseCrypt")
    private String licenseCryptDN;
    @LdapAttribute(name = "oxLicensesIssuedCount")
    private Integer licensesIssuedCount = 0;
    @LdapAttribute(name = "forceLicenseUpdate")
    private Boolean forceLicenseUpdate = false;

    private LicenseMetadata metadataAsObject;

    public LicenseMetadata getMetadataAsObject() {
        return metadataAsObject;
    }

    public Boolean getForceLicenseUpdate() {
        return forceLicenseUpdate;
    }

    public void setForceLicenseUpdate(Boolean forceLicenseUpdate) {
        this.forceLicenseUpdate = forceLicenseUpdate;
    }

    public Integer getLicensesIssuedCount() {
        return licensesIssuedCount;
    }

    public void setLicensesIssuedCount(Integer licensesIssuedCount) {
        this.licensesIssuedCount = licensesIssuedCount;
    }

    public void setMetadataAsObject(LicenseMetadata metadataAsObject) {
        this.metadataAsObject = metadataAsObject;
    }

    public String getLicenseCryptDN() {
        return licenseCryptDN;
    }

    public void setLicenseCryptDN(String licenseCryptDN) {
        this.licenseCryptDN = licenseCryptDN;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public List<String> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<String> licenses) {
        this.licenses = licenses;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
