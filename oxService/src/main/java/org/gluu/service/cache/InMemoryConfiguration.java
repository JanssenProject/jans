package org.gluu.service.cache;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;


/**
 * @author yuriyz on 02/21/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InMemoryConfiguration implements Serializable {

    private static final long serialVersionUID = 7544731515017051209L;

    @XmlElement(name = "defaultPutExpiration")
    private int defaultPutExpiration = 60; // in seconds

    public int getDefaultPutExpiration() {
        return defaultPutExpiration;
    }

    public void setDefaultPutExpiration(int defaultPutExpiration) {
        this.defaultPutExpiration = defaultPutExpiration;
    }

    @Override
    public String toString() {
        return "InMemoryConfiguration{" + "defaultPutExpiration=" + defaultPutExpiration + '}';
    }
}
