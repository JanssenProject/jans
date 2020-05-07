package org.gluu.oxd.server.persistence.configuration;

import org.gluu.conf.model.AppConfigurationEntry;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.JsonObject;

public class OxdAppConfigurationEntry extends AppConfigurationEntry {

    private static final long serialVersionUID = -7301311833970330177L;

    @JsonObject
    @AttributeName(name="oxConfApplication")
    private OxdAppConfiguration application;

    public OxdAppConfiguration getApplication()
    {
        return this.application;
    }

    public void setApplication(OxdAppConfiguration application)
    {
        this.application = application;
    }

    @Override
    public String toString() {
        return "OxdAppConfigurationEntry{" +
                "application=" + application +
                '}';
    }
}
