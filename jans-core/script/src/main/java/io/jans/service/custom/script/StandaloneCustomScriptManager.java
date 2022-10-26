/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.custom.script;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.Vetoed;

import io.jans.orm.PersistenceEntryManager;
import io.jans.service.PythonService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

/**
 * Standalone custom script manager
 *
 * @author Yuriy Movchan Date: 06/19/2020
 */
@Vetoed
public class StandaloneCustomScriptManager extends CustomScriptManager {

	private static final long serialVersionUID = -7212146007659551839L;
	
	private final List<ExternalScriptService> externalScriptServices = new ArrayList<>();

    public StandaloneCustomScriptManager(PersistenceEntryManager entryManager, String scriptsBaseDn) {
        this(entryManager, scriptsBaseDn, null);
    }

    public StandaloneCustomScriptManager(PersistenceEntryManager entryManager, String scriptsBaseDn, String pythonModulesDir) {
		// Configure custom script service
		StandaloneCustomScriptService standaloneCustomScriptService = new StandaloneCustomScriptService();
		standaloneCustomScriptService.configure(entryManager, scriptsBaseDn);

		ExternalTypeCreator externalTypeCreator = new ExternalTypeCreator();
		if (StringUtils.isNotBlank(pythonModulesDir)) {
            externalTypeCreator.pythonService = createPythonService(pythonModulesDir);
        }
		externalTypeCreator.customScriptService = standaloneCustomScriptService;

		this.log = LoggerFactory.getLogger(StandaloneCustomScriptManager.class);
		this.supportedCustomScriptTypes = new ArrayList<>();
		this.externalTypeCreator = externalTypeCreator;
		this.customScriptService = standaloneCustomScriptService;
	}

	private static PythonService createPythonService(String pythonModulesDir) {
        // Configure python service
        PythonService pythonService = new PythonService();
        pythonService.configure();
        pythonService.init();
        // Initialize python interpreter
        pythonService.initPythonInterpreter(pythonModulesDir);
        return pythonService;
    }

	public void init() {
		configure();
		reloadTimerEvent(null);
	}

	public void destory() {
		super.destroy(null);
	}

	public void reload() {
		reloadTimerEvent(null);
	}
	
	public void registerExternalScriptService(ExternalScriptService externalScriptService) {
		externalScriptService.configure(this);
		externalScriptServices.add(externalScriptService);
		supportedCustomScriptTypes.add(externalScriptService.getCustomScriptType());
	}

	@Override
	public void updateScriptServices(boolean syncUpdate) {
		for (ExternalScriptService externalScriptService : externalScriptServices) {
			externalScriptService.reload(null);
		}
	}

}
