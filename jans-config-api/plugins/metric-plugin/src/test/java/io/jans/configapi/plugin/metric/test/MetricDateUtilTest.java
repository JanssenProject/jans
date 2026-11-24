/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.metric.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import io.jans.configapi.plugin.metric.util.MetricDateUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.testng.annotations.Test;

/**
 * Unit test (no server needed) of the start/end date parsing of the metric endpoints.
 */
public class MetricDateUtilTest {

    private static Date end(String value) {
        return MetricDateUtil.toDate(MetricDateUtil.parseEndDateTime(value));
    }

    private static Date start(String value) {
        return MetricDateUtil.toDate(MetricDateUtil.parseDateTime(value));
    }

    @Test
    public void wholeSecondEndIsWidenedToLastMillisecond() {
        assertEquals(end("2026-11-24T23:59:59Z"), Date.from(Instant.parse("2026-11-24T23:59:59.999Z")));
    }

    @Test
    public void explicitZeroFractionEndIsWidened() {
        assertEquals(end("2026-11-24T23:59:59.000Z"), Date.from(Instant.parse("2026-11-24T23:59:59.999Z")));
    }

    @Test
    public void endWithoutOffsetIsWidened() {
        assertEquals(end("2026-11-24T23:59:59"), Date.from(Instant.parse("2026-11-24T23:59:59.999Z")));
    }

    @Test
    public void endWithOffsetIsNormalizedToUtcThenWidened() {
        assertEquals(end("2026-11-25T00:59:59+01:00"), Date.from(Instant.parse("2026-11-24T23:59:59.999Z")));
    }

    @Test
    public void endWithMilliseconds() {
        assertEquals(end("2026-11-24T23:59:59.500Z"), Date.from(Instant.parse("2026-11-24T23:59:59.500Z")));
        assertEquals(end("2026-11-24T23:59:59.999Z"), Date.from(Instant.parse("2026-11-24T23:59:59.999Z")));
    }

    @Test
    public void dateOnlyEndIsEndOfDay() {
        assertEquals(end("2026-11-24"), Date.from(Instant.parse("2026-11-24T23:59:59.999Z")));
    }

    @Test
    public void legacyDateEndIsEndOfDay() {
        assertEquals(end("24-11-2026"), Date.from(Instant.parse("2026-11-24T23:59:59.999Z")));
    }

    @Test
    public void blankEndIsNull() {
        assertNull(MetricDateUtil.parseEndDateTime(" "));
        assertNull(MetricDateUtil.parseEndDateTime(null));
    }

    @Test(expectedExceptions = DateTimeParseException.class)
    public void invalidEndIsRejected() {
        MetricDateUtil.parseEndDateTime("not-a-date");
    }

    @Test
    public void startIsNotWidened() {
        assertEquals(start("2026-11-24T00:00:00Z"), Date.from(Instant.parse("2026-11-24T00:00:00Z")));
        assertEquals(start("2026-11-24"), Date.from(Instant.parse("2026-11-24T00:00:00Z")));
        assertEquals(start("24-11-2026"), Date.from(Instant.parse("2026-11-24T00:00:00Z")));
    }

    @Test
    public void dailyRowEndingAt999IsContainedByWholeSecondAndDateOnlyEnd() {
        Date rowEnd = Date.from(Instant.parse("2026-11-24T23:59:59.999Z"));
        assertTrue(!rowEnd.after(end("2026-11-24T23:59:59Z")));
        assertTrue(!rowEnd.after(end("2026-11-24")));
        // the unwidened value would have missed it
        assertTrue(rowEnd.after(MetricDateUtil.toDate(LocalDateTime.parse("2026-11-24T23:59:59"))));
    }

}
