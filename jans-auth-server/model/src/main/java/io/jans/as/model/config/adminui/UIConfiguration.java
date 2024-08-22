package io.jans.as.model.config.adminui;

public class UIConfiguration {
    private Integer sessionTimeoutInMins;
    private String configApiHost;

    public UIConfiguration() {
        //Do not remove
    }

    public String getConfigApiHost() {
        return configApiHost;
    }

    public void setConfigApiHost(String configApiHost) {
        this.configApiHost = configApiHost;
    }

    public Integer getSessionTimeoutInMins() {
        return sessionTimeoutInMins;
    }

    public void setSessionTimeoutInMins(Integer sessionTimeoutInMins) {
        this.sessionTimeoutInMins = sessionTimeoutInMins;
    }
}
