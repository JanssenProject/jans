package org.gluu.model.casa;

import io.jans.persist.annotation.AttributeName;
import io.jans.persist.annotation.DataEntry;
import io.jans.persist.annotation.JsonObject;
import io.jans.persist.annotation.ObjectClass;
import io.jans.persist.model.base.Entry;

@DataEntry
@ObjectClass(value = "oxApplicationConfiguration")
public class ApplicationConfiguration extends Entry {

    @AttributeName(name = "oxConfApplication")
    private String settings;

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }

}
