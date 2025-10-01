/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.custom.script;

import jakarta.enterprise.inject.Vetoed;

import io.jans.orm.PersistenceEntryManager;
import io.jans.service.PythonService;
import org.slf4j.LoggerFactory;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 06/19/2020
 */
@Vetoed
public class StandaloneCustomScriptService extends AbstractCustomScriptService {

	private static final long serialVersionUID = -5283102477313448031L;
	
	private String scriptsBaseDn;
	
	public void configure(PersistenceEntryManager entryManager, String scriptsBaseDn) {
		this.scriptsBaseDn = scriptsBaseDn;
		this.log = LoggerFactory.getLogger(PythonService.class);
		this.persistenceEntryManager = entryManager;
	}

    public String baseDn() {
        return scriptsBaseDn;
    }

}
