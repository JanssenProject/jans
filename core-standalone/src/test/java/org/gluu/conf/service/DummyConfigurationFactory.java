package org.gluu.conf.service;

import org.gluu.conf.model.AppConfiguration;
import org.gluu.conf.model.AppConfigurationEntry;

/**
 * @author Yuriy Movchan
 * @version 0.1, 01/02/2020
 */
public class DummyConfigurationFactory extends ConfigurationFactory<AppConfiguration, AppConfigurationEntry> {

	@Override
	protected String getDefaultConfigurationFileName() {
		return "gluu-dummy.properties";
	}

	@Override
	protected Class<AppConfigurationEntry> getAppConfigurationType() {
		return AppConfigurationEntry.class;
	}

	@Override
	protected String getApplicationConfigurationPropertyName() {
		return "oxdummyConfigurationEntryDN";
	}

}
