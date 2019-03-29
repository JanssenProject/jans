/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

/**
 * oxIDPAuthConf
 * 
 * @author Reda Zerrad Date: 08.14.2012
 */

@XmlRootElement(name = "oxIDPAuthConf")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonPropertyOrder({ "type", "name", "level", "priority", "enabled", "version", "fields", "config" })
@XmlType(propOrder = { "type", "name", "level", "priority", "enabled", "version", "fields", "config" })
public class oxIDPAuthConf {
	private String type;
	private String name;

	private int level;
	private int priority;
	private boolean enabled;
	private List<CustomProperty> fields;
	private int version;

	private String config;

	public oxIDPAuthConf() {
		this.fields = new ArrayList<CustomProperty>();
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<CustomProperty> getFields() {
		return this.fields;
	}

	public void setFields(List<CustomProperty> fields) {
		this.fields = fields;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

}
