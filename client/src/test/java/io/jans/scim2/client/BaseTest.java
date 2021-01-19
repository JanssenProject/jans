package io.jans.scim2.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.jans.as.model.util.SecurityProviderUtility;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.factory.ScimClientFactory;
import io.jans.scim2.client.rest.ClientSideService;
import io.jans.util.StringHelper;

import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

/**
 * Created by jgomer on 2017-06-09.
 * Base class for tests (as former BaseScimTest) but reads contents for properties from files instead of a having all JSON
 * content written in a single .properties file.
 */
public class BaseTest {

    private static final String FILE_PREFIX="file:";
    private static final Charset DEFAULT_CHARSET=Charset.forName("UTF-8");
    private static final String NEW_LINE=System.getProperty("line.separator");

    protected static ClientSideService client=null;
    protected Logger logger = LogManager.getLogger(getClass());
    protected ObjectMapper mapper=new ObjectMapper();

    @BeforeSuite
    public void initTestSuite(ITestContext context) throws Exception {
        SecurityProviderUtility.installBCProvider();

        logger.info("Invoked initTestSuite of '{}'", context.getCurrentXmlTest().getName());

        //Properties with the file: preffix will point to real .json files stored under src/test/resources folder
        String propertiesFile = context.getCurrentXmlTest().getParameter("propertiesFile");
        if (StringHelper.isEmpty(propertiesFile)) {
            propertiesFile = "target/test-classes/testng2.properties";
        }

        Properties prop = new Properties();
        prop.load(Files.newBufferedReader(Paths.get(propertiesFile), DEFAULT_CHARSET));     //do not bother much about IO issues here

        Map<String, String> parameters = new Hashtable<>();
        //do not bother about empty keys... but
        //If a value is found null, this will throw a NPE since we are using a Hashtable
        prop.forEach((Object key, Object value) -> parameters.put(key.toString(), decodeFileValue(value.toString())));
        // Override test parameters
        context.getSuite().getXmlSuite().setParameters(parameters);

        if (client == null) {
            setupClient(context.getSuite().getXmlSuite().getParameters());
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        
    }

    @AfterSuite
    public void finalize(){
        client.close();
    }

    private void setupClient(Map<String, String> params) throws Exception{

        logger.info("Initializing client...");
        String tokenEndpointAuthnMethod = params.get("tokenEndpointAuthnMethod");
        String domainURL = params.get("domainURL");
        String OIDCMetadataUrl = params.get("OIDCMetadataUrl");
        String clientId = params.get("clientId");

        if (tokenEndpointAuthnMethod.equals("client_secret_basic")) {
        	client = ScimClientFactory.getClient(domainURL, OIDCMetadataUrl, clientId, params.get("clientSecret"));

        } else if (tokenEndpointAuthnMethod.equals("client_secret_post")) {
        	client = ScimClientFactory.getClient(domainURL, OIDCMetadataUrl, clientId, params.get("clientSecret"), true);

        } else if (tokenEndpointAuthnMethod.equals("private_key_jwt")) {
        	client = ScimClientFactory.getClient(domainURL, OIDCMetadataUrl, clientId, 
        		Paths.get(params.get("keyStorePath")), params.get("keyStorePath"), params.get("keyId"));
        } else {
           throw new Exception("Unsupported method for token endpoint authentication: " + tokenEndpointAuthnMethod);
        }

    }

    private String decodeFileValue(String value){

        String decoded = value;
        if (value.startsWith(FILE_PREFIX)) {
            value = value.substring(FILE_PREFIX.length());    //remove the prefix

            try (BufferedReader bfr = Files.newBufferedReader(Paths.get(value), DEFAULT_CHARSET)) {     //create reader
                //appends every line after another
                decoded = bfr.lines().reduce("", (partial, next) -> partial + NEW_LINE + next);
                if (decoded.length()==0)
                    logger.warn("Key '{}' is empty", value);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                decoded=null;
            }
        }
        return decoded;

    }

    public UserResource getDeepCloneUsr(UserResource bean) throws Exception{
        return mapper.readValue(mapper.writeValueAsString(bean), UserResource.class);
    }

}
