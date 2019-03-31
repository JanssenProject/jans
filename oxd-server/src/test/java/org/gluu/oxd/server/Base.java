package org.gluu.oxd.server;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;

/**
 * @author Yuriy Zabrovarnyy
 */
public class Base {

    @Parameters({"host", "opHost", "redirectUrl"})
    @BeforeClass
    public static void beforeClass(String host, String opHost, String redirectUrl) {
        SetUpTest.beforeSuite(host, opHost, redirectUrl);
    }

    @AfterClass
    public static void afterClass() {
        SetUpTest.afterSuite();
    }
}
