package io.jans.staging;

import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;
import io.jans.staging.port.ContentStore;
import io.jans.staging.port.FileStorageLayout;
import io.jans.staging.port.StagedFileRepository;
import io.jans.staging.port.StoredContent;
import io.jans.staging.port.TimeSource;
import io.jans.staging.port.TokenGenerator;

import java.time.Duration;
import java.util.List;

/**
 * Coordinates the staging lifecycle over its ports. {@link #stage} mints a token, asks the
 * {@link FileStorageLayout} for a file name and the staging directory, stores the bytes at that
 * explicit location (with the content type as intrinsic metadata), and records a {@link StagedFile}.
 * {@link #claim} loads the record, applies the aggregate's claim guards, moves the file from its
 * staging location to the durable {@link Handle}, and returns it. {@link #reapExpired} drops
 * staged-but-expired files. The aggregate owns the rules; this service only wires the ports.
 */
public final class FileStagingService {

    private final StagedFileRepository repository;
    private final ContentStore contentStore;
    private final FileStorageLayout layout;
    private final TimeSource timeSource;
    private final TokenGenerator tokenGenerator;
    private final Duration ttl;

    private FileStagingService(StagedFileRepository repository, ContentStore contentStore, FileStorageLayout layout,
                               TimeSource timeSource, TokenGenerator tokenGenerator, Duration ttl) {

        this.repository = repository;
        this.contentStore = contentStore;
        this.layout = layout;
        this.timeSource = timeSource;
        this.tokenGenerator = tokenGenerator;
        this.ttl = ttl;
    }

    public static Result<FileStagingService> create(StagedFileRepository repository, ContentStore contentStore,
                                                    FileStorageLayout layout, TimeSource timeSource,
                                                    TokenGenerator tokenGenerator, Duration ttl) {

        if (repository == null) {

            return Result.failure(RequiredValueMissing.forField("repository"));
        }
        if (contentStore == null) {

            return Result.failure(RequiredValueMissing.forField("contentStore"));
        }
        if (layout == null) {

            return Result.failure(RequiredValueMissing.forField("layout"));
        }
        if (timeSource == null) {

            return Result.failure(RequiredValueMissing.forField("timeSource"));
        }
        if (tokenGenerator == null) {

            return Result.failure(RequiredValueMissing.forField("tokenGenerator"));
        }
        if (ttl == null) {

            return Result.failure(RequiredValueMissing.forField("ttl"));
        }
        return Result.success(
            new FileStagingService(repository, contentStore, layout, timeSource, tokenGenerator, ttl));
    }

    /**
     * Stages freshly-uploaded content: name the file, stream it to its staging location (with the
     * content type as metadata), then record a {@code STAGED} file expiring at {@code now + ttl}.
     * Absent content type is stored as {@link ContentType#none()}; empty content is rejected once its
     * size is known (the just-stored empty file is dropped).
     */
    public Result<StagedFile> stage(ContentSource content, ContentType contentType) {

        if (content == null) {

            return Result.failure(RequiredValueMissing.forField("content"));
        }

        ContentType type = contentType == null ? ContentType.none() : contentType;
        Token token = tokenGenerator.generate();
        FileName fileName = layout.fileNameFor(token, type);
        Handle stagingLocation = layout.stagingArea().resolve(fileName);

        Result<StoredContent> stored = contentStore.store(stagingLocation, type, content);
        if (stored.isFailure()) {

            return Result.failure(stored.getError());
        }
        StoredContent storedContent = stored.getValue();
        if (storedContent.size() == 0) {

            contentStore.delete(stagingLocation);
            return Result.failure(RequiredValueMissing.forField("content"));
        }

        Result<StagedFile> staged = StagedFile.stage(token, fileName, storedContent.hash(), storedContent.size(),
            type, timeSource.now(), ttl);
        if (staged.isFailure()) {

            return staged;
        }

        return repository.save(staged.getValue());
    }

    /**
     * Claims a staged file to {@code destination}: apply the aggregate guards, move the file from its
     * staging location to the durable {@link Handle}, persist the claimed record, and return the
     * handle. Idempotent per (token, destination).
     */
    public Result<Handle> claim(Token token, Destination destination) {

        if (token == null) {

            return Result.failure(RequiredValueMissing.forField("token"));
        }
        if (destination == null) {

            return Result.failure(RequiredValueMissing.forField("destination"));
        }

        Result<StagedFile> found = repository.findByToken(token);
        if (found.isFailure()) {

            return Result.failure(found.getError());
        }

        Result<StagedFile> claimed = found.getValue().claim(destination, timeSource.now());
        if (claimed.isFailure()) {

            return Result.failure(claimed.getError());
        }
        StagedFile claimedFile = claimed.getValue();

        Handle stagingLocation = layout.stagingArea().resolve(claimedFile.fileName());
        Result<Void> moved = contentStore.move(stagingLocation, claimedFile.handle());
        if (moved.isFailure()) {

            return Result.failure(moved.getError());
        }

        Result<StagedFile> saved = repository.save(claimedFile);
        if (saved.isFailure()) {

            return Result.failure(saved.getError());
        }

        return Result.success(claimedFile.handle());
    }

    /**
     * Reaps every staged-but-expired file (dropping its bytes and record) and reports how many were
     * removed. Claimed files are out of scope — they have left staging.
     */
    public Result<Integer> reapExpired() {

        Result<List<StagedFile>> expired = repository.findExpiredUnclaimed(timeSource.now());
        if (expired.isFailure()) {

            return Result.failure(expired.getError());
        }

        int reaped = 0;
        for (StagedFile file : expired.getValue()) {

            Handle stagingLocation = layout.stagingArea().resolve(file.fileName());
            contentStore.delete(stagingLocation);
            Result<Void> deleted = repository.delete(file.token());
            if (deleted.isSuccess()) {

                reaped++;
            }
        }
        return Result.success(reaped);
    }
}
