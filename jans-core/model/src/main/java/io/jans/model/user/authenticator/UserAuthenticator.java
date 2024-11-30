/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.user.authenticator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
* Person authenticator descriptor
*
* @author Yuriy Movchan Date: 03/28/2024
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserAuthenticator implements Serializable {

	private static final long serialVersionUID = -9173244116167488365L;

	@JsonProperty(value = "id")
	private String id;

	@JsonProperty(value = "type")
	private String type;
	
	@JsonProperty(value = "custom")
	private Map<String, Object> custom;

	public UserAuthenticator() {
	}

	public UserAuthenticator(String id, String type) {
		this.id = id;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, Object> getCustom() {
		return custom;
	}

	public void setCustom(Map<String, Object> custom) {
		this.custom = custom;
	}

	public boolean hasCustom() {
		return (this.custom == null) || (this.custom.size() == 0);
	}

	public void addCustom(String name, Object value) {
		if (this.custom == null) {
			this.custom = new HashMap<>();
		}
		
		this.custom.put(name, value);
	}

	public boolean hasCustom(String name) {
		if (this.custom == null) {
			return false;
		}
		
		return this.custom.containsKey(name);
	}

	public Object getCustom(String name) {
		if (this.custom == null) {
			return null;
		}
		
		return this.custom.get(name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(custom, id, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserAuthenticator other = (UserAuthenticator) obj;
		return Objects.equals(custom, other.custom) && Objects.equals(id, other.id) && Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return "UserAuthenticator [id=" + id + ", type=" + type + ", custom=" + custom + "]";
	}

}

