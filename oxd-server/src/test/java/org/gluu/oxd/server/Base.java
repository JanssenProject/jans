package org.gluu.oxd.server;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;

/**
 * @author Yuriy Zabrovarnyy
 */
public class Base {

    @Parameters({"host", "opHost", "redirectUrls"})
    @BeforeClass
    public static void beforeClass(String host, String opHost, String redirectUrls) {
        SetUpTest.beforeSuite(host, opHost, redirectUrls);
    }

    @AfterClass
    public static void afterClass() {
        SetUpTest.afterSuite();
    }
}
