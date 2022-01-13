/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.9, 15/10/2012
 */

public abstract class BaseComponentTest extends BaseTest {

    public static void sleepSeconds(int p_seconds) {
        try {
            Thread.sleep(p_seconds * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
