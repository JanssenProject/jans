package io.jans.service.document.store.model;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

/**
 * jansDocument
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

    @AttributeName(name = "displayName")
    private String fileName;

    @AttributeName(name = "jansFilePath")
    private String filePath;

    @AttributeName
    private String description;

    @AttributeName
    private String document;

    @AttributeName
    private Date creationDate;

    @AttributeName(name = "jansService")
    private String service;

    @AttributeName(name = "jansLevel")
    private Integer level;

    @AttributeName(name = "jansRevision")
    private Integer revision;

    @AttributeName(name = "jansEnabled")
    private boolean enabled;

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
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

    public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public Integer getRevision() {
		return revision;
	}

	public void setRevision(Integer revision) {
		this.revision = revision;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public String toString() {
		return "Document [inum=" + inum + ", fileName=" + fileName + ", filePath=" + filePath + ", description="
				+ description + ", creationDate=" + creationDate + ", service=" + service
				+ ", level=" + level + ", revision=" + revision + ", enabled=" + enabled + "]";
	}
}
