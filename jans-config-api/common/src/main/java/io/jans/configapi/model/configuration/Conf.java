package io.jans.configapi.model.configuration;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

@DataEntry
@ObjectClass(value = "jansAppConf")
public class Conf {

    @DN
    private String dn;

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    private ApiAppConfiguration dynamicConf;

    @JsonObject
    @AttributeName(name = "jansConfStatic")
    private StaticConfiguration staticConf;

    @AttributeName(name = "jansRevision")
    private long revision;

    public Conf() {
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

    public ApiAppConfiguration getDynamicConf() {
        return dynamicConf;
    }

    public void setDynamicConf(ApiAppConfiguration dynamicConf) {
        this.dynamicConf = dynamicConf;
    }

    public StaticConfiguration getStaticConf() {
        return staticConf;
    }

    public void setStaticConf(StaticConfiguration staticConf) {
        this.staticConf = staticConf;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    @Override
    public String toString() {
        return "Conf [dn=" + dn + ", dynamicConf=" + dynamicConf + ", staticConf=" + staticConf + ", revision="
                + revision + "]";
    }
}