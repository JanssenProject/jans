package org.xdi.config.oxtrust;

import java.util.List;

/**
 * oxTrust configuration
 * 
 * @author shekhar laad
 * @date 12/10/2015
 */

public class ImportPersonConfig {
	
	private List <ImportPerson> mappings;

	public List<ImportPerson> getMappings() {
		return mappings;
	}

	public void setMappings(List<ImportPerson> mappings) {
		this.mappings = mappings;
	}
}
