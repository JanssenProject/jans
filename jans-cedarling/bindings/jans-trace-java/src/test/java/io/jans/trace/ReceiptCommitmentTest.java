// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.expectThrows;

import org.testng.annotations.Test;

public class ReceiptCommitmentTest {

    @Test
    public void forAssertionTakesIdsFromTheVerifiedRecord() throws TraceException {
        ReceiptCommitment receipt = Receipts.first();
        assertEquals(receipt.evidenceDomainId(), Receipts.DOMAIN, "domain");
        assertEquals(receipt.position(), ReceiptPosition.genesis(), "position");
        assertEquals(receipt.receivedAt().toString(), Receipts.FIRST_RECEIVED_AT, "received_at");
        assertEquals(receipt.producerId(), Records.PRODUCER, "producer from the verified record");
        assertEquals(receipt.recordId(), Records.RECORD_ID, "record id from the verified record");
        assertEquals(receipt.contentDigest().toString(), Records.CONTENT_DIGEST, "content_digest from the record");
    }

    @Test
    public void receiptHashMatchesTheIndependentVector() throws TraceException {
        assertEquals(Receipts.first().receiptHash().toString(), Receipts.FIRST_HASH, "first receipt");
        assertEquals(Receipts.second().receiptHash().toString(), Receipts.SECOND_HASH, "second receipt");
    }

    @Test
    public void verifyLinkReturnsTheNewHead() throws TraceException {
        assertEquals(Receipts.first().verifyLink(Digest.genesis()).toString(), Receipts.FIRST_HASH,
                "the first receipt links to the sentinel");
        assertEquals(Receipts.second().verifyLink(Digest.parse(Receipts.FIRST_HASH)).toString(),
                Receipts.SECOND_HASH, "the second links to the first");
    }

    @Test
    public void verifyLinkRejectsAnotherHead() throws TraceException {
        ReceiptLinkException e = expectThrows(ReceiptLinkException.class,
                () -> Receipts.second().verifyLink(Digest.genesis()));
        assertEquals(e.code(), ErrorCode.RECEIPT_LINK_MISMATCH, "the receipt names another predecessor");
    }

    @Test
    public void fromStoredRebuildsTheSameCommitment() throws TraceException {
        ReceiptCommitment ingested = Receipts.first();
        ReceiptCommitment stored = ReceiptCommitment.fromStored(StoredReceipt.builder()
                .evidenceDomainId(Receipts.DOMAIN)
                .position(ReceiptPosition.genesis())
                .receivedAt(ReceivedAt.parse(Receipts.FIRST_RECEIVED_AT))
                .producerId(Records.PRODUCER)
                .recordId(Records.RECORD_ID)
                .contentDigest(Digest.parse(Records.CONTENT_DIGEST))
                .build());
        assertEquals(stored, ingested, "stored values rebuild the ingested commitment");
        assertEquals(stored.receiptHash(), ingested.receiptHash(), "and hash alike");
    }

    @Test
    public void storedReceiptWithEmptyProducerIsRejected() throws TraceException {
        StoredReceipt stored = StoredReceipt.builder()
                .evidenceDomainId(Receipts.DOMAIN)
                .position(ReceiptPosition.genesis())
                .receivedAt(ReceivedAt.fromUnixMillis(0))
                .producerId("")
                .recordId(Records.RECORD_ID)
                .contentDigest(Digest.genesis())
                .build();
        StoredReceiptException e = expectThrows(StoredReceiptException.class,
                () -> ReceiptCommitment.fromStored(stored));
        assertEquals(e.code(), ErrorCode.INVALID_RECEIPT, "no verified record has an empty producer");
    }

    @Test
    public void incompleteStoredReceiptIsRejected() {
        IllegalStateException e = expectThrows(IllegalStateException.class,
                () -> StoredReceipt.builder().evidenceDomainId(Receipts.DOMAIN).build());
        assertEquals(e.getMessage(), "StoredReceipt is missing position", "the message names the first missing field");
    }

    @Test
    public void nullArgumentsAreRejected() throws TraceException {
        VerifiedAssertion verified = Receipts.verified();
        assertThrows(NullPointerException.class,
                () -> ReceiptCommitment.forAssertion(null, ReceiptPosition.genesis(), ReceivedAt.fromUnixMillis(0), verified));
        assertThrows(NullPointerException.class, () -> ReceiptCommitment.fromStored(null));
        assertThrows(NullPointerException.class, () -> Receipts.first().verifyLink(null));
        assertThrows(NullPointerException.class, () -> StoredReceipt.builder().producerId(null));
    }
}
