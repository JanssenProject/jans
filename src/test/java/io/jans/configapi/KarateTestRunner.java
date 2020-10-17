/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi;

import io.quarkus.test.junit.QuarkusTest;

import com.intuit.karate.junit5.Karate;

@QuarkusTest
public class KarateTestRunner {

    @Karate.Test
    Karate testFullPath() {
        //return Karate.run("classpath:karate/tags.feature").tags("@first");
        return Karate.run("src/test/resources/feature");
    }

}
