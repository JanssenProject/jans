package org.xdi.oxd.license.client.js;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.List;

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
    @JsonProperty(value = "license_name")
    private String licenseName;
    @JsonProperty(value = "license_features")
    private List<String> licenseFeatures;

    public LicenseMetadata() {
    }

    public LicenseMetadata(LicenseType licenseType, boolean multiServer, int threadsCount) {
        this.licenseType = licenseType;
        this.multiServer = multiServer;
        this.threadsCount = threadsCount;
    }

    public List<String> getLicenseFeatures() {
        return licenseFeatures;
    }

    public LicenseMetadata setLicenseFeatures(List<String> licenseFeatures) {
        this.licenseFeatures = licenseFeatures;
        return this;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public LicenseMetadata setLicenseName(String licenseName) {
        this.licenseName = licenseName;
        return this;
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
