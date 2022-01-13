package io.jans.ca.common;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Yuriy Zabrovarnyy
 */
public class CoreUtilsTest {

    @Test
    public void cleanUpLogs() {
        Assert.assertEquals("{'client_name':'name',  'rp_id':'test_id'}", CoreUtils.cleanUpLog("{'client_name':'name', 'client_secret':'secret', 'rp_id':'test_id'}"));
    }
}
