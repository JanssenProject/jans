package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class DateUtilTest extends BaseTest {

    @Test
    public void dateToUnixEpoch_dateNull_zero() {
        showTitle("dateToUnixEpoch_dateNull_zero");
        Long unixEpoch = DateUtil.dateToUnixEpoch(null);
        assertNotNull(unixEpoch, "unix epoch is null");
        assertEquals(unixEpoch.longValue(), -1L);
    }

    @Test
    public void dateToUnixEpoch_validDate_correctUnixEpoch() {
        showTitle("dateToUnixEpoch_validDate_correctUnixEpoch");
        Date now = new Date();
        Long unixEpoch = DateUtil.dateToUnixEpoch(now);
        assertNotNull(unixEpoch, "unix epoch is null");
        assertEquals(unixEpoch.longValue(), now.getTime() / 1000L);
    }
}
