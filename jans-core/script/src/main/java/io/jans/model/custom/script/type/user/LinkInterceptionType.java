/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.user;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.bind.BindCredentials;
import io.jans.model.custom.script.type.BaseExternalType;

/**
 * Base interface for external Link Interception python script
 *
 * @author Yuriy Movchan Date: 12/30/2012
 */
public interface LinkInterceptionType extends BaseExternalType {

    public BindCredentials getBindCredentials(String configId, Map<String, SimpleCustomProperty> configurationAttributes);

    public boolean isStartProcess(Map<String, SimpleCustomProperty> configurationAttributes);

    public boolean updateUser(Object person, Map<String, SimpleCustomProperty> configurationAttributes);

}
