package org.xdi.service.cache;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NativePersistenceConfiguration implements Serializable {

    @XmlElement(name = "defaultPutExpiration")
    private int defaultPutExpiration = 60; // in seconds

    @XmlElement(name = "defaultCleanupBatchSize")
    private int defaultCleanupBatchSize = 25; // 25 objects per iteration

    @JsonIgnore
    private String baseDn;

    public int getDefaultPutExpiration() {
        return defaultPutExpiration;
    }

    public void setDefaultPutExpiration(int defaultPutExpiration) {
        this.defaultPutExpiration = defaultPutExpiration;
    }

    public int getDefaultCleanupBatchSize() {
        return defaultCleanupBatchSize;
    }

    public void setDefaultCleanupBatchSize(int defaultCleanupBatchSize) {
        this.defaultCleanupBatchSize = defaultCleanupBatchSize;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NativePersistenceConfiguration [defaultPutExpiration=").append(defaultPutExpiration).append(", defaultCleanupBatchSize=")
                .append(defaultCleanupBatchSize).append(", baseDn=").append(baseDn).append("]");
        return builder.toString();
    }
}
