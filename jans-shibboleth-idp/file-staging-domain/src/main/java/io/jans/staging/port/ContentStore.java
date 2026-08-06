package io.jans.staging.port;

import io.jans.kernel.Result;
import io.jans.staging.ContentType;
import io.jans.staging.Handle;

/**
 * Storage port for the file bytes, addressed by path. {@link #store} writes bytes at an explicit
 * location, recording the content type as intrinsic file metadata, and reports their size and hash;
 * {@link #move} relocates a file from one location to another on claim; {@link #delete} drops a file
 * during reaping. All operations act within the shared document store, so {@code move} transfers no
 * bytes over the wire.
 */
public interface ContentStore {

    Result<StoredContent> store(Handle location, ContentType contentType, byte[] content);

    /** Moves the file from {@code from} to {@code to}. Idempotent once moved. */
    Result<Void> move(Handle from, Handle to);

    /** Idempotent — deleting an absent file still succeeds. */
    Result<Void> delete(Handle location);
}
