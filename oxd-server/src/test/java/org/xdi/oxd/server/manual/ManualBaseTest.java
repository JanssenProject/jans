package org.xdi.oxd.server.manual;

import org.junit.After;
import org.junit.Before;
import org.testng.annotations.Parameters;
import org.xdi.oxd.server.SetUpTest;

/**
 * @author yuriyz
 */
public class ManualBaseTest {

    @Parameters({"host", "opHost", "redirectUrl"})
    @Before
    public void before(String host, String opHost, String redirectUrl) {
        SetUpTest.beforeSuite(host, opHost, redirectUrl);
    }

    @After
    public void after() {
        SetUpTest.afterSuite();
    }
}
