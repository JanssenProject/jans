package org.gluu.model.casa;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.JsonObject;
import org.gluu.persist.annotation.ObjectClass;
import org.gluu.persist.model.base.Entry;

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
