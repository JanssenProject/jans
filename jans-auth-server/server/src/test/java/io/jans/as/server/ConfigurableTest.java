/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server;

import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.util.StringHelper;
import io.jans.util.properties.FileConfiguration;
import io.jans.util.security.SecurityProviderUtility;
import org.apache.commons.io.IOUtils;
import org.testng.ITestContext;
import org.testng.Reporter;
import org.testng.annotations.BeforeSuite;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Base class for all seam test which require external configuration
 *
 * @author Yuriy Movchan
 * @author Sergey Manoylo
 * @version December 29, 2021
 */
public abstract class ConfigurableTest {

    public static FileConfiguration testData;
    public boolean initialized = false;
    private TestServerCryptoContext cryptoContext;

    @BeforeSuite
    public void initTestSuite(ITestContext context) throws IOException {
        if (initialized) {
            return;
        }
        System.setProperty("testng.ignore.callback.skip", "true");

        Reporter.log("Invoked init test suite method", true);

        String propertiesFile = context.getCurrentXmlTest().getParameter("propertiesFile");
        if (StringHelper.isEmpty(propertiesFile)) {
            propertiesFile = "target/test-classes/testng.properties";
        }

        // Load test parameters
        FileInputStream conf = new FileInputStream(propertiesFile);
        Properties prop;
        try {
            prop = new Properties();
            prop.load(conf);
        } finally {
            IOUtils.closeQuietly(conf);
        }

        Map<String, String> parameters = new HashMap<String, String>();
        for (Entry<Object, Object> entry : prop.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (StringHelper.isEmptyString(key) || StringHelper.isEmptyString(value)) {
                continue;
            }
            parameters.put(key.toString(), value.toString());
        }

        // Override test parameters
        context.getSuite().getXmlSuite().setParameters(parameters);

        SecurityProviderUtility.installBCProvider();
        cryptoContext = TestServerCryptoContext.getInstance();

        initialized = true;
    }

    public TestServerCryptoContext getCryptoContext() {
        return cryptoContext;
    }

    public AuthCryptoProvider getCryptoProvider() {
        return cryptoContext.getCryptoProvider();
    }
}
