package io.jans.staging;

import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;
import io.jans.staging.error.AlreadyClaimed;
import io.jans.staging.error.TokenExpired;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A file held in the staging area, identified by its {@link Token}. Lifecycle is {@code STAGED →
 * CLAIMED}: {@link #stage} records a freshly-uploaded file with a time-to-live; {@link #claim} takes
 * ownership, deriving the durable {@link Handle} from the destination and the token. Expiry of an
 * unclaimed file is decided against a supplied clock ({@link #isExpired}) so the reaper can collect
 * it. The aggregate is immutable — {@code claim} returns the claimed instance.
 */
public final class StagedFile {

    private final Token token;
    private final ContentHash contentHash;
    private final long size;
    private final ContentType contentType;
    private final Instant stagedAt;
    private final Instant expiresAt;
    private final StagedFileStatus status;
    private final Handle handle;

    private StagedFile(Token token, ContentHash contentHash, long size, ContentType contentType,
                       Instant stagedAt, Instant expiresAt, StagedFileStatus status, Handle handle) {

        this.token = token;
        this.contentHash = contentHash;
        this.size = size;
        this.contentType = contentType;
        this.stagedAt = stagedAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.handle = handle;
    }

    /**
     * Records a freshly-uploaded file as {@code STAGED}, expiring at {@code now + ttl}. The bytes,
     * their hash and size are supplied by infrastructure; the domain only guards presence.
     */
    public static Result<StagedFile> stage(Token token, ContentHash contentHash, long size,
                                           ContentType contentType, Instant now, Duration ttl) {

        if (token == null) {

            return Result.failure(RequiredValueMissing.forField("token"));
        }
        if (contentHash == null) {

            return Result.failure(RequiredValueMissing.forField("contentHash"));
        }
        if (contentType == null) {

            return Result.failure(RequiredValueMissing.forField("contentType"));
        }
        if (now == null) {

            return Result.failure(RequiredValueMissing.forField("now"));
        }
        if (ttl == null) {

            return Result.failure(RequiredValueMissing.forField("ttl"));
        }
        return Result.success(new StagedFile(token, contentHash, size, contentType,
            now, now.plus(ttl), StagedFileStatus.STAGED, Handle.none()));
    }

    /**
     * Reconstructs a stored staged file verbatim (no rules run). Used by the persistence mapper.
     */
    public static StagedFile rehydrate(Token token, ContentHash contentHash, long size, ContentType contentType,
                                       Instant stagedAt, Instant expiresAt, StagedFileStatus status, Handle handle) {

        return new StagedFile(token, contentHash, size, contentType, stagedAt, expiresAt, status, handle);
    }

    /**
     * Takes ownership of the file, moving it (logically) to {@code destination}. Succeeds and returns
     * the claimed instance carrying the derived {@link Handle}. Idempotent when re-claimed to the same
     * destination; a claim to a different destination once claimed is {@link AlreadyClaimed}; claiming
     * an expired staged file is {@link TokenExpired}.
     */
    public Result<StagedFile> claim(Destination destination, Instant now) {

        if (destination == null) {

            return Result.failure(RequiredValueMissing.forField("destination"));
        }
        if (now == null) {

            return Result.failure(RequiredValueMissing.forField("now"));
        }

        Handle target = destination.resolve(token);

        if (status.isClaimed()) {

            return handle.equals(target)
                ? Result.success(this)
                : Result.failure(AlreadyClaimed.instance());
        }

        if (isExpired(now)) {

            return Result.failure(TokenExpired.instance());
        }

        return Result.success(new StagedFile(token, contentHash, size, contentType,
            stagedAt, expiresAt, StagedFileStatus.CLAIMED, target));
    }

    public boolean isExpired(Instant now) {

        return !now.isBefore(expiresAt);
    }

    public Token token() {

        return token;
    }

    public ContentHash contentHash() {

        return contentHash;
    }

    public long size() {

        return size;
    }

    public ContentType contentType() {

        return contentType;
    }

    public Instant stagedAt() {

        return stagedAt;
    }

    public Instant expiresAt() {

        return expiresAt;
    }

    public StagedFileStatus status() {

        return status;
    }

    public Handle handle() {

        return handle;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {

            return true;
        }
        if (!(o instanceof StagedFile)) {

            return false;
        }
        StagedFile other = (StagedFile) o;
        return size == other.size
            && token.equals(other.token)
            && contentHash.equals(other.contentHash)
            && contentType.equals(other.contentType)
            && stagedAt.equals(other.stagedAt)
            && expiresAt.equals(other.expiresAt)
            && status == other.status
            && handle.equals(other.handle);
    }

    @Override
    public int hashCode() {

        return Objects.hash(token, contentHash, size, contentType, stagedAt, expiresAt, status, handle);
    }
}
