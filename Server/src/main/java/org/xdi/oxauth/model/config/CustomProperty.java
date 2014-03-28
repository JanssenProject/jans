package org.xdi.oxauth.model.config;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

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
