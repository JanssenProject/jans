/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.custom.interfaces;

import java.util.Map;

import org.xdi.model.SimpleCustomProperty;

/**
 * Base interface for external python script
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public interface BaseExternalType {

	public boolean init(Map<String, SimpleCustomProperty> configurationAttributes);

	public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes);

	public int getApiVersion();

}
