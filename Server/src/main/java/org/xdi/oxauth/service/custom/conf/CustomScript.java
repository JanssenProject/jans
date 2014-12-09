/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.custom.conf;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapJsonObject;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.ldap.model.BaseEntry;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.oxauth.model.uma.persistence.ProgrammingLanguage;

/**
 * Custom script configuration 
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxCustomScript"})
public class CustomScript extends BaseEntry {

	@LdapAttribute(ignoreDuringUpdate = true)
	private String inum;

    @NotNull(message = "Name should be not empty")
    @LdapAttribute(name = "name")
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
	private List<SimpleCustomProperty> configurationProperties;

    @LdapAttribute(name = "oxLevel")
    private int level;

    @LdapAttribute(name = "oxRevision")
    private int revision;

    @LdapAttribute(name = "gluuStatus")
    private boolean enabled;
	
	public CustomScript() {
		this.configurationProperties = new ArrayList<SimpleCustomProperty>();
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

	public List<SimpleCustomProperty> getConfigurationProperties() {
		return configurationProperties;
	}

	public void setConfigurationProperties(List<SimpleCustomProperty> properties) {
		this.configurationProperties = properties;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
