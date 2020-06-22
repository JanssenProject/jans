package org.gluu.service.custom.script;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Vetoed;

import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.service.PythonService;
import org.slf4j.LoggerFactory;

/**
 * Standalone custom script manager
 *
 * @author Yuriy Movchan Date: 06/19/2020
 */
@Vetoed
public class StandaloneCustomScriptManager extends CustomScriptManager {

	private static final long serialVersionUID = -7212146007659551839L;
	
	private List<ExternalScriptService> externalScriptServices = new ArrayList<ExternalScriptService>();
	
	public StandaloneCustomScriptManager(PersistenceEntryManager entryManager, String scriptsBaseDn, String pythonModulesDir) {
		// Configure python service
		PythonService pythonService = new PythonService();
		pythonService.configure();
		pythonService.init();
		// Initialize python interpreter
		pythonService.initPythonInterpreter(pythonModulesDir);

		// Configure custom script service
		StandaloneCustomScriptService standaloneCustomScriptService = new StandaloneCustomScriptService();
		standaloneCustomScriptService.configure(entryManager, scriptsBaseDn);

		this.log = LoggerFactory.getLogger(StandaloneCustomScriptManager.class);
		this.supportedCustomScriptTypes = new ArrayList<CustomScriptType>();
		this.pythonService = pythonService;
		this.customScriptService = standaloneCustomScriptService;
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
	public void updateScriptServices() {
		for (ExternalScriptService externalScriptService : externalScriptServices) {
			externalScriptService.reload(null);
		}
	}

}
