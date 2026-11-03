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
import io.jans.trace.TraceException.ChainHeadException;
import io.jans.trace.TraceException.ParseDigestException;
import io.jans.trace.TraceException.ReceiptLinkException;

public class ChainHeadTest {

    @Test
    public void emptyHeadIsSequenceZeroAtTheSentinel() {
        ChainHead head = ChainHead.empty(Receipts.DOMAIN);
        assertEquals(head.evidenceDomainId(), Receipts.DOMAIN, "domain");
        assertEquals(head.sequence(), 0, "no receipts yet");
        assertEquals(head.hash(), Digest.genesis(), "the sentinel");
    }

    @Test
    public void walkFollowsTheChain() throws TraceException {
        ChainHead head = ChainHead.empty(Receipts.DOMAIN).append(Receipts.first()).append(Receipts.second());
        assertEquals(head.sequence(), 2, "two receipts");
        assertEquals(head.hash().toString(), Receipts.SECOND_HASH, "the head is the last receipt hash");
    }

    @Test
    public void appendLeavesTheOriginalUnchanged() throws TraceException {
        ChainHead empty = ChainHead.empty(Receipts.DOMAIN);
        empty.append(Receipts.first());
        assertEquals(empty.sequence(), 0, "ChainHead is immutable");
    }

    @Test
    public void resumeContinuesAWalk() throws TraceException {
        ChainHead resumed = ChainHead.resume(Receipts.DOMAIN, 1, Digest.parse(Receipts.FIRST_HASH));
        assertEquals(resumed.append(Receipts.second()).hash().toString(), Receipts.SECOND_HASH,
                "a saved head continues where it stopped");
        assertEquals(ChainHead.resume(Receipts.DOMAIN, 0, Digest.genesis()), ChainHead.empty(Receipts.DOMAIN),
                "resuming at zero is the empty head");
    }

    private static void assertAppendRejected(ChainHead head, ReceiptCommitment receipt, String why) {
        ReceiptLinkException e = expectThrows(ReceiptLinkException.class, () -> head.append(receipt));
        assertEquals(e.code(), ErrorCode.RECEIPT_LINK_MISMATCH, why);
    }

    @Test
    public void otherDomainIsRejected() throws TraceException {
        assertAppendRejected(ChainHead.empty("domain-2"), Receipts.first(), "a receipt of another domain");
    }

    @Test
    public void skippedSequenceIsRejected() throws TraceException {
        assertAppendRejected(ChainHead.empty(Receipts.DOMAIN), Receipts.second(), "sequence 2 does not follow 0");
    }

    @Test
    public void wrongPredecessorIsRejected() throws TraceException {
        ChainHead head = ChainHead.resume(Receipts.DOMAIN, 1, Digest.parse(Receipts.SECOND_HASH));
        assertAppendRejected(head, Receipts.second(), "the receipt names another head");
    }

    private static void assertResumeRejected(long sequence, Digest hash) {
        ChainHeadException e = expectThrows(ChainHeadException.class,
                () -> ChainHead.resume(Receipts.DOMAIN, sequence, hash));
        assertEquals(e.code(), ErrorCode.INVALID_RECEIPT, sequence + " / " + hash + " is not a saved head");
    }

    @Test
    public void impossibleSavedHeadsAreRejected() throws ParseDigestException {
        Digest hash = Digest.parse(Receipts.FIRST_HASH);
        assertResumeRejected(0, hash);
        assertResumeRejected(1, Digest.genesis());
        assertResumeRejected(-1, hash);
        assertResumeRejected(1L << 53, hash);
    }

    @Test
    public void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> ChainHead.empty(null));
        assertThrows(NullPointerException.class, () -> ChainHead.resume(Receipts.DOMAIN, 1, null));
        assertThrows(NullPointerException.class, () -> ChainHead.empty(Receipts.DOMAIN).append(null));
    }
}
