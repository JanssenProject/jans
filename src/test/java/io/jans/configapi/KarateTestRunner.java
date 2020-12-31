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
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

@QuarkusTest
public class KarateTestRunner {
    
    @Inject
    ApiTestMode apiTestMode;
    
    private Client client;
    
   // @BeforeAll
    public void createClient() throws Exception {
        System.out.println("\n\n KarateTestRunner:::createClient() - Entry ");
        this.client = apiTestMode.init();
        System.out.println("\n\n KarateTestRunner:::createClient() - this.client = "+this.client);
    }

    @Karate.Test
    Karate testFullPath() throws Exception{
        System.out.println("\n\n KarateTestRunner:::testFullPath() - Entry ");
        createClient();
        System.out.println("\n\n KarateTestRunner:::testFullPath() - this.client = "+this.client);
        return Karate.run("src/test/resources/feature");
    }

}
