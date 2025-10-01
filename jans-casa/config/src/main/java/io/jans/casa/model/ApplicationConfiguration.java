package io.jans.casa.model;

import io.jans.casa.conf.MainSettings;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

@DataEntry
@ObjectClass(value = "jansAppConf")
public class ApplicationConfiguration extends Entry {

    @JsonObject
    @AttributeName(name = "jansConfApp")
    private MainSettings settings;

    public MainSettings getSettings() {
        return settings;
    }

    public void setSettings(MainSettings settings) {
        this.settings = settings;
    }

}
