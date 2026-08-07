package io.jans.staging;

import io.jans.kernel.Result;
import io.jans.staging.error.ContentUnreadable;
import io.jans.staging.port.ContentStore;
import io.jans.staging.port.StoredContent;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/** In-memory, path-addressed {@link ContentStore} for service tests. */
final class InMemoryContentStore implements ContentStore {

    private static final class Entry {

        private final byte[] bytes;
        private final ContentType contentType;

        private Entry(byte[] bytes, ContentType contentType) {

            this.bytes = bytes;
            this.contentType = contentType;
        }
    }

    private final Map<String, Entry> byPath = new HashMap<>();

    @Override
    public Result<StoredContent> store(Handle location, ContentType contentType, ContentSource content) {

        byte[] bytes;
        try (InputStream in = content.open()) {

            bytes = in.readAllBytes();
        } catch (IOException e) {

            return Result.failure(ContentUnreadable.instance());
        }
        byPath.put(location.getValue(), new Entry(bytes, contentType));
        return Result.success(new StoredContent(bytes.length, ContentHash.of(sha256Hex(bytes)).getValue()));
    }

    @Override
    public Result<Void> move(Handle from, Handle to) {

        Entry entry = byPath.remove(from.getValue());
        if (entry != null) {

            byPath.put(to.getValue(), entry);
        }
        // Absent source => already moved (idempotent re-claim); nothing to do.
        return Result.success(null);
    }

    @Override
    public Result<Void> delete(Handle location) {

        byPath.remove(location.getValue());
        return Result.success(null);
    }

    boolean has(Handle location) {

        return byPath.containsKey(location.getValue());
    }

    ContentType contentTypeAt(Handle location) {

        Entry entry = byPath.get(location.getValue());
        return entry == null ? null : entry.contentType;
    }

    private static String sha256Hex(byte[] bytes) {

        try {

            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {

                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
