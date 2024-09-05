package io.jans.as.model.config.adminui;

public class UIConfiguration {
    private Integer sessionTimeoutInMins;

    public UIConfiguration() {
        //Do not remove
    }

    public Integer getSessionTimeoutInMins() {
        return sessionTimeoutInMins;
    }

    public void setSessionTimeoutInMins(Integer sessionTimeoutInMins) {
        this.sessionTimeoutInMins = sessionTimeoutInMins;
    }
}
