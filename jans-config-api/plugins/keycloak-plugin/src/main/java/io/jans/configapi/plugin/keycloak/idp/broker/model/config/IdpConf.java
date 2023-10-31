/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.model.config;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

@DataEntry
@ObjectClass(value = "jansAppConf")
public class IdpConf {
    @DN
    private String dn;

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    private IdpAppConfiguration dynamicConf;

    @JsonObject
    @AttributeName(name = "jansConfStatic")
    private StaticConfiguration staticsConf;

    @AttributeName(name = "jansRevision")
    private long revision;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public IdpAppConfiguration getDynamicConf() {
        return dynamicConf;
    }

    public void setDynamicConf(IdpAppConfiguration dynamicConf) {
        this.dynamicConf = dynamicConf;
    }

    public StaticConfiguration getStaticsConf() {
        return staticsConf;
    }

    public void setStaticsConf(StaticConfiguration staticsConf) {
        this.staticsConf = staticsConf;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    @Override
    public String toString() {
        return "IdpConf [dn=" + dn + ", dynamicConf=" + dynamicConf + ", staticsConf=" + staticsConf + ", revision="
                + revision + "]";
    }
}
