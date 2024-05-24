/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.service.custom;

import java.util.Arrays;
import java.util.List;

import io.jans.model.custom.script.CustomScriptType;
import io.jans.service.custom.script.CustomScriptActivator;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Provides lock service plugin list of lock scripts
 *
 * @author Yuriy Movchan Date: 12/22/2023
 */
@ApplicationScoped
public class LockCustomScriptActivator implements CustomScriptActivator {

	@Override
	public List<CustomScriptType> getActiveCustomScripts() {
		return Arrays.asList(CustomScriptType.LOCK_EXTENSION);
	}
	
}
