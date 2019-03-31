/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/11/2014
 */

public class LicenseStatusOpResponse implements IOpResponse {

    @JsonProperty(value = "valid")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "valid")
    private boolean valid = false;
    @JsonProperty(value = "thread_count")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "thread_count")
    private int threadCount;
    @JsonProperty(value = "name")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name")
    private String name;
    @JsonProperty(value = "features")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "features")
    private List<String> features;

    public LicenseStatusOpResponse() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFeatures() {
        return features;
    }

    public LicenseStatusOpResponse setFeatures(List<String> features) {
        this.features = features;
        return this;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public LicenseStatusOpResponse setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    public boolean isValid() {
        return valid;
    }

    public LicenseStatusOpResponse setValid(boolean valid) {
        this.valid = valid;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LicenseStatusOpResponse");
        sb.append("{features=").append(features);
        sb.append(", valid=").append(valid);
        sb.append(", threadCount=").append(threadCount);
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
