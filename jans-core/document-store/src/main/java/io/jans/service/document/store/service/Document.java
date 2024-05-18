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
@ObjectClass(value = "jansDocument")
@JsonInclude(Include.NON_NULL)
public class Document extends Entry implements Serializable {

	private static final long serialVersionUID = -2812480357430436503L;

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
	private List<String> jansModuleProperty;

	@AttributeName
	private Integer jansLevel;

	@AttributeName
	private Integer jansRevision;
	
	@AttributeName
	private boolean jansEnabled;
	
	@AttributeName
	private String jansAlias;

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

	public String getDocument() {
		return document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public List<String> getJansModuleProperty() {
		return jansModuleProperty;
	}

	public void setJansModuleProperty(List<String> jansModuleProperty) {
		this.jansModuleProperty = jansModuleProperty;
	}

	public Integer getJansLevel() {
		return jansLevel;
	}

	public void setJansLevel(Integer jansLevel) {
		this.jansLevel = jansLevel;
	}

    public Integer getJansRevision() {
        return jansRevision;
    }

    public void setJansRevision(Integer jansRevision) {
        this.jansRevision = jansRevision;
    }

    public boolean isJansEnabled() {
        return jansEnabled;
    }

    public void setJansEnabled(boolean jansEnabled) {
        this.jansEnabled = jansEnabled;
    }

    public String getJansAlias() {
        return jansAlias;
    }

    public void setJansAlias(String jansAlias) {
        this.jansAlias = jansAlias;
    }

    @Override
    public String toString() {
        return "Document [inum=" + inum + ", displayName=" + displayName + ", description=" + description
                + ", document=" + document + ", creationDate=" + creationDate + ", jansModuleProperty="
                + jansModuleProperty + ", jansLevel=" + jansLevel + ", jansRevision=" + jansRevision + ", jansEnabled="
                + jansEnabled + ", jansAlias=" + jansAlias + "]";
    }
}
