package io.jans.configapi.model.configuration;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginConf implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Schema(description = "Name of the plugin.")
    private String name;
    
    @Schema(description = "Description of the plugin.")
    private String description;
    
    @Schema(description = "Plugin application class.")
    private String className;

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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
        return "PluginConf [name=" + name + ", description=" + description + ", className=" + className + "]";
    }

}