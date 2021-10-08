/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.adminui;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.Test;

public class KarateTestRunner {

    @Karate.Test
    Karate testFullPath() throws Exception {
        return Karate.run("src/test/resources/feature");
    }

}
