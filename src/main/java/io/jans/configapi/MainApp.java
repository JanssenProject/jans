/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * @author Mougang T.Gasmyr
 *
 */
@QuarkusMain
public class MainApp {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}
