/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;

@ApiModel(value = "SCIM Configuration")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ScimConfiguration {

	@JsonProperty(value = "version")
	@ApiModelProperty(value = "The version of the SCIM core protocol to which this server conforms.", required = true)
	private String version;

	@JsonProperty(value = "authorization_supported")
	@ApiModelProperty(value = "The allowed authorization methods.", required = true)
	private List<String> authorizationSupported;

	@JsonProperty(value = "user_endpoint")
	@ApiModelProperty(value = "The endpoint URI at which it's possible Retrieve, Add, Delete, Modify Users.", required = true)
	private String userEndpoint;

	@JsonProperty(value = "group_endpoint")
	@ApiModelProperty(value = "The endpoint URI at which it's possible Retrieve, Add, Delete, Modify Groups.", required = true)
	private String groupEndpoint;

	@JsonProperty(value = "fido2_devices_endpoint")
	@ApiModelProperty(value = "The endpoint URI at which it's possible Retrieve, Delete, Modify FIDO 2 devices.", required = true)
	private String fido2DevicesEndpoint;

	@JsonProperty(value = "bulk_endpoint")
	@ApiModelProperty(value = "The endpoint URI at which it's possible Bulk updates to one or more resources.", required = true)
	private String bulkEndpoint;

	@JsonProperty(value = "service_provider_endpoint")
	@ApiModelProperty(value = "The endpoint URI at which it's possible to retrieve Service Provider's configuration.", required = true)
	private String serviceProviderEndpoint;

	@JsonProperty(value = "resource_types_endpoint")
	@ApiModelProperty(value = "The endpoint URI at which it's possible to retrieve supported Resource Types.", required = true)
	private String resourceTypesEndpoint;

	@JsonProperty(value = "schemas_endpoint")
	@ApiModelProperty(value = "The endpoint URI at which it's possible to retrieve information about resource schemas supported.", required = true)
	private String schemasEndpoint;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<String> getAuthorizationSupported() {
		return authorizationSupported;
	}

	public void setAuthorizationSupported(List<String> authorizationSupported) {
		this.authorizationSupported = authorizationSupported;
	}

	public String getUserEndpoint() {
		return userEndpoint;
	}

	public void setUserEndpoint(String userEndpoint) {
		this.userEndpoint = userEndpoint;
	}

	public String getGroupEndpoint() {
		return groupEndpoint;
	}

	public void setGroupEndpoint(String groupEndpoint) {
		this.groupEndpoint = groupEndpoint;
	}

	public String getFido2DevicesEndpoint() {
		return fido2DevicesEndpoint;
	}

	public void setFido2DevicesEndpoint(String fido2DevicesEndpoint) {
		this.fido2DevicesEndpoint = fido2DevicesEndpoint;
	}

	public String getBulkEndpoint() {
		return bulkEndpoint;
	}

	public void setBulkEndpoint(String bulkEndpoint) {
		this.bulkEndpoint = bulkEndpoint;
	}

	public String getServiceProviderEndpoint() {
		return serviceProviderEndpoint;
	}

	public void setServiceProviderEndpoint(String serviceProviderEndpoint) {
		this.serviceProviderEndpoint = serviceProviderEndpoint;
	}

	public String getResourceTypesEndpoint() {
		return resourceTypesEndpoint;
	}

	public void setResourceTypesEndpoint(String resourceTypesEndpoint) {
		this.resourceTypesEndpoint = resourceTypesEndpoint;
	}

	public String getSchemasEndpoint() {
		return schemasEndpoint;
	}

	public void setSchemasEndpoint(String schemasEndpoint) {
		this.schemasEndpoint = schemasEndpoint;
	}
	
}
