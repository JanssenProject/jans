package org.gluu.oxd.server;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;

/**
 * @author Yuriy Zabrovarnyy
 */
public class Base {

    @Parameters({"host", "opHost", "redirectUrls", "isLoadTest"})
    @BeforeClass
    public static void beforeClass(String host, String opHost, String redirectUrls, boolean isLoadTest) {
        SetUpTest.beforeSuite(host, opHost, redirectUrls, isLoadTest);
    }

    @AfterClass
    public static void afterClass() {
        SetUpTest.afterSuite();
    }
}
