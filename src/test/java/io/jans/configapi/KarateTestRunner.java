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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
@ApplicationScoped
public class KarateTestRunner {
    
    @Inject
    ApiTestMode apiTestMode;
    
    @BeforeAll
    public void createTestClient() {       
        Client client = apiTestMode.init();
    }

    @Karate.Test
    Karate testFullPath() throws Exception{
        return Karate.run("src/test/resources/feature");
    }
    
   
}
