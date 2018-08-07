package org.xdi.service.cache;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NativePersistenceConfiguration implements Serializable {

    @XmlElement(name = "defaultPutExpiration")
    private int defaultPutExpiration = 60; // in seconds

    @JsonIgnore
    private String baseDn;

    public int getDefaultPutExpiration() {
        return defaultPutExpiration;
    }

    public void setDefaultPutExpiration(int defaultPutExpiration) {
        this.defaultPutExpiration = defaultPutExpiration;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    @Override
    public String toString() {
        return "NativePersistenceConfiguration{" +
                "defaultPutExpiration=" + defaultPutExpiration +
                ", baseDn=" + baseDn +
                '}';
    }
}
