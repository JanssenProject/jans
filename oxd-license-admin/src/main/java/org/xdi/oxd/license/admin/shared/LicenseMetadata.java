package org.xdi.oxd.license.admin.shared;

import org.xdi.oxd.license.client.js.LicenseType;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 25/09/2014
 */

public class LicenseMetadata implements Serializable {

    private LicenseType type;
    private int numberOfThreads;
    private int numberOfServers;

    public LicenseMetadata() {
    }

    public LicenseMetadata(int numberOfThreads, LicenseType type) {
        this.numberOfThreads = numberOfThreads;
        this.type = type;
    }

    public LicenseType getType() {
        return type;
    }

    public LicenseMetadata setType(LicenseType type) {
        this.type = type;
        return this;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public LicenseMetadata setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        return this;
    }

    public int getNumberOfServers() {
        return numberOfServers;
    }

    public LicenseMetadata setNumberOfServers(int numberOfServers) {
        this.numberOfServers = numberOfServers;
        return this;
    }
}
