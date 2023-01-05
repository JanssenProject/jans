package io.jans.ads.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentDetails {

    private String projectName;
    private List<String> folders;
    private Map<String, String> flowsError;
    private String error;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public List<String> getFolders() {
        return folders;
    }
    
    public void setFolders(List<String> folders) {
        this.folders = folders;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Map<String, String> getFlowsError() {
        return flowsError;
    }

    public void setFlowsError(Map<String, String> flowsError) {
        this.flowsError = flowsError;
    }

}