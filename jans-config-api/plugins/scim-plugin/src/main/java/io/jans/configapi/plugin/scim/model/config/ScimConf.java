/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.scim.model.config;

import io.jans.configapi.core.model.Conf;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

@DataEntry
@ObjectClass(value = "jansAppConf")
public class ScimConf extends Conf {

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    protected ScimAppConfiguration dynamicConf;

    public ScimAppConfiguration getDynamicConf() {
        return dynamicConf;
    }

    public void setDynamicConf(ScimAppConfiguration dynamicConf) {
        this.dynamicConf = dynamicConf;
    }

    @Override
    public String toString() {
        return "ScimConf [dn=" + dn + ", dynamicConf=" + dynamicConf + ", staticConf=" + staticConf + ", revision="
                + revision + "]";
    }
}