package io.jans.cedarling.opensearch;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.apache.logging.log4j.*;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AlterSuiteListener implements IAlterSuiteListener {
    
    private Logger logger = LogManager.getLogger(getClass());
    
    @Override
	public void alter(List<XmlSuite> suites) {
	    
        try {
            XmlSuite suite = suites.get(0);
            Path propertiesFilePath = Paths.get(suite.getParameter("propertiesFile"));
            
            Properties prop = new Properties();
            prop.load(Files.newBufferedReader(propertiesFilePath, UTF_8));

            Map<String, String> parameters = new Hashtable<>();
            //do not bother about empty keys... but
            //If a value is found null, this will throw a NPE since we are using a Hashtable
            prop.forEach((Object key, Object value) -> parameters.put(key.toString(), value.toString()));
            
            //query file assumed to be in the same directory of properties file
            String p = "queryFile";
            Path queryFilePath = Path.of(propertiesFilePath.getParent().toString(), parameters.get(p));
            //overwrite file name with actual contents
            parameters.put(p, Files.readString(queryFilePath, UTF_8));            
            
            suite.setParameters(parameters);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

	}
	
}