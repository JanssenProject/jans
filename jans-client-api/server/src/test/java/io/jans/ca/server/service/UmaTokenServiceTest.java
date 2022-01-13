package io.jans.ca.server.service;

import org.testng.annotations.Test;
import io.jans.ca.common.CoreUtils;

import java.util.Calendar;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

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
