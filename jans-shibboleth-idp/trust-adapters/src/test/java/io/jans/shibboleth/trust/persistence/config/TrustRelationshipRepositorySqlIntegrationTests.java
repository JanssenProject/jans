package io.jans.shibboleth.trust.persistence.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.persistence.SqlEntryManagerExtension;

import io.jans.orm.sql.impl.SqlEntryManager;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.shared.Result;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration tests for {@link TrustRelationshipRepositoryImpl} against a real jans-orm SQL backend.
 *
 * <p><b>Gated:</b> skipped unless {@code -Dtrust.it.sql.uri} is set — jans-orm SQL has no embedded/H2 mode,
 * so these need a running Postgres whose schema already contains the {@code jansTrustRelationship} table
 * and a JDBC driver on the classpath. The {@code docker-compose.yaml} in this module starts such an
 * instance, provisioning the table from {@code src/test/resources/init-scripts/00-trustrelationship-init.sql}.
 *
 * <p>Run e.g.:
 * <pre>
 * docker compose -f trust-adapters/docker-compose.yaml up -d
 * mvn -pl trust-adapters test -Dtest=TrustRelationshipRepositorySqlIntegrationTests \
 *   -Dtrust.it.sql.uri=jdbc:postgresql://localhost:5432/jansdb \
 *   -Dtrust.it.sql.schema=public -Dtrust.it.sql.user=jans \
 *   -Dtrust.it.sql.password='VWSAG/ixu14S7EDjDNH4cQ=='
 * </pre>
 */
@DisplayName("TrustRelationshipRepository — SQL integration (gated by -Dtrust.it.sql.uri)")
@ExtendWith(SqlEntryManagerExtension.class)
public class TrustRelationshipRepositorySqlIntegrationTests {

    private static TrustRelationshipRepository repository;

    @BeforeAll
    static void connect(SqlEntryManager entryManager) {

        String baseDn = System.getProperty("trust.it.sql.baseDn", "ou=trustRelationships,o=jans");
        repository = new TrustRelationshipRepositoryImpl(entryManager, baseDn);
    }

    @Test
    @DisplayName("save assigns an id and persists; findById rehydrates the same aggregate; delete removes it")
    public void savesFindsAndDeletes() {

        TrustRelationship draft = draft("IT save/find " + UUID.randomUUID());

        Result<TrustRelationship> saved = repository.save(draft);
        assertThat(saved.isSuccess()).isTrue();
        Id id = saved.getValue().getId();
        assertThat(id.isAssigned()).isTrue();

        try {

            Result<TrustRelationship> found = repository.findById(id);
            assertThat(found.isSuccess()).isTrue();
            assertThat(found.getValue()).isEqualTo(saved.getValue());
        } finally {

            repository.delete(id);
        }

        assertThat(repository.findById(id).isFailure()).isTrue();
    }

    @Test
    @DisplayName("list projects a filtered, paged summary page without touching the JSON blobs")
    public void listsWithFilter() {

        String marker = "IT-list-" + UUID.randomUUID();
        Result<TrustRelationship> a = repository.save(draft(marker + " Alpha"));
        Result<TrustRelationship> b = repository.save(draft(marker + " Beta"));

        try {

            Result<TrustRelationshipSummaryPage> page =
                repository.list(new TrustRelationshipQuery(marker, null, 1, 10));

            assertThat(page.isSuccess()).isTrue();
            assertThat(page.getValue().getTotalElements()).isEqualTo(2L);
            assertThat(page.getValue().getItems())
                .extracting(summary -> summary.getDisplayName())
                .allMatch(name -> name.startsWith(marker));
        } finally {

            repository.delete(a.getValue().getId());
            repository.delete(b.getValue().getId());
        }
    }

    private static TrustRelationship draft(String displayName) {

        return TrustRelationship.create(
            io.jans.shibboleth.trust.config.DisplayName.of(displayName).getValue(),
            Description.of("Unimaginative Test Description"), TrustNature.AGGREGATE).getValue();
    }
}
