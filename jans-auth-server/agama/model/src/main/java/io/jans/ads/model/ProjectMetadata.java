package io.jans.ads.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectMetadata {

    private String projectName;
    private String author;
    private String type;
    private String description;
    private String version;

    @JsonProperty("configs")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Map<String, Map<String, Object>> configHints;

    @JsonProperty("noDirectLaunch")
    private List<String> noDirectLaunchFlows;

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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, Map<String, Object>> getConfigHints() {
        return configHints;
    }

    public void setConfigHints(Map<String, Map<String, Object>> configHints) {
        this.configHints = configHints;
    }

    public List<String> getNoDirectLaunchFlows() {
        return noDirectLaunchFlows;
    }

    public void setNoDirectLaunchFlows(List<String> noDirectLaunchFlows) {
        this.noDirectLaunchFlows = noDirectLaunchFlows;
    }

}
