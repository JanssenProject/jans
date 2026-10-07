package io.jans.staging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A supplier of the file bytes to stage, so content is never forced into memory as a {@code byte[]}.
 * The {@link io.jans.staging.port.ContentStore} opens the stream, copies it to storage while computing
 * size and hash in one pass, and closes it — the service and aggregate never touch the bytes. Modelled
 * as a factory ({@code open()} returns a fresh stream) rather than a live {@code InputStream} so the
 * store owns the read lifecycle and the source can be re-opened if needed.
 *
 * <p>An adapter typically passes {@code () -> request.getInputStream()}; {@link #ofBytes(byte[])} is a
 * convenience for small, already-in-memory payloads.
 */
@FunctionalInterface
public interface ContentSource {

    InputStream open() throws IOException;

    static ContentSource ofBytes(byte[] bytes) {

        byte[] copy = bytes.clone();
        return () -> new ByteArrayInputStream(copy);
    }
}
