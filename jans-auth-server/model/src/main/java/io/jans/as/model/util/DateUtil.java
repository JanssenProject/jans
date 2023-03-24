package io.jans.as.model.util;

import java.util.Date;

public class DateUtil {

    public static Long dateToUnixEpoch(Date date) {
        if (date == null) {
            return -1L;
        }
        return date.getTime() / 1000L;
    }
}
