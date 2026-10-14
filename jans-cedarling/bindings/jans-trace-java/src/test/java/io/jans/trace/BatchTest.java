// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.expectThrows;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;
import io.jans.trace.uniffi.ErrorCode;

public class BatchTest {

    @Test
    public void bodyJoinsRecordTextsAndParseSplitsThem() throws TraceException {
        String pretty = Records.SIGNED.replace(",", ", ");
        SignedAssertion first = Records.signed();
        SignedAssertion second = SignedAssertion.parse(new JsonBody(pretty), new Limits());

        String body = Batch.body(Arrays.asList(first, second), new Limits());
        assertEquals(body, "[" + Records.SIGNED + "," + pretty + "]", "records keep their own text");

        List<BatchRecord> records = Batch.parse(new JsonBody(body), new Limits());
        assertEquals(records.size(), 2, "one result per record");
        assertEquals(records.get(0).get().text(), Records.SIGNED, "first record text is exact");
        assertEquals(records.get(1).get().text(), pretty, "second record text is exact");
    }

    @Test
    public void badRecordFailsOnlyItself() throws TraceException {
        String body = "[" + Records.SIGNED + ",{\"producer\":\"p\"}]";
        List<BatchRecord> records = Batch.parse(new JsonBody(body), new Limits());
        assertEquals(records.get(0).get().identity().recordId(), Records.RECORD_ID, "good record parses");
        ParseSignedAssertionException e = expectThrows(ParseSignedAssertionException.class,
                () -> records.get(1).get());
        assertEquals(e.code(), ErrorCode.INVALID_ASSERTION_SHAPE, "bad record carries its own code");
        assertEquals(expectThrows(ParseSignedAssertionException.class, () -> records.get(1).get()).code(),
                ErrorCode.INVALID_ASSERTION_SHAPE, "the result can be read again");
    }

    @Test
    public void duplicateInsideARecordIsKept() throws TraceException {
        String body = "[" + Records.SIGNED.replace("{\"kid\":\"key-2026-01\",", "{\"kid\":\"key-2026-01\",\"kid\":\"x\",") + "]";
        List<BatchRecord> records = Batch.parse(new JsonBody(body), new Limits());
        assertEquals(expectThrows(ParseSignedAssertionException.class, () -> records.get(0).get()).code(),
                ErrorCode.DUPLICATE_MEMBER, "the strict parser sees the record's own duplicate");
    }

    private static void assertBodyRejected(String body, Limits limits, ErrorCode code) {
        ParseBatchException e = expectThrows(ParseBatchException.class,
                () -> Batch.parse(new JsonBody(body), limits));
        assertEquals(e.code(), code, body + " should be rejected as " + code);
    }

    @Test
    public void objectBodyIsInvalidBatch() {
        assertBodyRejected(Records.SIGNED, new Limits(), ErrorCode.INVALID_BATCH);
    }

    @Test
    public void brokenBodyIsInvalidJson() {
        assertBodyRejected("[" + Records.SIGNED, new Limits(), ErrorCode.INVALID_JSON);
    }

    @Test
    public void tooManyRecordsExceedSizeLimit() {
        assertBodyRejected("[" + Records.SIGNED + "," + Records.SIGNED + "]",
                new Limits().withMaxBatchRecords(1), ErrorCode.SIZE_LIMIT_EXCEEDED);
    }

    @Test
    public void tooLargeBodyExceedsSizeLimit() {
        assertBodyRejected("[" + Records.SIGNED + "]", new Limits().withMaxBatchBytes(10), ErrorCode.SIZE_LIMIT_EXCEEDED);
    }

    @Test
    public void bodyAboveRecordLimitIsRefused() throws TraceException {
        List<SignedAssertion> records = Arrays.asList(Records.signed(), Records.signed());
        SizeLimitException e = expectThrows(SizeLimitException.class,
                () -> Batch.body(records, new Limits().withMaxBatchRecords(1)));
        assertEquals(e.code(), ErrorCode.SIZE_LIMIT_EXCEEDED, "the producer must split the batch first");
    }

    @Test
    public void nullArgumentsAreRejected() throws TraceException {
        assertThrows(NullPointerException.class, () -> Batch.parse(null, new Limits()));
        assertThrows(NullPointerException.class, () -> Batch.body(null, new Limits()));
        assertThrows(NullPointerException.class,
                () -> Batch.body(Arrays.asList(Records.signed(), null), new Limits()));
    }
}
