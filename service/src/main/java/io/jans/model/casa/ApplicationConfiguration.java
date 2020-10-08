package io.jans.model.casa;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

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
