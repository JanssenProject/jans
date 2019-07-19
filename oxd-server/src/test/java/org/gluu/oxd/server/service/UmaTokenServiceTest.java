package org.gluu.oxd.server.service;

import org.testng.annotations.Test;
import org.gluu.oxd.common.CoreUtils;

import java.util.Calendar;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 04/10/2016
 */

public class UmaTokenServiceTest {

    @Test
    public void isExpired() {
        Calendar future = Calendar.getInstance();
        future.add(Calendar.HOUR, 1);

        Calendar past = Calendar.getInstance();
        past.add(Calendar.HOUR, -1);

        assertFalse(CoreUtils.isExpired(future.getTime()));
        assertTrue(CoreUtils.isExpired(past.getTime()));
    }
}
