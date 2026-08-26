/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.shibboleth.plugin.config.model;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

@DataEntry
@ObjectClass(value="jansAppConf")
public class ShibbolethPluginAppConfiguration {
    
    @DN
    private String dn;

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    private ShibbolethPluginConfiguration dynamicConf;

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

    public ShibbolethPluginConfiguration getDynamicConf() {

        return dynamicConf;
    }

    public void setDynamicConf(ShibbolethPluginConfiguration dynamicConf) {

        this.dynamicConf = dynamicConf;
    }

    public StaticConfiguration getStaticsConf() {

        return this.staticsConf;
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

        return "ShibbolethPluginAppConfiguration[ " +
            "\ndn=" + dn +
            "\ndynamicConf=" + dynamicConf + 
            "\nStaticsConf=" + staticsConf + 
            "\nRevision=" + revision +  
        "]";
    }
}
