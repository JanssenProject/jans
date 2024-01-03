/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.custom.script;

import java.util.List;

import io.jans.model.custom.script.CustomScriptType;

/**
 * Provides list of supported additional scripts
 *
 * @author Yuriy Movchan Date: 12/22/2023
 */
public interface CustomScriptActivator {

	List<CustomScriptType> getActiveCustomScripts();

}
