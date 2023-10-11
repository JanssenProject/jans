/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.link.service.custom;

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
