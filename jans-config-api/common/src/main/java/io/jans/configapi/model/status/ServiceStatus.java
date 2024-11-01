package io.jans.configapi.model.status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceStatus {

    private Map<String, String> serviceStatus;

    public Map<String, String> getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(Map<String, String> serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    @Override
    public String toString() {
        return "ServiceStatus [serviceStatus=" + serviceStatus + "]";
    }

}
