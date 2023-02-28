package io.jans.cacherefresh.model;

import io.jans.configapi.core.model.Conf;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

@DataEntry
@ObjectClass(value = "jansAppConf")
public class ApiConf extends Conf {

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    private CacheRefreshConfiguration dynamicConf;

    public CacheRefreshConfiguration getDynamicConf() {
        return dynamicConf;
    }

    public void setDynamicConf(CacheRefreshConfiguration dynamicConf) {
        this.dynamicConf = dynamicConf;
    }

    @Override
    public String toString() {
        return "ApiConf [dn=" + dn + ", dynamicConf=" + dynamicConf + ", revision="
                + revision + "]";
    }
}