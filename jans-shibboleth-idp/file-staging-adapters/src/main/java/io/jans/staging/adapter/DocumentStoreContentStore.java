package io.jans.staging.adapter;

import io.jans.kernel.Result;
import io.jans.service.document.store.exception.DocumentException;
import io.jans.service.document.store.provider.DocumentStore;
import io.jans.staging.ContentHash;
import io.jans.staging.ContentSource;
import io.jans.staging.ContentType;
import io.jans.staging.Handle;
import io.jans.staging.error.ContentUnreadable;
import io.jans.staging.port.ContentStore;
import io.jans.staging.port.StoredContent;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * {@link ContentStore} backed by the jans document store. {@link #store} streams the content straight
 * into the store via {@code saveBinaryDocumentStream}, computing size and SHA-256 in the same pass (the
 * bytes are never buffered); the content type rides along as the document's description. {@link #move}
 * is {@code renameDocument} (idempotent when the destination already exists); {@link #delete} is
 * {@code removeDocument}. Any read/write failure surfaces as {@link ContentUnreadable}.
 */
public final class DocumentStoreContentStore implements ContentStore {

    private static final String MODULE = "file-staging";

    private final DocumentStore<?> documentStore;

    public DocumentStoreContentStore(DocumentStore<?> documentStore) {

        this.documentStore = documentStore;
    }

    @Override
    public Result<StoredContent> store(Handle location, ContentType contentType, ContentSource content) {

        MessageDigest sha256 = sha256();

        try (InputStream raw = content.open();
             HashingCountingInputStream counted = new HashingCountingInputStream(raw, sha256)) {

            String stored = documentStore.saveBinaryDocumentStream(
                location.value(), contentType.value(), counted, MODULE);

            if (stored == null) {

                return Result.failure(ContentUnreadable.instance());
            }

            return Result.success(new StoredContent(counted.count(), ContentHash.of(toHex(sha256.digest())).getValue()));
        } catch (IOException | DocumentException e) {

            return Result.failure(ContentUnreadable.instance());
        }
    }

    @Override
    public Result<Void> move(Handle from, Handle to) {

        try {

            if (documentStore.hasDocument(to.value())) {

                return Result.success(null);
            }

            String moved = documentStore.renameDocument(from.value(), to.value());

            // Some providers (e.g. the local filesystem store) report success even when the underlying
            // rename silently no-ops — e.g. the destination directory does not exist. Verify the file
            // actually landed rather than lose content silently.
            if (moved == null || !documentStore.hasDocument(to.value())) {

                return Result.failure(ContentUnreadable.instance());
            }
            return Result.success(null);
        } catch (DocumentException e) {

            return Result.failure(ContentUnreadable.instance());
        }
    }

    @Override
    public Result<Void> delete(Handle location) {

        try {

            documentStore.removeDocument(location.value());
        } catch (DocumentException alreadyGone) {

            // idempotent: dropping an absent document is a success
        }

        return Result.success(null);
    }

    private static MessageDigest sha256() {

        try {

            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String toHex(byte[] bytes) {

        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {

            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * Wraps the upload stream so the document store's read of it also feeds the digest and the byte count.
     */
    private static final class HashingCountingInputStream extends FilterInputStream {

        private final MessageDigest digest;
        private long count;

        private HashingCountingInputStream(InputStream in, MessageDigest digest) {

            super(in);
            this.digest = digest;
        }

        @Override
        public int read() throws IOException {

            int b = in.read();
            if (b != -1) {

                digest.update((byte) b);
                count++;
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {

            int n = in.read(buffer, offset, length);
            if (n > 0) {

                digest.update(buffer, offset, n);
                count += n;
            }
            return n;
        }

        private long count() {

            return count;
        }
    }
}
