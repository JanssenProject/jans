/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.keycloak.link.model.config;

import io.jans.orm.annotation.*;
import jakarta.enterprise.inject.Vetoed;

/**
 * @author Yuriy Movchan
 * @version 0.1, 04/05/2023
 */
@Vetoed
@DataEntry
@ObjectClass(value = "jansAppConf")
public class Conf {
    @DN
    private String dn;

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    private AppConfiguration dynamic;

    @JsonObject
    @AttributeName(name = "jansConfStatic")
    private StaticConfiguration statics;

    @AttributeName(name = "jansRevision")
    private long revision;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public AppConfiguration getDynamic() {
        return dynamic;
    }

    public void setDynamic(AppConfiguration dynamic) {
        this.dynamic = dynamic;
    }

    public StaticConfiguration getStatics() {
        return statics;
    }

    public void setStatics(StaticConfiguration statics) {
        this.statics = statics;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Conf");
        sb.append("{dn='").append(dn).append('\'');
        sb.append(", dynamic='").append(dynamic).append('\'');
        sb.append(", static='").append(statics).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
