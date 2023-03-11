package io.jans.ads.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectMetadata {

    private String projectName;
    private String author;
    private String type;
    private String description;

    @JsonProperty("configs")
    private Map<String, Object> configHints;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getConfigHints() {
        return configHints;
    }

    public void setConfigHints(Map<String, Object> configHints) {
        this.configHints = configHints;
    }

}
