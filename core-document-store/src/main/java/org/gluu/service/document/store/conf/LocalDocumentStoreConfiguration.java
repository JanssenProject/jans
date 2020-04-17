package org.gluu.service.document.store.conf;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocalDocumentStoreConfiguration implements Serializable {

	private static final long serialVersionUID = -2403247753790576983L;

	@XmlElement(name = "baseLocation")
    private String baseLocation = "/";

    public String getBaseLocation() {
		return baseLocation;
	}

	public void setBaseLocation(String baseLocation) {
		this.baseLocation = baseLocation;
	}

	@Override
	public String toString() {
		return "LocalConfiguration [baseLocation=" + baseLocation + "]";
	}
}
