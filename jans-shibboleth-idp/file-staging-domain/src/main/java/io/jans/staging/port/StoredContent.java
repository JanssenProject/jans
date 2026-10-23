package io.jans.staging.port;

import io.jans.staging.ContentHash;

/**
 * What a {@link ContentStore#store} reports back about the bytes it stored: their size and integrity
 * hash, computed by infrastructure so the domain need not read the bytes.
 */
public final class StoredContent {

    private final long size;
    private final ContentHash hash;

    public StoredContent(long size, ContentHash hash) {

        this.size = size;
        this.hash = hash;
    }

    public long size() {

        return size;
    }

    public ContentHash hash() {

        return hash;
    }
}
