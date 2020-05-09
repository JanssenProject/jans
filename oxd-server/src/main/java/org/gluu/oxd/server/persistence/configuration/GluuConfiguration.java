package org.gluu.oxd.server.persistence.configuration;

public class GluuConfiguration {

    private String configFileLocation;

    public String getConfigFileLocation() {
        return configFileLocation;
    }

    public void setConfigFileLocation(String configFileLocation) {
        this.configFileLocation = configFileLocation;
    }

    @Override
    public String toString() {
        return "GluuConfiguration{" +
                "configFileLocation='" + configFileLocation + '\'' +
                '}';
    }

}
