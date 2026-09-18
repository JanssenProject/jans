package io.jans.staging;

import io.jans.kernel.Result;
import io.jans.staging.error.TokenNotFound;
import io.jans.staging.port.StagedFileRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** In-memory {@link StagedFileRepository} for service tests. */
final class InMemoryStagedFileRepository implements StagedFileRepository {

    private final Map<String, StagedFile> byToken = new HashMap<>();

    @Override
    public Result<StagedFile> save(StagedFile file) {

        byToken.put(file.token().getValue(), file);
        return Result.success(file);
    }

    @Override
    public Result<StagedFile> findByToken(Token token) {

        StagedFile file = byToken.get(token.getValue());
        return file == null ? Result.failure(TokenNotFound.instance()) : Result.success(file);
    }

    @Override
    public Result<Void> delete(Token token) {

        byToken.remove(token.getValue());
        return Result.success(null);
    }

    @Override
    public Result<List<StagedFile>> findExpiredUnclaimed(Instant now) {

        List<StagedFile> expired = new ArrayList<>();
        for (StagedFile file : byToken.values()) {

            if (file.status().isStaged() && file.isExpired(now)) {

                expired.add(file);
            }
        }
        return Result.success(expired);
    }

    int size() {

        return byToken.size();
    }

    boolean contains(Token token) {

        return byToken.containsKey(token.getValue());
    }

    StagedFile get(Token token) {

        return byToken.get(token.getValue());
    }
}
