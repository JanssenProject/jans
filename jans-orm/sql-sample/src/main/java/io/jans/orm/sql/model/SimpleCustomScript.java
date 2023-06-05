/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.model;

import java.util.List;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Custom script configuration
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@DataEntry(sortBy = "level", sortByName = "jansLevel")
@ObjectClass("jansCustomScr")
public class SimpleCustomScript extends BaseEntry {

    public static final String LOCATION_TYPE_MODEL_PROPERTY = "location_type";
    public static final String LOCATION_PATH_MODEL_PROPERTY = "location_path";

    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "displayName")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-\\:\\/\\.]+$", message = "Name should contain only letters, digits and underscores")
    @Size(min = 2, max = 60, message = "Length of the Name should be between 1 and 30")
    private String name;
    
    @AttributeName(name = "jansAlias")
    private List<String> aliases;

    @AttributeName(name = "description")
    private String description;

    @AttributeName(name = "jansScr")
    private String script;

    @AttributeName(name = "jansLevel")
    private int level;

    @AttributeName(name = "jansRevision")
    private long revision;

    @AttributeName(name = "jansEnabled")
    private boolean enabled;

    @Transient
    private boolean modified;

    @Transient
    private boolean internal;

    public SimpleCustomScript() {
    }

    public SimpleCustomScript(String dn, String inum, String name) {
        super(dn);
        this.inum = inum;
        this.name = name;
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getAliases() {
		return aliases;
	}

	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}

	public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

}
