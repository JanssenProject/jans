/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server;

import io.jans.as.server.util.Deployments;
import io.jans.util.StringHelper;
import io.jans.util.properties.FileConfiguration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.apache.commons.io.IOUtils;
import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.testng.ITestContext;
import org.testng.Reporter;
import org.testng.annotations.BeforeSuite;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import static org.omnifaces.util.Faces.getServletContext;

/**
 * Base class for all seam test which require external configuration
 *
 * @author Yuriy Movchan
 * @author Sergey Manoylo
 * @version December 29, 2021
 */
@ArquillianSuiteDeployment
public abstract class ConfigurableTest extends Arquillian {

    public static FileConfiguration testData;
    public boolean initialized = false;

    @Deployment
//    @OverProtocol("Servlet 3.0")
    public static Archive<?> createDeployment() {
        return Deployments.createDeployment();
    }

    @BeforeSuite
    public void initTestSuite(ITestContext context) throws IOException {
        if (initialized) {
            return;
        }

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

        initialized = true;
    }

    /**
     * Data Provider, that returns correct arrays, which should be used, when JEE testing platform Arquillian is used.
     *
     * @author Sergey Manoylo
     * @version December 29, 2021
     */
    public static class ArquillianDataProvider {

        private static final Map<String, Integer> calls = new HashMap<String, Integer>();   // contains map: data provider - number of call (counter value) of the function 'provide'

        /**
         * Constructor.
         * <p>
         * Private, so only public static functions should be called.
         */
        private ArquillianDataProvider() {
        }

        /**
         * Returns Array of Data, that should be used by testing framework TestNG.
         * <p>
         * This function returns two-dimensional array, when this function
         * is called 1-st time for some defined provider name.
         * <p>
         * This function returns row of the two-dimensional array, when this function
         * is called 2-nd - N-st time.
         * Number of the row of the two-dimensional array, == (number of call - 1).
         *
         * @param providerName Provider Name.
         * @param providerData Data of the provider (two-dimensional array).
         * @return current array (two-dimensional array or row).
         */
        public synchronized static Object[][] provide(final String providerName, final Object[][] providerData) {
            if (calls.containsKey(providerName)) {
                // get instance and increase calls counter
                Object[][] testCase = new Object[][]{providerData[calls.get(providerName)]};
                calls.put(providerName, (calls.get(providerName) + 1) % providerData.length);
                return testCase;
            } else {
                calls.put(providerName, 0);
                return providerData;
            }
        }

        /**
         * Clears counter of calls for all providers.
         */
        public synchronized static void initCalls() {
            calls.clear();
        }

        /**
         * Clears counter of calls for some provider.
         *
         * @param providerName Provider Name.
         */
        public synchronized static void initCalls(final String providerName) {
            calls.put(providerName, 0);
        }

    }

}
