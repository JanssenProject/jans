package org.xdi.oxauth.service.custom.conf;

import java.util.ArrayList;
import java.util.List;

import org.xdi.model.SimpleCustomProperty;

/**
 * Custom script configuration 
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public class BaseScriptConfiguration {

	private CustomScriptType customScriptType;

	private String name;
	private int version;
	private boolean enabled;

	private List<SimpleCustomProperty> properties;

	private String script;
	
	public BaseScriptConfiguration() {
		this.properties = new ArrayList<SimpleCustomProperty>();
	}

	public CustomScriptType getCustomScriptType() {
		return customScriptType;
	}

	public void setCustomScriptType(CustomScriptType customScriptType) {
		this.customScriptType = customScriptType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<SimpleCustomProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<SimpleCustomProperty> properties) {
		this.properties = properties;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

}
