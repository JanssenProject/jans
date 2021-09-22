/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server;

import io.jans.as.server.util.Deployments;
import io.jans.util.StringHelper;
import io.jans.util.properties.FileConfiguration;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Base class for all seam test which requre external configuration
 * 
 * @author Yuriy Movchan Date: 05/16/2016
 */
@ArquillianSuiteDeployment
public abstract class ConfigurableTest extends Arquillian {

	public static FileConfiguration testData;
	public boolean initialized = false;

	@Deployment
	@OverProtocol("Servlet 3.0")
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

}
