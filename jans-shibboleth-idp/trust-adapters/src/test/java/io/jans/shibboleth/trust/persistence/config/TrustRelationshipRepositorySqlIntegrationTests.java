package io.jans.shibboleth.trust.persistence.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.impl.SqlEntryManagerFactory;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.shared.Result;

import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link TrustRelationshipRepositoryImpl} against a real jans-orm SQL backend.
 *
 * <p><b>Gated:</b> skipped unless {@code -Dtrust.it.sql.uri} is set — jans-orm SQL has no embedded/H2 mode,
 * so these need a running MySQL/Postgres whose schema already contains the {@code jansTrustRelationship}
 * table (see {@code src/test/resources/sql/trust-relationship-mysql.sql}) and a JDBC driver on the classpath.
 *
 * <p>Run e.g.:
 * <pre>
 * mvn -pl trust-adapters test -Dtest=TrustRelationshipRepositorySqlIntegrationTests \
 *   -Dtrust.it.sql.uri=jdbc:mysql://localhost:3306/jansdb \
 *   -Dtrust.it.sql.schema=jansdb -Dtrust.it.sql.user=root -Dtrust.it.sql.password=secret
 * </pre>
 */
@DisplayName("TrustRelationshipRepository — SQL integration (gated by -Dtrust.it.sql.uri)")
public class TrustRelationshipRepositorySqlIntegrationTests {

    private static SqlEntryManager entryManager;
    private static TrustRelationshipRepository repository;

    @BeforeAll
    static void connect() {

        String uri = System.getProperty("trust.it.sql.uri");
        assumeTrue(uri != null,
            "SQL integration tests skipped: set -Dtrust.it.sql.uri (+ .schema/.user/.password) to run them");

        Properties properties = new Properties();
        properties.put("sql#connection.uri", uri);
        properties.put("sql#db.schema.name", System.getProperty("trust.it.sql.schema", "public"));
        properties.put("sql#auth.userName", System.getProperty("trust.it.sql.user", "jans"));
        properties.put("sql#auth.userPassword", System.getProperty("trust.it.sql.password", ""));
        properties.put("sql#connection.driver-property.serverTimezone", "UTC");
        properties.put("sql#connection.pool.max-total", "10");
        properties.put("sql#connection.pool.max-idle", "10");
        properties.put("sql#connection.pool.create-max-wait-time-millis", "20000");
        properties.put("sql#connection.pool.max-wait-time-millis", "20000");
        properties.put("sql#connection.pool.min-evictable-idle-time-millis", "1800000");
        properties.put("sql#password.encryption.method", "SSHA-256");

        SqlEntryManagerFactory factory = new SqlEntryManagerFactory();
        factory.create();
        entryManager = factory.createEntryManager(properties);

        String baseDn = System.getProperty("trust.it.sql.baseDn", "ou=trust-relationships,o=jans");
        repository = new TrustRelationshipRepositoryImpl(entryManager, baseDn);
    }

    @AfterAll
    static void disconnect() {

        if (entryManager != null) {

            entryManager.destroy();
        }
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
            Description.of(""), TrustNature.AGGREGATE).getValue();
    }
}
