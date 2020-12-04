/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.client;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.type.BaseExternalType;

import java.util.Map;

/**
 * Base interface for external custom client registration python script
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public interface ClientRegistrationType extends BaseExternalType {

    public boolean createClient(Object registerRequest, Object client, Map<String, SimpleCustomProperty> configurationAttributes);

    public boolean updateClient(Object registerRequest, Object client, Map<String, SimpleCustomProperty> configurationAttributes);

    String getSoftwareStatementHmacSecret(Object context);

    String getSoftwareStatementJwks(Object context);

}
