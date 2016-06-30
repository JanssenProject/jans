/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.seam.mock.SeamTest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.testng.ITestContext;
import org.testng.Reporter;
import org.testng.annotations.BeforeSuite;
import org.xdi.util.StringHelper;
import org.xdi.util.properties.FileConfiguration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Base class for all seam test which requre external configuration
 * 
 * @author Yuriy Movchan
 * Date: 05/16/2016
 */
public abstract class ConfigurableTest extends SeamTest {

	public static FileConfiguration testData;

    @Deployment
    public static Archive<?> createDeployment()
    {
       return ShrinkWrap.create(WebArchive.class, "test.war")
    		   .addAsResource(EmptyAsset.INSTANCE, "seam.properties")
                        .setWebXML("web.xml");
    }

	/**
	 * Prepare configuration before tests execution
	 */
	@BeforeClass
	public static void initTest() {
		String propertiesFile = "./target/test-classes/testng.properties";
		testData = new FileConfiguration(propertiesFile);
	}

    /**
	 * Get configuration
	 */
	public FileConfiguration getTestData() {
		return testData;
	}

	@BeforeSuite
    public void initTestSuite(ITestContext context) throws FileNotFoundException, IOException {
        Reporter.log("Invoked init test suite method \n", true);

        String propertiesFile = context.getCurrentXmlTest().getParameter("propertiesFile");
        if (StringHelper.isEmpty(propertiesFile)) {
            propertiesFile = "target/test-classes/testng.properties";
        }

        // Load test paramters
        //propertiesFile = "/Users/JAVIER/IdeaProjects/oxAuth/Client/target/test-classes/testng.properties";
        FileInputStream conf = new FileInputStream(propertiesFile);
        Properties prop = new Properties();
        prop.load(conf);

        Map<String, String> parameters = new HashMap<String, String>();
        for (Entry<Object, Object> entry : prop.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (StringHelper.isEmptyString(key) || StringHelper.isEmptyString(value)) {
                continue;
            }
            parameters.put(key.toString(), value.toString());
        }

        // Overrided test paramters
        context.getSuite().getXmlSuite().setParameters(parameters);
    }

}
