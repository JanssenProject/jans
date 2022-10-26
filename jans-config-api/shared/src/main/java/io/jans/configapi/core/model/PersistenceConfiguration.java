package io.jans.configapi.core.model;

import java.io.Serializable;

public class PersistenceConfiguration implements Serializable {
    
    private static final long serialVersionUID = -1214215449005176251L;
    
    private String persistenceType;

    public String getPersistenceType() {
        return persistenceType;
    }

    public void setPersistenceType(String persistenceType) {
        this.persistenceType = persistenceType;
    }

    @Override
    public String toString() {
        return "PersistenceConfiguration [persistenceType=" + persistenceType + "]";
    }
}
