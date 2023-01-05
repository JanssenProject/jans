package io.jans.configapi.model.configuration;

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
    private ApiAppConfiguration dynamicConf;

    public ApiAppConfiguration getDynamicConf() {
        return dynamicConf;
    }

    public void setDynamicConf(ApiAppConfiguration dynamicConf) {
        this.dynamicConf = dynamicConf;
    }

    @Override
    public String toString() {
        return "ApiConf [dn=" + dn + ", dynamicConf=" + dynamicConf + ", staticConf=" + staticConf + ", revision="
                + revision + "]";
    }
}