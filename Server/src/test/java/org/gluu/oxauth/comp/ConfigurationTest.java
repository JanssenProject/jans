/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.comp;

import java.io.File;
import java.io.FileInputStream;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.gluu.oxauth.ConfigurableTest;
import org.gluu.oxauth.model.config.Conf;
import org.gluu.oxauth.model.config.ConfigurationFactory;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.config.WebKeysConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorMessages;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.persist.PersistenceEntryManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/01/2013
 */

public class ConfigurationTest extends ConfigurableTest {

	@Inject
	private ConfigurationFactory configurationFactory;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	/*
	 * Configuration must be present, otherwise server will not start
	 * normally... There is fallback configuration from file but server will not
	 * work as expected in cluster.`
	 */
	@Test
	public void configurationPresence() {
		Assert.assertTrue((configurationFactory != null) && (configurationFactory.getPersistenceConfiguration().getConfiguration() != null)
				&& (configurationFactory.getAppConfiguration() != null)
				&& (configurationFactory.getErrorResponseFactory() != null)
				&& (configurationFactory.getStaticConfiguration() != null)
				&& (configurationFactory.getWebKeysConfiguration() != null));
	}

	/*
	 * Useful test method to get create newest test configuration. It shouldn't
	 * be used directly for testing.
	 */
	// @Test
	public void createLatestTestConfInLdapFromFiles() throws Exception {
		final String prefix = "U:\\own\\project\\oxAuth\\Server\\src\\test\\resources\\conf";

		final String errorsFile = prefix + "\\oxauth-errors.json";
		final String staticFile = prefix + "\\oxauth-static-conf.json";
		final String webKeysFile = prefix + "\\oxauth-web-keys.json";
		final String configFile = prefix + "\\oxauth-config.xml";

		final String errorsJson = IOUtils.toString(new FileInputStream(errorsFile));
		final String staticConfJson = IOUtils.toString(new FileInputStream(staticFile));
		final String webKeysJson = IOUtils.toString(new FileInputStream(webKeysFile));
		
		final StaticConfiguration staticConf = ServerUtil.createJsonMapper().readValue(staticConfJson, StaticConfiguration.class);
		final ErrorMessages errorConf = ServerUtil.createJsonMapper().readValue(errorsJson, ErrorMessages.class);
		final WebKeysConfiguration webKeys = ServerUtil.createJsonMapper().readValue(webKeysJson, WebKeysConfiguration.class);

		final AppConfiguration configJson = loadConfFromFile(configFile);

		final Conf c = new Conf();
		c.setDn("ou=testconfiguration,o=gluu");
		c.setDynamic(configJson);
		c.setErrors(errorConf);
		c.setStatics(staticConf);
		c.setWebKeys(webKeys);
		ldapEntryManager.persist(c);
	}

	private static AppConfiguration loadConfFromFile(String p_filePath) throws JAXBException {
		final JAXBContext jc = JAXBContext.newInstance(AppConfiguration.class);
		final Unmarshaller u = jc.createUnmarshaller();
		return (AppConfiguration) u.unmarshal(new File(p_filePath));
	}
}
