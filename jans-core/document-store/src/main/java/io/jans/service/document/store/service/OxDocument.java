package io.jans.service.document.store.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

/**
 * oxDocument
 * 
 * @author Shekhar L. Date: 01.10.2022
 */


@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "oxDocument")
@JsonInclude(Include.NON_NULL)
public class OxDocument extends Entry implements Serializable {

	private static final long serialVersionUID = -2812480357430436503L;

	private transient boolean selected;

	@AttributeName(ignoreDuringUpdate = true)
	private String inum;


	@AttributeName
	private String displayName;

	@AttributeName
	private String description;

	@AttributeName
	private String document;
	
	@AttributeName
	private Date creationDate;
	
	@AttributeName
	private List<String> oxModuleProperty;

	@AttributeName
	private String oxLevel;

	@AttributeName
	private String oxRevision;
	
	@AttributeName
	private String oxEnabled;
	
	@AttributeName
	private String oxAlias;

	public String getInum() {
		return inum;
	}

	public void setInum(String inum) {
		this.inum = inum;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getDocument() {
		return document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	public List<String> getOxModuleProperty() {
		return oxModuleProperty;
	}

	public void setOxModuleProperty(List<String> oxModuleProperty) {
		this.oxModuleProperty = oxModuleProperty;
	}

	public String getOxLevel() {
		return oxLevel;
	}

	public void setOxLevel(String oxLevel) {
		this.oxLevel = oxLevel;
	}

	public String getOxRevision() {
		return oxRevision;
	}

	public void setOxRevision(String oxRevision) {
		this.oxRevision = oxRevision;
	}

	public String getOxEnabled() {
		return oxEnabled;
	}

	public void setOxEnabled(String oxEnabled) {
		this.oxEnabled = oxEnabled;
	}

	public String getOxAlias() {
		return oxAlias;
	}

	public void setOxAlias(String oxAlias) {
		this.oxAlias = oxAlias;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

}
