package io.jans.ca.server.configuration.model;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.ca.server.configuration.ApiAppConfiguration;
import io.jans.orm.annotation.*;

@DataEntry
@ObjectClass("jansAppConf")
public class ApiConf {

    @DN
    protected String dn;

    @JsonObject
    @AttributeName(name = "jansConfStatic")
    protected StaticConfiguration staticConf;

    @AttributeName(name = "jansRevision")
    protected long revision;

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    private ApiAppConfiguration dynamicConf;


    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
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

    public ApiAppConfiguration getDynamicConf() {
        return dynamicConf;
    }

    public void setDynamicConf(ApiAppConfiguration dynamicConf) {
        this.dynamicConf = dynamicConf;
    }

    @Override
    public String toString() {
        return "ApiConf{" +
                "dn='" + dn + '\'' +
                ", staticConf=" + staticConf +
                ", revision=" + revision +
                ", dynamicConf=" + dynamicConf +
                '}';
    }
}
