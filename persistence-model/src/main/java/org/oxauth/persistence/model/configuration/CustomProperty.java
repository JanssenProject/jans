/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.oxauth.persistence.model.configuration;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Custom Attributes
 * 
 * @author Reda Zerrad Date: 07.26.2012
 * @author Yuriy Movchan Date: 08.27.2012
 */
@JsonPropertyOrder({ "name", "values" })
@XmlType(propOrder = { "name", "values" })
public class CustomProperty {

	private String name;
	private List<String> values;

	public CustomProperty() {
		name = "";
		values = new ArrayList<String>();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElementWrapper(name = "values")
	@XmlElement(name = "value")
	public List<String> getValues() {
		return this.values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

}
