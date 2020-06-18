/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model.custom.script.type.client;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * Base interface for external custom client registration python script
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public interface ClientRegistrationType extends BaseExternalType {

    public boolean createClient(Object registerRequest, Object client, Map<String, SimpleCustomProperty> configurationAttributes);

    public boolean updateClient(Object registerRequest, Object client, Map<String, SimpleCustomProperty> configurationAttributes);

}
