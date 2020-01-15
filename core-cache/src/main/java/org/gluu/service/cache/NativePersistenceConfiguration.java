package org.gluu.service.cache;


import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NativePersistenceConfiguration implements Serializable {

    @XmlElement(name = "defaultPutExpiration")
    private int defaultPutExpiration = 60; // in seconds

    @XmlElement(name = "defaultCleanupBatchSize")
    private int defaultCleanupBatchSize = 1000; // 1000 objects per iteration

    @XmlElement(name = "deleteExpiredOnGetRequest")
    private boolean deleteExpiredOnGetRequest = false;

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

    public boolean isDeleteExpiredOnGetRequest() {
		return deleteExpiredOnGetRequest;
	}

	public void setDeleteExpiredOnGetRequest(boolean deleteExpiredOnGetRequest) {
		this.deleteExpiredOnGetRequest = deleteExpiredOnGetRequest;
	}

	public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    @Override
	public String toString() {
		return "NativePersistenceConfiguration [defaultPutExpiration=" + defaultPutExpiration + ", defaultCleanupBatchSize="
				+ defaultCleanupBatchSize + ", deleteExpiredOnGetRequest=" + deleteExpiredOnGetRequest + ", baseDn=" + baseDn + "]";
	}
}
