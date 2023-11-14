package io.jans.casa.core.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.InumEntry;
import io.jans.service.cache.CacheConfiguration;

//For unknown reason io.jans.as.persistence.model.configuration.GluuConfiguration is giving trouble
@DataEntry
@ObjectClass(value = "jansAppConf")
public class GluuConfiguration extends InumEntry {

    @AttributeName(name = "jansCacheConf")
    @JsonObject
    private CacheConfiguration cacheConfiguration;

    public CacheConfiguration getCacheConfiguration() {
        return cacheConfiguration;
    }

    public void setCacheConfiguration(CacheConfiguration cacheConfiguration) {
        this.cacheConfiguration = cacheConfiguration;
    }

}
