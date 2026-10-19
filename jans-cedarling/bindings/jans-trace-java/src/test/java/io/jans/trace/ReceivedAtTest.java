// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.expectThrows;

import org.testng.annotations.Test;
import io.jans.trace.uniffi.ErrorCode;
import io.jans.trace.TraceException.ParseReceivedAtException;
import io.jans.trace.TraceException.ReceivedAtRangeException;

public class ReceivedAtTest {

    @Test
    public void fromUnixMillisFormatsUtcWithMilliseconds() throws ReceivedAtRangeException {
        assertEquals(ReceivedAt.fromUnixMillis(Receipts.FIRST_MILLIS).toString(), Receipts.FIRST_RECEIVED_AT,
                "YYYY-MM-DDTHH:MM:SS.sssZ in UTC");
    }

    @Test
    public void rangeEndsAreFormatted() throws ReceivedAtRangeException {
        assertEquals(ReceivedAt.fromUnixMillis(0).toString(), "1970-01-01T00:00:00.000Z", "the epoch");
        assertEquals(ReceivedAt.fromUnixMillis(253_402_300_799_999L).toString(), "9999-12-31T23:59:59.999Z",
                "the last expressible millisecond");
    }

    @Test
    public void afterYear9999IsRejected() {
        ReceivedAtRangeException e = expectThrows(ReceivedAtRangeException.class,
                () -> ReceivedAt.fromUnixMillis(253_402_300_800_000L));
        assertEquals(e.code(), ErrorCode.INVALID_RECEIPT, "the format cannot express year 10000");
    }

    @Test
    public void beforeTheEpochIsRejected() {
        ReceivedAtRangeException e = expectThrows(ReceivedAtRangeException.class, () -> ReceivedAt.fromUnixMillis(-1));
        assertEquals(e.code(), ErrorCode.INVALID_RECEIPT, "Lock stamps the current time, never before 1970");
    }

    @Test
    public void parseRoundTrips() throws TraceException {
        assertEquals(ReceivedAt.parse(Receipts.FIRST_RECEIVED_AT), ReceivedAt.fromUnixMillis(Receipts.FIRST_MILLIS),
                "parsed and formatted values are equal");
        assertEquals(ReceivedAt.parse(Receipts.FIRST_RECEIVED_AT).hashCode(),
                ReceivedAt.fromUnixMillis(Receipts.FIRST_MILLIS).hashCode(), "equal values hash alike");
    }

    private static void assertParseRejected(String text) {
        ParseReceivedAtException e = expectThrows(ParseReceivedAtException.class, () -> ReceivedAt.parse(text));
        assertEquals(e.code(), ErrorCode.INVALID_RECEIPT, text + " is not a v1 receipt timestamp");
    }

    @Test
    public void impossibleDateIsRejected() {
        assertParseRejected("2026-02-30T00:00:00.000Z");
    }

    @Test
    public void otherLayoutsAreRejected() {
        assertParseRejected("2026-09-17T20:15:32Z");
        assertParseRejected("2026-09-17T20:15:32.123+00:00");
        assertParseRejected("2026-09-17 20:15:32.123Z");
    }

    @Test
    public void nullIsRejected() {
        assertThrows(NullPointerException.class, () -> ReceivedAt.parse(null));
    }
}
