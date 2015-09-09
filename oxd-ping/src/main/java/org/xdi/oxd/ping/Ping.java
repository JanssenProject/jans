/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.ping;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IResultMap;
import org.testng.ITestContext;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.xdi.oxd.client.DiscoveryTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 08/04/2014
 */

public class Ping {

    private static final Logger LOG = LoggerFactory.getLogger(Ping.class);

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "8099";
    private static final String DEFAULT_DISCOVERY_URL = "http://seed21.gluu.org/.well-known/openid-configuration";

    private static String parameterValue(String propertyName, String fallbackValue) {
        String value = System.getProperty(propertyName);
        return StringUtils.isNotBlank(value) ? value : fallbackValue;
    }

    private static Map<String, String> createParameterMap() {
        final String host = parameterValue("host", DEFAULT_HOST);
        final String port = parameterValue("port", DEFAULT_PORT);
        final String discoveryUrl = parameterValue("discoveryUrl", DEFAULT_DISCOVERY_URL);

        final Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("host", host);
        parameters.put("port", port);
        parameters.put("discoveryUrl", discoveryUrl);
        return parameters;
    }

    public static void main(String[] args) {

        final TestListenerAdapter tla = new TestListenerAdapter() {

            @Override
            public void onFinish(ITestContext testContext) {
                final IResultMap passedTests = testContext.getPassedTests();
                if (passedTests.size() == 1) {
                    LOG.info("oxD is running.");
                } else {
                    LOG.info("Unable to ping oxD.");
                }
            }
        };

        final List<XmlSuite> suites = new ArrayList<XmlSuite>();
        suites.add(createSuite(createParameterMap()));

        final TestNG testng = new TestNG();
        testng.addListener(tla);
        testng.setXmlSuites(suites);
        testng.run();
    }

    private static XmlSuite createSuite(Map<String, String> parameters) {
        final XmlSuite suite = new XmlSuite();
        suite.setName("TmpSuite");
        suite.setParameters(parameters);

        final XmlTest test = new XmlTest(suite);
        test.setName("TmpTest");
        final List<XmlClass> classes = new ArrayList<XmlClass>();
        classes.add(new XmlClass(DiscoveryTest.class));
        test.setXmlClasses(classes);
        return suite;
    }

}
