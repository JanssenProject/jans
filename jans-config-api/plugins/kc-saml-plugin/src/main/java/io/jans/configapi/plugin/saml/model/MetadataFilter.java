/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.model;

import java.io.Serializable;
import java.util.List;

public class MetadataFilter implements Serializable {


	private String name;
	private String description;
	private List<String> extensionSchemas;
	private String extensionSchema;
	private boolean removeRolelessEntityDescriptors;
	private boolean removeEmptyEntitiesDescriptors;
	private String retainedRole;
	private List<String> retainedRoles;
	private int maxValidityInterval;
	private String id;
	private String certPath;
	private boolean requireSignedMetadata;
	private String filterCertFileName;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof MetadataFilter) && name != null && name.equals(((MetadataFilter) obj).getName());
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public void setExtensionSchemas(List<String> extensionSchemas) {
		this.extensionSchemas = extensionSchemas;
	}

	public List<String> getExtensionSchemas() {
		return this.extensionSchemas;
	}

	public void setExtensionSchema(String extensionSchema) {
		this.extensionSchema = extensionSchema;
	}

	public String getExtensionSchema() {
		return extensionSchema;
	}

	public boolean getRemoveRolelessEntityDescriptors() {
		return removeRolelessEntityDescriptors;
	}

	public void setRemoveRolelessEntityDescriptors(boolean removeRolelessEntityDescriptors) {
		this.removeRolelessEntityDescriptors = removeRolelessEntityDescriptors;
	}

	public boolean getRemoveEmptyEntitiesDescriptors() {
		return removeEmptyEntitiesDescriptors;
	}

	public void setRemoveEmptyEntitiesDescriptors(boolean removeEmptyEntitiesDescriptors) {
		this.removeEmptyEntitiesDescriptors = removeEmptyEntitiesDescriptors;

	}

	public String getRetainedRole() {
		return retainedRole;
	}

	public void setRetainedRole(String retainedRole) {
		this.retainedRole = retainedRole;
	}

	public List<String> getRetainedRoles() {
		return retainedRoles;
	}

	public void setRetainedRoles(List<String> retainedRoles) {
		this.retainedRoles = retainedRoles;

	}

	public void setMaxValidityInterval(int maxValidityInterval) {
		this.maxValidityInterval = maxValidityInterval;
	}

	public int getMaxValidityInterval() {
		return maxValidityInterval;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public void setCertPath(String certPath) {
		this.certPath = certPath;
	}

	public String getCertPath() {
		return this.certPath;
	}

	public void setRequireSignedMetadata(boolean requireSignedMetadata) {
		this.requireSignedMetadata = requireSignedMetadata;
	}

	public boolean getRequireSignedMetadata() {
		return this.requireSignedMetadata;
	}

	public void setFilterCertFileName(String filterCertFileName) {
		this.filterCertFileName = filterCertFileName;

	}

	public String getFilterCertFileName() {
		return this.filterCertFileName;
	}

}
