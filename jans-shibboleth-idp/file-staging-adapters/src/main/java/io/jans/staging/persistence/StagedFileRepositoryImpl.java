package io.jans.staging.persistence;

import io.jans.kernel.Result;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;
import io.jans.staging.StagedFile;
import io.jans.staging.StagedFileStatus;
import io.jans.staging.Token;
import io.jans.staging.error.TokenNotFound;
import io.jans.staging.port.StagedFileRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code jans-orm}-backed {@link StagedFileRepository} for staged-file metadata. A file's DN is
 * {@code inum=<token>,<baseDn>}. {@link #findExpiredUnclaimed} queries the {@code STAGED} entries and
 * applies the expiry cutoff in memory (via the aggregate), avoiding a backend-specific date-range filter.
 */
public final class StagedFileRepositoryImpl implements StagedFileRepository {

    private final PersistenceEntryManager entryManager;
    private final String baseDn;

    public StagedFileRepositoryImpl(PersistenceEntryManager entryManager, String baseDn) {

        this.entryManager = entryManager;
        this.baseDn = baseDn;
    }

    @Override
    public Result<StagedFile> save(StagedFile file) {

        StagedFileEntry entry = StagedFileEntryMapper.toEntry(file);
        entry.setDn(dnFor(entry.getInum()));

        if (find(entry.getDn()) != null) {

            entryManager.merge(entry);
        } else {

            entryManager.persist(entry);
        }

        return Result.success(file);
    }

    @Override
    public Result<StagedFile> findByToken(Token token) {

        StagedFileEntry entry = find(dnFor(token.getValue()));

        if (entry == null) {

            return Result.failure(TokenNotFound.instance());
        }

        return StagedFileEntryMapper.toDomain(entry);
    }

    @Override
    public Result<Void> delete(Token token) {

        try {

            entryManager.remove(dnFor(token.getValue()), StagedFileEntry.class);
        } catch (EntryPersistenceException alreadyGone) {

            // idempotent: removing an absent entry is a success
        }

        return Result.success(null);
    }

    @Override
    public Result<List<StagedFile>> findExpiredUnclaimed(Instant now) {

        Filter filter = Filter.createEqualityFilter("jansStagedFileStatus", StagedFileStatus.STAGED.name());

        List<StagedFileEntry> entries = entryManager.findEntries(baseDn, StagedFileEntry.class, filter);

        List<StagedFile> expired = new ArrayList<>();

        for (StagedFileEntry entry : entries) {

            Result<StagedFile> domain = StagedFileEntryMapper.toDomain(entry);

            if (domain.isFailure()) {

                return Result.failure(domain.getError());
            }

            if (domain.getValue().isExpired(now)) {

                expired.add(domain.getValue());
            }
        }

        return Result.success(expired);
    }

    private StagedFileEntry find(String dn) {

        try {

            return entryManager.find(dn, StagedFileEntry.class, null);
        } catch (EntryPersistenceException notFound) {

            return null;
        }
    }

    private String dnFor(String inum) {

        return "inum=" + inum + "," + baseDn;
    }
}
