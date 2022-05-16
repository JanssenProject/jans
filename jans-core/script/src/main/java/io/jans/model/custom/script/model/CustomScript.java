/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.model;

import io.jans.model.ProgrammingLanguage;
import io.jans.model.ScriptLocationType;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.SimpleExtendedCustomProperty;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;
import io.jans.util.StringHelper;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Custom script configuration
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@DataEntry(sortBy = "level", sortByName = "jansLevel")
@ObjectClass("jansCustomScr")
public class CustomScript extends BaseEntry {

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

    @AttributeName(name = "jansScrTyp")
    private CustomScriptType scriptType;

    @AttributeName(name = "jansProgLng")
    private ProgrammingLanguage programmingLanguage;

    @JsonObject
    @AttributeName(name = "jansModuleProperty")
    private List<SimpleCustomProperty> moduleProperties =new ArrayList<>();

    @JsonObject
    @AttributeName(name = "jansConfProperty")
    private List<SimpleExtendedCustomProperty> configurationProperties;

    @AttributeName(name = "jansLevel")
    private int level;

    @AttributeName(name = "jansRevision")
    private long revision;

    @AttributeName(name = "jansEnabled")
    private boolean enabled;

    @JsonObject
    @AttributeName(name = "jansScrError")
    private ScriptError scriptError;

    @Transient
    private boolean modified;

    @Transient
    private boolean internal;

    public CustomScript() {
    }

    public CustomScript(String dn, String inum, String name) {
        super(dn);
        this.inum = inum;
        this.name = name;
    }

    public CustomScript(CustomScript customScript) {
        super(customScript.getDn());
        this.inum = customScript.inum;
        this.name = customScript.name;
        this.description = customScript.description;
        this.script = customScript.script;
        this.scriptType = customScript.scriptType;
        this.programmingLanguage = customScript.programmingLanguage;
        this.moduleProperties = customScript.moduleProperties;
        this.configurationProperties = customScript.configurationProperties;
        this.level = customScript.level;
        this.revision = customScript.revision;
        this.enabled = customScript.enabled;
        this.modified = customScript.modified;
        this.internal = customScript.internal;
        
        if (customScript.aliases != null) {
        	this.aliases = new ArrayList<>(customScript.aliases);
        }
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

    public String getScript() {
        if (script == null) {
            script = scriptType == CustomScriptType.PERSON_AUTHENTICATION ?
                    ScriptTemplate.AUTHEN.getValue() :
                    ScriptTemplate.NO_AUTHEN.getValue();
        }
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public CustomScriptType getScriptType() {
        return scriptType;
    }

    public void setScriptType(CustomScriptType scriptType) {
        this.scriptType = scriptType;
    }

    public ProgrammingLanguage getProgrammingLanguage() {
        return programmingLanguage;
    }

    public void setProgrammingLanguage(ProgrammingLanguage programmingLanguage) {
        this.programmingLanguage = programmingLanguage;
    }

    public List<SimpleCustomProperty> getModuleProperties() {
        return moduleProperties;
    }

    public void setModuleProperties(List<SimpleCustomProperty> moduleProperties) {
        this.moduleProperties = moduleProperties;
    }

    public List<SimpleExtendedCustomProperty> getConfigurationProperties() {
        return configurationProperties;
    }

    public void setConfigurationProperties(List<SimpleExtendedCustomProperty> properties) {
        this.configurationProperties = properties;
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

    public ScriptLocationType getLocationType() {
        SimpleCustomProperty moduleProperty = getModuleProperty(LOCATION_TYPE_MODEL_PROPERTY);
        if (moduleProperty == null) {
            return null;
        }

        return ScriptLocationType.getByValue(moduleProperty.getValue2());
    }

    public void setLocationType(ScriptLocationType locationType) {
        if (locationType != null) {
            setModuleProperty(LOCATION_TYPE_MODEL_PROPERTY, locationType.getValue());
        }
    }

    public String getLocationPath() {
        SimpleCustomProperty moduleProperty = getModuleProperty(LOCATION_PATH_MODEL_PROPERTY);
        if (moduleProperty == null) {
            return null;
        }

        return moduleProperty.getValue2();
    }

    public void setLocationPath(String locationPath) {
        setModuleProperty(LOCATION_PATH_MODEL_PROPERTY, locationPath);
    }

    protected SimpleCustomProperty getModuleProperty(final String modulePropertyName) {
        SimpleCustomProperty result = null;

        List<SimpleCustomProperty> modulePropertiesList = getModuleProperties();
        if (modulePropertiesList == null) {
            return result;
        }

        for (SimpleCustomProperty moduleProperty : getModuleProperties()) {
            if (StringHelper.equalsIgnoreCase(moduleProperty.getValue1(), modulePropertyName)) {
                result = moduleProperty;
                break;
            }
        }

        return result;
    }

    protected void setModuleProperty(String name, String value) {
        SimpleCustomProperty moduleProperty = getModuleProperty(name);

        if (moduleProperty == null) {
            addModuleProperty(name, value);
        } else {
            moduleProperty.setValue2(value);
        }
    }

    public void addModuleProperty(final String name, final String value) {
        SimpleCustomProperty usageTypeModuleProperties = new SimpleCustomProperty(name, value);
        getModuleProperties().add(usageTypeModuleProperties);
    }

    public void removeModuleProperty(final String modulePropertyName) {
        List<SimpleCustomProperty> modulePropertiesList = getModuleProperties();
        if (modulePropertiesList == null) {
            return;
        }

        for (Iterator<SimpleCustomProperty> it = modulePropertiesList.iterator(); it.hasNext();) {
            SimpleCustomProperty moduleProperty = it.next();
            if (StringHelper.equalsIgnoreCase(moduleProperty.getValue1(), modulePropertyName)) {
                it.remove();
                break;
            }
        }
    }

    public final ScriptError getScriptError() {
        return scriptError;
    }

    public final void setScriptError(ScriptError scriptError) {
        this.scriptError = scriptError;
    }

    
    @Override
    public String toString() {
        return "CustomScript [inum=" + inum
                + ", name=" + name 
                + ", description=" + description
                + ", programmingLanguage=" + programmingLanguage
                + ", scriptType=" + scriptType
                + ", level=" + level
                + ", revision=" + revision
                + ", enabled=" + enabled
                + "]";
    }
}
