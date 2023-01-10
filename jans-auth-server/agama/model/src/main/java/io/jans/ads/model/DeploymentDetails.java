package io.jans.ads.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentDetails {

    private List<String> folders;
    private Map<String, String> flowsError;
    private String error;
    private ProjectMetadata metadata = new ProjectMetadata();
    
    public ProjectMetadata getProjectMetadata() {
        return metadata;
    }

    public void setProjectMetadata(ProjectMetadata metadata) {
        this.metadata = metadata;
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