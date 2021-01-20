/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;

import io.jans.as.common.model.registration.Client;
import io.jans.configapi.util.TestUtil;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;

/**
 * @author Yuriy Zabrovarnyy
 */
public class JenkinsTestRunner {  	
	
    @Inject
    Logger log;
	
    @Inject
    TestUtil testUtil;
 
    @Test
    public void testParallel() throws Exception {
    	Client client = testUtil.init();
        log.trace(" ********************* KarateTestRunner:::testFullPath() - clientid = "+client.getClientId()+" ********************* ");
        String token = this.testUtil.createTestToken();
        System.setProperty("access.token", token);
        log.trace(" ********************* KarateTestRunner:::testFullPath() - token = "+token+" ********************* ");
        
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
}
