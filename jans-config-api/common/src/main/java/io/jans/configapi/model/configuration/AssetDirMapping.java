package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetDirMapping {

    @Schema(description = "Relative path to asset base directory.")
    private String directory;

    @Schema(description = "List of file extention that are stored in directory.")
    private List<String> type;

    @Schema(description = "Description of assets stored in directory.")
    private String description;

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

    @Override
    public String toString() {
        return "AssetDirMapping [directory=" + directory + ", type=" + type + ", description=" + description + "]";
    }

}
