package io.jans.configapi.core.test.listener;


import io.jans.util.security.SecurityProviderUtility;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Map;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;

public class AlterSuiteListener implements IAlterSuiteListener {
	
    static PersistenceType persistenceType;

    private static final String FILE_PREFIX = "file:";
    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private static final String NEW_LINE = System.getProperty("line.separator");
    
    private Logger logger = LogManager.getLogger(getClass());
    
	public void alter(List<XmlSuite> suites) {
		
		try {
            SecurityProviderUtility.installBCProvider();
            logger.info("\n\n Parsing XML suite");
	    	XmlSuite suite = suites.get(0);
		
            //Properties with the file: preffix will point to real .json files stored under src/test/resources folder
            String propertiesFile = suite.getParameter("propertiesFile");
       
            Properties prop = new Properties();
            prop.load(Files.newBufferedReader(Paths.get(propertiesFile), DEFAULT_CHARSET));  
        
            persistenceType = PersistenceType.fromString(prop.getProperty("persistenceType"));
            logger.info("Using persistence type = {}", persistenceType);

            Map<String, String> parameters = new Hashtable<>();
            prop.forEach((Object key, Object value) -> parameters.put(key.toString(), decodeFileValue(value.toString())));
            // Override test parameters
            suite.setParameters(parameters);

        } catch (IOException e) {
        	logger.error(e.getMessage(), e);
        }

	}

    private String decodeFileValue(String value) {
        logger.debug("\n\n decodeFileValue");
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
        
        logger.debug("\n\n decodeFileValue - decoded:{}",decoded);
        return decoded;

    }

}
