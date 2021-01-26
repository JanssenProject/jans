/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;

import io.jans.as.common.model.registration.Client;
import io.jans.configapi.util.Util;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
public class JenkinsTestRunner {  	
	
	private static final Logger LOG = LoggerFactory.getLogger(JenkinsTestRunner.class);

    @Test
    public void testParallel() throws Exception {
    	//Client client = testUtil.init();
        //String token = this.testUtil.createTestToken();
       // System.setProperty("access.token", token);
        LOG.error(" ********************* JenkinsTestRunner:::testFullPath() -  ********************* ");
		System.out.println( "JenkinsTestRunner ::::testParallel() - Before Calling testUtil \n");
		testUtil();
		System.out.println( "JenkinsTestRunner ::::testParallel() - After Calling testUtil \n");
        LOG.error(" ********************* JenkinsTestRunner:::testFullPath() - after ********************* ");

        System.setProperty("karate.env", "jenkins");
        Results results = Runner.path("src/test/resources/feature").tags("~@ignore").parallel(5);
        generateReport(results.getReportDir());
        Assertions.assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }

    public static void generateReport(String karateOutputPath) {
        Collection<File> jsonFiles = FileUtils.listFiles(new File(karateOutputPath), new String[]{"json"}, true);
        List<String> jsonPaths = new ArrayList(jsonFiles.size());
        jsonFiles.forEach(file -> jsonPaths.add(file.getAbsolutePath()));
        Configuration config = new Configuration(new File("target"), "karateTesting");
        ReportBuilder reportBuilder = new ReportBuilder(jsonPaths, config);
        reportBuilder.generateReports();
    }
    
    private void testUtil() {
    	try {
    		String clientId = Util.getApiClientId();
    		System.out.println( "JenkinsTestRunner ::::testUtil() - clientId ="+clientId+"\n");
    		
    		String clientPwd = Util.getApiClientPassword();
    		System.out.println( "JenkinsTestRunner ::::testUtil() - clientPwd ="+clientPwd+"\n");
    		
    		Client testClient = Util.createTestClient();
    		System.out.println( "JenkinsTestRunner ::::testUtil() - testClient.getClientId() ="+testClient.getClientId()+"\n");
    		
    		String token = Util.createTestToken();
    		System.out.println( "JenkinsTestRunner ::::testUtil() - token ="+token
    				+"\n");
    		
    	}catch(Exception ex) {
    		ex.printStackTrace();
    		System.out.println( "JenkinsTestRunner ::::testUtil() - ex.getMessage() ="+ex.getMessage()+"\n");
    	}
    }
}
