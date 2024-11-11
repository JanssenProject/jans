/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.codec.binary.Base64;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import static java.nio.charset.StandardCharsets.UTF_8;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


public class BaseTest {

    protected Logger logger = LogManager.getLogger(getClass());
    protected ObjectMapper mapper = new ObjectMapper();
    private static Map<String, String> propertiesMap = null;

    @BeforeSuite
    public void initTestSuite(ITestContext context) throws Exception {

        logger.info("Invoked initTestSuite of '{}'", context.getCurrentXmlTest().getName());
        String propertiesFile = context.getCurrentXmlTest().getParameter("propertiesFile");
        Properties prop = new Properties();
        prop.load(Files.newBufferedReader(Paths.get(propertiesFile), UTF_8));

        propertiesMap = new Hashtable<>();
        prop.forEach((key, value) -> propertiesMap.put(key.toString(), value.toString()));
        context.getSuite().getXmlSuite().setParameters(propertiesMap);
    }

    @AfterSuite
    public void finalize() {
        //cleanup
        logger.info("After Suite finalize'");
    }


    public String getAccessToken() throws Exception {
        String mainUrl =  propertiesMap.get("token.endpoint");
        String grantType =  propertiesMap.get("token.grant.type");
        String clientId =  propertiesMap.get("test.client.id");
        String clientSecret =  propertiesMap.get("test.client.secret");
        String scopes =  propertiesMap.get("test.scopes");
        String authStr = clientId+':'+clientSecret;

        String token = new String(Base64.decodeBase64(authStr), StandardCharsets.UTF_8);
        String encodedScopes = URLDecoder.decode(scopes, "UTF-8");

       
    }

}
