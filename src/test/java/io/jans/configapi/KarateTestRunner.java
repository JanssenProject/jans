/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi;


import com.intuit.karate.junit5.Karate;
import io.jans.as.common.model.registration.Client;
import io.jans.configapi.util.TestUtil;
import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import org.slf4j.Logger;

@QuarkusTest
public class KarateTestRunner {
	
    @Inject
    Logger log;
	
    @Inject
    TestUtil testUtil;

    @Karate.Test
    Karate testFullPath() throws Exception{
    	Client client = testUtil.init();
        log.trace(" ********************* KarateTestRunner:::testFullPath() - clientid = "+client.getClientId()+" ********************* ");
        String token = this.testUtil.createTestToken();
        System.setProperty("access.token", token);
        log.trace(" ********************* KarateTestRunner:::testFullPath() - token = "+token+" ********************* ");
        
        return Karate.run("src/test/resources/feature");
    }
    
   
}
