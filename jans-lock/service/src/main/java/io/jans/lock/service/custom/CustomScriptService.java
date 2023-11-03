/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.service.custom;

import io.jans.service.custom.script.AbstractCustomScriptService;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
public abstract class CustomScriptService extends AbstractCustomScriptService {

	private static final long serialVersionUID = -5283102477313448031L;

	public abstract String baseDn();

}
