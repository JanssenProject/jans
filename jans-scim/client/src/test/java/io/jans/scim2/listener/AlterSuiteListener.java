package io.jans.scim2.listener;

import io.jans.util.StringHelper;
import io.jans.util.security.SecurityProviderUtility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Map;
import java.util.List;
import java.util.Properties;

public class AlterSuiteListener implements IAlterSuiteListener {
	
    static PersistenceType persistenceType;

    private static final String FILE_PREFIX = "file:";
    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private static final String NEW_LINE = System.getProperty("line.separator");
    
    private Logger logger = LogManager.getLogger(getClass());
    
	public void alter(List<XmlSuite> suites) {
		
		try {
            SecurityProviderUtility.installBCProvider();
            logger.info("Parsing XML suite");
	    	XmlSuite suite = suites.get(0);
		
            //Properties with the file: preffix will point to real .json files stored under src/test/resources folder
            String propertiesFile = suite.getParameter("propertiesFile");
            if (StringHelper.isEmpty(propertiesFile)) {
                propertiesFile = "target/test-classes/testng2.properties";
            }

            Properties prop = new Properties();
            prop.load(Files.newBufferedReader(Paths.get(propertiesFile), DEFAULT_CHARSET));     //do not bother about IO issues here
        
            persistenceType = PersistenceType.fromString(prop.getProperty("persistenceType"));
            logger.info("Using persistence type = {}", persistenceType);

            Map<String, String> parameters = new Hashtable<>();
            //do not bother about empty keys... but
            //If a value is found null, this will throw a NPE since we are using a Hashtable
            prop.forEach((Object key, Object value) -> parameters.put(key.toString(), decodeFileValue(value.toString())));
            // Override test parameters
            suite.setParameters(parameters);

        } catch (IOException e) {
        	logger.error(e.getMessage(), e);
        }

	}

    private String decodeFileValue(String value) {

        String decoded = value;
        if (value.startsWith(FILE_PREFIX)) {
            value = value.substring(FILE_PREFIX.length());    //remove the prefix

            try (BufferedReader bfr = Files.newBufferedReader(Paths.get(value), DEFAULT_CHARSET)) {     //create reader
                //appends every line after another
                decoded = bfr.lines().reduce("", (partial, next) -> partial + NEW_LINE + next);
                if (decoded.length() == 0)
                    logger.warn("Key '{}' is empty", value);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                decoded = null;
            }
        }
        return decoded;

    }

}
