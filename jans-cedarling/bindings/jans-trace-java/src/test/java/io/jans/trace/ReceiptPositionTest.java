// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.expectThrows;

import org.testng.annotations.Test;

public class ReceiptPositionTest {

    private static final long MAX_SAFE_INTEGER = (1L << 53) - 1;

    private static Digest hash() throws ParseDigestException {
        return Digest.parse(Receipts.FIRST_HASH);
    }

    @Test
    public void genesisIsSequenceOneNamingTheSentinel() {
        ReceiptPosition genesis = ReceiptPosition.genesis();
        assertEquals(genesis.sequence(), 1, "the first receipt has sequence 1");
        assertEquals(genesis.prevReceiptHash(), Digest.genesis(), "and names the sentinel");
    }

    @Test
    public void laterPositionsKeepTheirValues() throws TraceException {
        ReceiptPosition position = ReceiptPosition.of(MAX_SAFE_INTEGER, hash());
        assertEquals(position.sequence(), MAX_SAFE_INTEGER, "the largest safe sequence is allowed");
        assertEquals(position.prevReceiptHash(), hash(), "predecessor is kept");
        assertEquals(position, ReceiptPosition.of(MAX_SAFE_INTEGER, hash()), "equal values are equal");
    }

    @Test
    public void ofOneWithSentinelIsGenesis() throws ReceiptPositionException {
        assertEquals(ReceiptPosition.of(1, Digest.genesis()), ReceiptPosition.genesis(), "same position");
    }

    private static void assertRejected(long sequence, Digest prev) {
        ReceiptPositionException e = expectThrows(ReceiptPositionException.class,
                () -> ReceiptPosition.of(sequence, prev));
        assertEquals(e.code(), ErrorCode.INVALID_RECEIPT, sequence + " / " + prev + " is not a v1 position");
    }

    @Test
    public void genesisWithPredecessorIsRejected() throws ParseDigestException {
        assertRejected(1, hash());
    }

    @Test
    public void sentinelAfterGenesisIsRejected() {
        assertRejected(2, Digest.genesis());
    }

    @Test
    public void sequenceOutsideTheSafeRangeIsRejected() throws ParseDigestException {
        assertRejected(0, hash());
        assertRejected(-1, hash());
        assertRejected(MAX_SAFE_INTEGER + 1, hash());
    }

    @Test
    public void nullIsRejected() {
        assertThrows(NullPointerException.class, () -> ReceiptPosition.of(2, null));
    }
}
