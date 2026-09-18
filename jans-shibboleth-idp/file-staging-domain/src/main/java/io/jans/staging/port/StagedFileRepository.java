package io.jans.staging.port;

import io.jans.kernel.Result;
import io.jans.staging.StagedFile;
import io.jans.staging.Token;

import java.time.Instant;
import java.util.List;

/**
 * Persistence port for staged-file records (the metadata, not the bytes — those are the
 * {@link ContentStore}'s concern). Implemented by an adapter over the document store.
 */
public interface StagedFileRepository {

    Result<StagedFile> save(StagedFile file);

    /** Fails with {@code TokenNotFound} when no record exists for the token. */
    Result<StagedFile> findByToken(Token token);

    /** Idempotent — removing an absent record still succeeds. */
    Result<Void> delete(Token token);

    /** Records still {@code STAGED} and expired as of {@code now} — the reaper's work-list. */
    Result<List<StagedFile>> findExpiredUnclaimed(Instant now);
}
