package io.jans.staging.persistence;

import io.jans.kernel.Result;
import io.jans.staging.ContentHash;
import io.jans.staging.ContentType;
import io.jans.staging.FileName;
import io.jans.staging.Handle;
import io.jans.staging.StagedFile;
import io.jans.staging.StagedFileStatus;
import io.jans.staging.Token;

import java.util.Date;

/**
 * Translates between the {@link StagedFile} aggregate and its {@link StagedFileEntry}. The token is the
 * {@code inum}; absent content type / handle are stored as empty strings and read back as their null-object
 * values ({@link ContentType#none()} / {@link Handle#none()}), so a round-trip is lossless.
 */
public final class StagedFileEntryMapper {

    private StagedFileEntryMapper() {
    }

    public static StagedFileEntry toEntry(StagedFile file) {

        StagedFileEntry entry = new StagedFileEntry();
        entry.setInum(file.token().getValue());
        entry.setFileName(file.fileName().getValue());
        entry.setContentHash(file.contentHash().getValue());
        entry.setSize(file.size());
        entry.setContentType(file.contentType().value());
        entry.setStatus(file.status().name());
        entry.setStagedAt(Date.from(file.stagedAt()));
        entry.setExpiresAt(Date.from(file.expiresAt()));
        entry.setHandle(file.handle().value());

        return entry;
    }

    public static Result<StagedFile> toDomain(StagedFileEntry entry) {

        Result<Token> token = Token.of(entry.getInum());
        if (token.isFailure()) {

            return Result.failure(token.getError());
        }

        Result<FileName> fileName = FileName.of(entry.getFileName());
        if (fileName.isFailure()) {

            return Result.failure(fileName.getError());
        }

        Result<ContentHash> contentHash = ContentHash.of(entry.getContentHash());
        if (contentHash.isFailure()) {

            return Result.failure(contentHash.getError());
        }

        StagedFile file = StagedFile.rehydrate(
            token.getValue(),
            fileName.getValue(),
            contentHash.getValue(),
            entry.getSize(),
            ContentType.of(entry.getContentType()),
            entry.getStagedAt().toInstant(),
            entry.getExpiresAt().toInstant(),
            StagedFileStatus.valueOf(entry.getStatus()),
            Handle.of(entry.getHandle()));

        return Result.success(file);
    }
}
