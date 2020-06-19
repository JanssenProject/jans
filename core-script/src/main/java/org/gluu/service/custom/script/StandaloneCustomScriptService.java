package org.gluu.service.custom.script;

import org.gluu.persist.PersistenceEntryManager;
import org.gluu.service.PythonService;
import org.slf4j.LoggerFactory;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 06/19/2020
 */
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
