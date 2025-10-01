/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.persistence;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.type.BaseExternalType;
import io.jans.orm.extension.PersistenceExtension;

/**
 * Base interface for persistence script
 *
 * @author Yuriy Movchan Date: 06/04/2020
 */
public interface PersistenceType extends BaseExternalType, PersistenceExtension {

	void onAfterCreate(Object context, Map<String, SimpleCustomProperty> configurationAttributes);
	void onAfterDestroy(Object context, Map<String, SimpleCustomProperty> configurationAttributes);

}
