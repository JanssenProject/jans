/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import io.jans.as.model.config.Conf;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorMessages;
import io.jans.as.server.BaseComponentTest;
import io.jans.as.server.util.ServerUtil;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/01/2013
 */

public class ConfigurationTest extends BaseComponentTest {

    /*
     * Configuration must be present, otherwise server will not start
     * normally... There is fallback configuration from file but server will not
     * work as expected in cluster.`
     */
    @Test
    public void configurationPresence() {
        Assert.assertTrue((getConfigurationFactory() != null) && (getConfigurationFactory().getBaseConfiguration() != null)
                && (getConfigurationFactory().getPersistenceConfiguration().getConfiguration() != null)
                && (getConfigurationFactory().getAppConfiguration() != null)
                && (getConfigurationFactory().getErrorResponseFactory() != null)
                && (getConfigurationFactory().getStaticConfiguration() != null)
                && (getConfigurationFactory().getWebKeysConfiguration() != null));
    }

    /*
     * Useful test method to get create newest test configuration. It shouldn't
     * be used directly for testing.
     */
    // @Test
    public void createLatestTestConfInLdapFromFiles() throws Exception {
        final String prefix = "U:\\own\\project\\jans-auth\\server\\src\\test\\resources\\conf";

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
        c.setDn("ou=testconfiguration,o=jans");
        c.setDynamic(configJson);
        c.setErrors(errorConf);
        c.setStatics(staticConf);
        c.setWebKeys(webKeys);
        getPersistenceEntryManager().persist(c);
    }

    private static AppConfiguration loadConfFromFile(String filePath) throws JAXBException {
        final JAXBContext jc = JAXBContext.newInstance(AppConfiguration.class);
        final Unmarshaller u = jc.createUnmarshaller();
        return (AppConfiguration) u.unmarshal(new File(filePath));
    }
}
