package org.gluu.oxd.server.persistence.configuration;

public class GluuConfiguration {

    private String location;

    public String getLocation()
    {
        return this.location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    @Override
    public String toString() {
        return "GluuConfiguration{" +
                "location='" + location + '\'' +
                '}';
    }
}
