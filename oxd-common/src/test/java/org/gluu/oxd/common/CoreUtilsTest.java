package org.gluu.oxd.common;

import junit.framework.Assert;
import org.testng.annotations.Test;

/**
 * @author Yuriy Zabrovarnyy
 */
public class CoreUtilsTest {

    @Test
    public void cleanUpLogs() {
        Assert.assertEquals("{'client_name':'name',  'oxd_id':'test_id'}", CoreUtils.cleanUpLog("{'client_name':'name', 'client_secret':'secret', 'oxd_id':'test_id'}"));
    }
}
