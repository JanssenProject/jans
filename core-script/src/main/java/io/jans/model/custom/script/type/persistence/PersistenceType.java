/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.model.custom.script.type.persistence;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.type.BaseExternalType;
import io.jans.persist.exception.extension.PersistenceExtension;

/**
 * Base interface for persistence script
 *
 * @author Yuriy Movchan Date: 06/04/2020
 */
public interface PersistenceType extends BaseExternalType, PersistenceExtension {

	void onAfterCreate(Object context, Map<String, SimpleCustomProperty> configurationAttributes);
	void onAfterDestroy(Object context, Map<String, SimpleCustomProperty> configurationAttributes);

}
