package io.jans.casa.core.model;

import io.jans.model.SimpleCustomProperty;
import io.jans.orm.model.base.Entry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

import java.util.List;

import io.jans.casa.misc.Utils;

/**
 * A basic representation of a Gluu Server custom script. Use this class in conjunction with
 * {@link io.jans.casa.service.IPersistenceService} to read data, modify or delete custom scripts from the server.
 */
@DataEntry
@ObjectClass("jansCustomScr")
public class CustomScript extends Entry {

    @AttributeName
    private String displayName;

    @AttributeName(name = "jansEnabled")
    private Boolean enabled;

    @JsonObject
    @AttributeName(name = "jansConfProperty")
    private List<SimpleCustomProperty> configurationProperties;

    @AttributeName(name = "jansLevel")
    private Integer level;

    @JsonObject
    @AttributeName(name = "jansModuleProperty")
    private List<SimpleCustomProperty> moduleProperties;

    @AttributeName(name = "jansRevision")
    private Long revision;

    @AttributeName(name = "jansScr")
    private String script;

    public String getDisplayName() {
        return displayName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public List<SimpleCustomProperty> getConfigurationProperties() {
        return configurationProperties;
    }

    public Integer getLevel() {
        return level;
    }

    public List<SimpleCustomProperty> getModuleProperties() {
        return Utils.nonNullList(moduleProperties);
    }

    public Long getRevision() {
        return revision;
    }

    public String getScript() {
        return script;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setConfigurationProperties(List<SimpleCustomProperty> configurationProperties) {
        this.configurationProperties = configurationProperties;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public void setModuleProperties(List<SimpleCustomProperty> moduleProperties) {
        this.moduleProperties = moduleProperties;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }

    public void setScript(String script) {
        this.script = script;
    }

}
