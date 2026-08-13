package io.jans.staging.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.staging.ContentHash;
import io.jans.staging.ContentType;
import io.jans.staging.FileName;
import io.jans.staging.StagedFile;
import io.jans.staging.Token;
import io.jans.staging.error.TokenNotFound;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StagedFileRepositoryImpl — against a mocked PersistenceEntryManager")
public class StagedFileRepositoryImplTests {

    private static final String BASE = "ou=stagedFiles,o=jans";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String HASH = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

    private PersistenceEntryManager entryManager;
    private StagedFileRepositoryImpl repository;

    @BeforeEach
    void setUp() {

        entryManager = mock(PersistenceEntryManager.class);
        repository = new StagedFileRepositoryImpl(entryManager, BASE);
    }

    private static StagedFile staged(String token, Instant stagedAt) {

        return StagedFile.stage(Token.of(token).getValue(), FileName.of(token + ".xml").getValue(),
            ContentHash.of(HASH).getValue(), 10L, ContentType.of("text/xml"), stagedAt, TTL).getValue();
    }

    private static String dn(String token) {

        return "inum=" + token + "," + BASE;
    }

    @Test
    @DisplayName("GIVEN no existing entry WHEN saved THEN it is persisted, not merged")
    public void saveNewPersists() {

        when(entryManager.find(dn("tok-1"), StagedFileEntry.class, null)).thenReturn(null);

        repository.save(staged("tok-1", NOW));

        verify(entryManager).persist(any(StagedFileEntry.class));
        verify(entryManager, never()).merge(any(StagedFileEntry.class));
    }

    @Test
    @DisplayName("GIVEN an existing entry WHEN saved THEN it is merged, not persisted")
    public void saveExistingMerges() {

        when(entryManager.find(dn("tok-1"), StagedFileEntry.class, null)).thenReturn(new StagedFileEntry());

        repository.save(staged("tok-1", NOW));

        verify(entryManager).merge(any(StagedFileEntry.class));
        verify(entryManager, never()).persist(any(StagedFileEntry.class));
    }

    @Test
    @DisplayName("GIVEN a stored entry WHEN found by token THEN it is rehydrated to an equal aggregate")
    public void findByTokenRehydrates() {

        StagedFile file = staged("tok-1", NOW);
        when(entryManager.find(dn("tok-1"), StagedFileEntry.class, null))
            .thenReturn(StagedFileEntryMapper.toEntry(file));

        assertThat(repository.findByToken(Token.of("tok-1").getValue()).getValue()).isEqualTo(file);
    }

    @Test
    @DisplayName("GIVEN no entry WHEN found by token THEN it fails with TokenNotFound")
    public void findByTokenMissing() {

        assertThat(repository.findByToken(Token.of("nope").getValue()).getError())
            .isInstanceOf(TokenNotFound.class);
    }

    @Test
    @DisplayName("WHEN deleted THEN the entry is removed by DN")
    public void deleteRemovesByDn() {

        repository.delete(Token.of("tok-1").getValue());

        verify(entryManager).remove(dn("tok-1"), StagedFileEntry.class);
    }

    @Test
    @DisplayName("GIVEN STAGED entries WHEN finding expired-unclaimed THEN only those past the cutoff are returned")
    public void findExpiredUnclaimedFiltersByExpiry() {

        StagedFile expired = staged("expired", NOW);                 // expires NOW + TTL
        StagedFile fresh = staged("fresh", NOW.plus(TTL));           // expires NOW + 2*TTL
        Instant reapAt = NOW.plus(TTL).plusSeconds(1);

        when(entryManager.findEntries(eq(BASE), eq(StagedFileEntry.class), any(Filter.class)))
            .thenReturn(List.of(StagedFileEntryMapper.toEntry(expired), StagedFileEntryMapper.toEntry(fresh)));

        List<StagedFile> result = repository.findExpiredUnclaimed(reapAt).getValue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).token().getValue()).isEqualTo("expired");
    }
}
