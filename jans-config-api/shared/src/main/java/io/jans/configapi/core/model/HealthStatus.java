package io.jans.configapi.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthStatus implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private String status;
    private List<Status> checks;
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public List<Status> getChecks() {
        return checks;
    }
    public void setChecks(List<Status> checks) {
        this.checks = checks;
    }
    @Override
    public String toString() {
        return "HealthStatus [status=" + status + ", checks=" + checks + "]";
    }
}
