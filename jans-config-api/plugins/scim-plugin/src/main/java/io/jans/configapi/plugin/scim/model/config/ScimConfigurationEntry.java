/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.scim.model.config;

import io.jans.conf.model.AppConfigurationEntry;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

@DataEntry
@ObjectClass(value = "jansAppConf")
public class ScimConfigurationEntry extends AppConfigurationEntry{

    private static final long serialVersionUID = 6101723502950147256L;
    
    @JsonObject
    @AttributeName(name = "jansConfStatic")
    private StaticConfiguration staticConf;

}
