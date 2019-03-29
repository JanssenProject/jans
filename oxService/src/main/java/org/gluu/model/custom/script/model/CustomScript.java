/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.model.custom.script.model;

import java.util.Iterator;
import java.util.List;

import javax.persistence.Transient;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.gluu.model.ProgrammingLanguage;
import org.gluu.model.ScriptLocationType;
import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.SimpleExtendedCustomProperty;
import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.persist.model.base.BaseEntry;
import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapJsonObject;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.gluu.util.StringHelper;

/**
 * Custom script configuration
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@LdapEntry(sortBy = "level")
@LdapObjectClass(values = { "top", "oxCustomScript" })
public class CustomScript extends BaseEntry {

    public static final String LOCATION_TYPE_MODEL_PROPERTY = "location_type";
    public static final String LOCATION_PATH_MODEL_PROPERTY = "location_path";

    @LdapAttribute(ignoreDuringUpdate = true)
    private String inum;

    @LdapAttribute(name = "displayName")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-\\:\\/\\.]+$", message = "Name should contain only letters, digits and underscores")
    @Size(min = 2, max = 60, message = "Length of the Name should be between 1 and 30")
    private String name;

    @LdapAttribute(name = "description")
    private String description;

    @LdapAttribute(name = "oxScript")
    private String script;

    @LdapAttribute(name = "oxScriptType")
    private CustomScriptType scriptType;

    @LdapAttribute(name = "programmingLanguage")
    private ProgrammingLanguage programmingLanguage;

    @LdapJsonObject
    @LdapAttribute(name = "oxModuleProperty")
    private List<SimpleCustomProperty> moduleProperties;

    @LdapJsonObject
    @LdapAttribute(name = "oxConfigurationProperty")
    private List<SimpleExtendedCustomProperty> configurationProperties;

    @LdapAttribute(name = "oxLevel")
    private int level;

    @LdapAttribute(name = "oxRevision")
    private long revision;

    @LdapAttribute(name = "gluuStatus")
    private boolean enabled;

    @LdapJsonObject
    @LdapAttribute(name = "oxScriptError")
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getScript() {
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

        List<SimpleCustomProperty> moduleProperties = getModuleProperties();
        if (moduleProperties == null) {
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
        List<SimpleCustomProperty> moduleProperties = getModuleProperties();
        if (moduleProperties == null) {
            return;
        }

        for (Iterator<SimpleCustomProperty> it = moduleProperties.iterator(); it.hasNext();) {
            SimpleCustomProperty moduleProperty = (SimpleCustomProperty) it.next();
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

}
