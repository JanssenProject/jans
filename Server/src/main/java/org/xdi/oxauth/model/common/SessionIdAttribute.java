package org.xdi.oxauth.model.common;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

/**
 * @author Yuriy Movchan
 * @version 0.1, 07/31/2014
 */

@JsonPropertyOrder({ "name", "value" })
public class SessionIdAttribute implements Serializable {
	
	private static final long serialVersionUID = -4302759878934177750L;

	private String name;
	private String value;
	
    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
