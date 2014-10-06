package org.xdi.oxd.license.client.js;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/10/2014
 */

public class LicenseMetadata implements Serializable {

    @JsonProperty(value = "thread_count")
    private int threadsCount;
    @JsonProperty(value = "multi_server")
    private boolean multiServer;
    @JsonProperty(value = "license_type")
    private LicenseType licenseType;

    public LicenseMetadata() {
    }

    public LicenseMetadata(LicenseType licenseType, boolean multiServer, int threadsCount) {
        this.licenseType = licenseType;
        this.multiServer = multiServer;
        this.threadsCount = threadsCount;
    }

    public LicenseType getLicenseType() {
        return licenseType;
    }

    public LicenseMetadata setLicenseType(LicenseType licenseType) {
        this.licenseType = licenseType;
        return this;
    }

    public boolean isMultiServer() {
        return multiServer;
    }

    public LicenseMetadata setMultiServer(boolean multiServer) {
        this.multiServer = multiServer;
        return this;
    }

    public int getThreadsCount() {
        return threadsCount;
    }

    public LicenseMetadata setThreadsCount(int threadsCount) {
        this.threadsCount = threadsCount;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LicenseMetadata");
        sb.append("{licenseType=").append(licenseType);
        sb.append(", threadsCount=").append(threadsCount);
        sb.append(", multiServer=").append(multiServer);
        sb.append('}');
        return sb.toString();
    }
}
