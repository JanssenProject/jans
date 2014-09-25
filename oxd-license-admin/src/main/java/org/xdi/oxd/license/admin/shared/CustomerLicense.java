package org.xdi.oxd.license.admin.shared;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 25/09/2014
 */

public class CustomerLicense implements Serializable {

    private LicenseType type;
    private int numberOfThreads;

    public CustomerLicense() {
    }

    public CustomerLicense(int numberOfThreads, LicenseType type) {
        this.numberOfThreads = numberOfThreads;
        this.type = type;
    }

    public LicenseType getType() {
        return type;
    }

    public void setType(LicenseType type) {
        this.type = type;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }
}
