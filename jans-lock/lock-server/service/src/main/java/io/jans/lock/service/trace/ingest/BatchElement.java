/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * One element of a bulk request for {@link TraceIngestionService#ingestBatch}: its position in the
 * request and its exact raw bytes, which are fed verbatim to verification as a standalone submission would be.
 *
 * @author Yuriy Movchan
 */
public final class BatchElement {

	private final int index;

	private final byte[] raw;

	public BatchElement(int index, byte[] raw) {
		this.index = index;
		this.raw = Objects.requireNonNull(raw, "raw");
	}

	public int getIndex() {
		return index;
	}

	/** @return a new stream over the element bytes */
	public InputStream openStream() {
		return new ByteArrayInputStream(raw);
	}

}
