package org.xdi.config.oxtrust;

import java.io.Serializable;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * oxTrust configuration
 * 
 * @author shekhar laad
 * @date 12/10/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportPersonConfig implements Serializable {
	
	private static final long serialVersionUID = 2686538577505167695L;

	private List <ImportPerson> mappings;

	public List<ImportPerson> getMappings() {
		return mappings;
	}

	public void setMappings(List<ImportPerson> mappings) {
		this.mappings = mappings;
	}
}
