package io.jans.configapi.core.model;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

@DataEntry
@ObjectClass(value = "jansAppConf")
public class Conf<C> {

    @DN
    protected String dn;

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    protected C dynamicConf;

    @JsonObject
    @AttributeName(name = "jansConfStatic")
    protected StaticConfiguration staticConf;

    @AttributeName(name = "jansRevision")
    protected long revision;

    public Conf() {
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public C getDynamicConf() {
        System.out.println(" Conf<C>-getDynamicConf() - dynamicConf.getClass() =" +dynamicConf.getClass());
        return dynamicConf;
    }

    public void setDynamicConf(C dynamicConf) {
        System.out.println(" Conf<C>-setDynamicConf() - dynamicConf.getClass() =" +dynamicConf.getClass());
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