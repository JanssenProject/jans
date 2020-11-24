/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

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

        if (client==null) {
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
        boolean testMode=Boolean.parseBoolean(System.getProperty("testmode"));

        /*
         To get a simpler client (not one that supports all possible operations as in this case), you can use as class
         parameter any other interface from gluu.scim2.client.rest or org.gluu.oxtrust.ws.rs.scim2 packages. Find an
         example at test method gluu.scim2.client.SampleTest#smallerClient
         */
        if (testMode)
            client=ScimClientFactory.getTestClient(ClientSideService.class, params.get("domainURL"), params.get("OIDCMetadataUrl"));
            //client=ScimClientFactory.getTestClient(ClientSideService.class, params.get("domainURL"), params.get("OIDCMetadataUrl"), "clientId", "clientSecret");
        else
            client=ScimClientFactory.getClient(
                    ClientSideService.class,
                    params.get("domainURL"),
                    params.get("umaAatClientId"),
                    params.get("umaAatClientJksPath"),
                    params.get("umaAatClientJksPassword"),
                    params.get("umaAatClientKeyId"));
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
            }
            catch (IOException e){
                logger.error(e.getMessage(), e);
                decoded=null;
            }
            //No need to close bfr: try-with-resources statements does it
        }
        return decoded;

    }

    public UserResource getDeepCloneUsr(UserResource bean) throws Exception{
        return mapper.readValue(mapper.writeValueAsString(bean), UserResource.class);
    }

}
