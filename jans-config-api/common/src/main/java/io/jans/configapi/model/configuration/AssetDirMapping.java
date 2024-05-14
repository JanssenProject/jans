package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetDirMapping {

    /**
     * Relative path to asset base directory
     */
    private String directory;

    /**
     * List of file extention that are stored in directory
     */
    private List<String> type;

    /**
     * Description of assets stored in directory
     */
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
