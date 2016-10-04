package org.xdi.oxd.server.service;

import org.testng.annotations.Test;

import java.util.Calendar;

import static junit.framework.Assert.*;

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

        assertFalse(UmaTokenService.isExpired(future.getTime()));
        assertTrue(UmaTokenService.isExpired(past.getTime()));
    }
}
