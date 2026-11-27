package io.jans.ca.plugin.adminui.test.utils;

import io.jans.ca.plugin.adminui.utils.CommonUtils;
import org.testng.annotations.Test;

import java.time.LocalDateTime;

import static org.testng.Assert.assertEquals;

public class CommonUtilsTest {

    @Test
    public void convertStringToLocalDateParsesLegacyDateOnlyValue() {
        assertEquals(CommonUtils.convertStringToLocalDate("2027-01-05"),
                LocalDateTime.of(2027, 1, 5, 0, 0));
    }

    @Test
    public void convertStringToLocalDateParsesTimestamp() {
        assertEquals(CommonUtils.convertStringToLocalDate("2027-01-05T12:30:45"),
                LocalDateTime.of(2027, 1, 5, 12, 30, 45));
    }
}
