/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi;

import com.intuit.karate.junit5.Karate;

import io.jans.as.common.model.registration.Client;
import io.jans.configapi.util.ApiTestMode;
import io.quarkus.test.junit.QuarkusTest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
@ApplicationScoped
public class KarateTestRunner {
    
    @Inject
    ApiTestMode apiTestMode;
    
    private Client client;
    
    @BeforeAll
    public void createTestClient() {
        System.out.println("\n\n @@@@@@@@@@@@@@@@@@@@@ KarateTestRunner:::createTestClient() - Entry ");
        this.client = apiTestMode.init();
        System.out.println("\n\n KarateTestRunner:::createTestClient() - getClientId() = "+client.getClientId());
    }

    @Karate.Test
    Karate testFullPath() throws Exception{
        System.out.println("\n\n KarateTestRunner:::testFullPath() - Entry ");
        System.out.println("\n\n KarateTestRunner:::testFullPath() - client.getClientId() = "+client.getClientId());
        return Karate.run("src/test/resources/feature");
    }
    
    
    /*
     * @AfterAll public void deleteTestClient() { System.out.
     * println("\n\n ************* KarateTestRunner:::deleteTestClient() - Entry - client.getClientId() = "
     * +client.getClientId());
     * 
     * apiTestMode.deleteTestClient(client.getClientId()); client = null;
     * 
     * }
     */
      
      @BeforeEach
      public void beforeEach() { 
          System.out.
      println("\n\n ############# KarateTestRunner:::beforeEach() - Entry #############"); 
          
         
      }
}
