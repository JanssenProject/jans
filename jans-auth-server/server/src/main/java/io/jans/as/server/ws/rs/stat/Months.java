package io.jans.as.server.ws.rs.stat;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;

/**
 * @author Yuriy Zabrovarnyy
 */
public class Months {

    private static final Logger log = LoggerFactory.getLogger(Months.class);

    public static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    private Months() {
    }

    public static boolean isValid(String months, String startMonth, String endMonth) {
        boolean hasMonths = StringUtils.isNotBlank(months);
        boolean hasRange = StringUtils.isNotBlank(startMonth) && StringUtils.isNotBlank(endMonth);
        if (hasMonths && hasRange) { // if both are present then invalid
            return false;
        }
        return hasMonths || hasRange;
    }

    public static Set<String> getMonths(String months, String startMonth, String endMonth) {
        if (!isValid(months, startMonth, endMonth)) {
            return new LinkedHashSet<>();
        }

        boolean hasMonths = StringUtils.isNotBlank(months);
        if (hasMonths) {
            return getMonths(months);
        }
        return getMonths(startMonth, endMonth);
    }

    public static LocalDate parse(String month) {
        // append first day of month -> "01"
        return LocalDate.parse(month + "01", YYYYMMDD).with(firstDayOfMonth());
    }

    public static Set<String> getMonths(String startMonth, String endMonth) {
        Set<String> monthList = new LinkedHashSet<>();
        if (!checkMonthFormat(startMonth) || !checkMonthFormat(endMonth)) {
            return monthList;
        }

        LocalDate start = parse(startMonth);
        LocalDate end = parse(endMonth);

        LocalDate date = start;

        while (date.isBefore(end)) {
            monthList.add(date.format(YYYYMM));

            date = date.plusMonths(1).with(firstDayOfMonth());
        }

        if (!monthList.isEmpty()) { // add last month
            monthList.add(date.format(YYYYMM));
        }
        return monthList;
    }

    public static boolean checkMonthFormat(String month) {
        if (month.length() == 6) {
            return true;
        }

        log.error("Invalid month `{}`, month must be 6 chars length in format yyyyMM, e.g. 202212", month);
        return false;
    }

    public static Set<String> getMonths(String months) {
        Set<String> monthList = new LinkedHashSet<>();
        if (StringUtils.isBlank(months)) {
            return monthList;
        }

        for (String m : months.split(" ")) {
            m = m.trim();
            if (checkMonthFormat(m)) {
                monthList.add(m);
            }
        }
        return monthList;
    }
}
