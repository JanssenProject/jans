package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetDirMapping implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Schema(description = "Relative path to asset base directory.")
    private String directory;

    @Schema(description = "List of file extention that are stored in directory.")
    private List<String> type;

    @Schema(description = "Description of assets stored in directory.")
    private String description;

    @Schema(description = "List of supported service module where asset can be uploaded.")
    private List<String> jansServiceModule;

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public List<String> getType() {
        return type;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getJansServiceModule() {
        return jansServiceModule;
    }

    public void setJansServiceModule(List<String> jansServiceModule) {
        this.jansServiceModule = jansServiceModule;
    }

    @Override
    public String toString() {
        return "AssetDirMapping [directory=" + directory + ", type=" + type + ", description=" + description
                + ", jansServiceModule=" + jansServiceModule + "]";
    }

}
