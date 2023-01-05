/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model;

/**
 * @author Jorge Munoz
 * @version March 16, 2022
 */
public abstract class BaseTest {

    public void showTitle(String title) {
        String testTitle = "TEST: " + title;

        System.out.println("#######################################################");
        System.out.println(testTitle);
        System.out.println("#######################################################");
    }

}
