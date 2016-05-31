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
import org.xdi.oxd.client.PingTest;

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
    private static final String DEFAULT_OP_HOST = "http://ce-dev.gluu.org";
    private static final String DEFAULT_REDIRECT_URL = "https://client.example.com/cb";
    private static final String DEFAULT_LOGOUT_URL = "https://client.example.com/logout";
    private static final String DEFAULT_POST_LOGOUT_REDIRECT_URL = "https://client.example.com/cb/logout";

    private static String parameterValue(String propertyName, String fallbackValue) {
        String value = System.getProperty(propertyName);
        return StringUtils.isNotBlank(value) ? value : fallbackValue;
    }

    private static Map<String, String> createParameterMap() {
        final String host = parameterValue("host", DEFAULT_HOST);
        final String port = parameterValue("port", DEFAULT_PORT);
        final String opHost = parameterValue("opHost", DEFAULT_OP_HOST);
        final String redirectUrl = parameterValue("redirectUrl", DEFAULT_REDIRECT_URL);
        final String logoutUrl = parameterValue("logoutUrl", DEFAULT_LOGOUT_URL);
        final String postLogoutRedirectUrl = parameterValue("postLogoutRedirectUrl", DEFAULT_POST_LOGOUT_REDIRECT_URL);

        final Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("host", host);
        parameters.put("port", port);
        parameters.put("opHost", opHost);
        parameters.put("redirectUrl", redirectUrl);
        parameters.put("logoutUrl", logoutUrl);
        parameters.put("postLogoutRedirectUrl", postLogoutRedirectUrl);
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
        classes.add(new XmlClass(PingTest.class));
        test.setXmlClasses(classes);
        return suite;
    }

}
