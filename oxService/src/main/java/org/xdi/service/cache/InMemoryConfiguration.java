package org.xdi.service.cache;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

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
        return "InMemoryConfiguration{" +
                "defaultPutExpiration=" + defaultPutExpiration +
                '}';
    }
}
