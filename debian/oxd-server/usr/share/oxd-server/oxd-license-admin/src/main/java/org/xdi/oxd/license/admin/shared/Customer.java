package org.xdi.oxd.license.admin.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class Customer implements Serializable {

    private String dn;
    private String id;
    private String name;
    private String licenseCryptDn;
    private List<CustomerLicenseId> licenseIds = new ArrayList<CustomerLicenseId>();

    public List<CustomerLicenseId> getLicenseIds() {
        return licenseIds;
    }

    public void setLicenseIds(List<CustomerLicenseId> licenseIds) {
        this.licenseIds = licenseIds;
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

    public String getName() {
        return name;
    }

    public Customer setName(String name) {
        this.name = name;
        return this;
    }

    public String getLicenseCryptDn() {
        return licenseCryptDn;
    }

    public Customer setLicenseCryptDn(String licenseCryptDn) {
        this.licenseCryptDn = licenseCryptDn;
        return this;
    }
}
