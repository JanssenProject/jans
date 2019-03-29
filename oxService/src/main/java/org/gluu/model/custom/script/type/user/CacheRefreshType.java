/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.model.custom.script.type.user;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.model.bind.BindCredentials;
import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * Base interface for external cache refresh python script
 *
 * @author Yuriy Movchan Date: 12/30/2012
 */
public interface CacheRefreshType extends BaseExternalType {

    public BindCredentials getBindCredentials(String configId, Map<String, SimpleCustomProperty> configurationAttributes);

    public boolean isStartProcess(Map<String, SimpleCustomProperty> configurationAttributes);

    public boolean updateUser(Object person, Map<String, SimpleCustomProperty> configurationAttributes);

}
