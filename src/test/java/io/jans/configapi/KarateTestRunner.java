/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi;

import com.intuit.karate.junit5.Karate;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class KarateTestRunner {

    @Karate.Test
    Karate testFullPath() throws Exception{
        return Karate.run("src/test/resources/feature");
    }
    
   
}
