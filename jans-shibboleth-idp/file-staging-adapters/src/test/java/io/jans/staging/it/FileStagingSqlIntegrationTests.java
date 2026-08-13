package io.jans.staging.it;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.service.document.store.StandaloneDocumentStoreProviderFactory;
import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.service.document.store.conf.LocalDocumentStoreConfiguration;
import io.jans.service.document.store.provider.DocumentStore;

import io.jans.staging.ContentSource;
import io.jans.staging.ContentType;
import io.jans.staging.Destination;
import io.jans.staging.FileStagingService;
import io.jans.staging.Handle;
import io.jans.staging.StagedFile;
import io.jans.staging.StagedFileStatus;
import io.jans.staging.Token;
import io.jans.staging.adapter.DefaultFileStorageLayout;
import io.jans.staging.adapter.DocumentStoreContentStore;
import io.jans.staging.adapter.SystemTimeSource;
import io.jans.staging.adapter.UuidTokenGenerator;
import io.jans.staging.persistence.StagedFileRepositoryImpl;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end integration test for the file-staging adapters: real jans-orm SQL for the {@code jansStagedFile}
 * metadata, and a real jans {@code LocalDocumentStore} (over a temp directory) for the content, wired through
 * {@link FileStagingService}.
 *
 * <p><b>Gated:</b> skipped unless {@code -Djans.it.sql.uri} is set — jans-orm SQL has no embedded mode, so
 * this needs a running Postgres whose schema contains the {@code jansStagedFile} table (provisioned from
 * {@code docker/init-scripts/02-staged-file-init.sql}). The {@code docker/docker-compose.yaml} at the repo
 * root starts such an instance (one Postgres for the whole IT suite).
 *
 * <p>Run e.g.:
 * <pre>
 * docker compose -f docker/docker-compose.yaml up -d
 * mvn -pl file-staging-adapters test -Dtest=FileStagingSqlIntegrationTests \
 *   -Djans.it.sql.uri=jdbc:postgresql://localhost:5432/jansdb \
 *   -Djans.it.sql.schema=public -Djans.it.sql.user=jans \
 *   -Djans.it.sql.password='VWSAG/ixu14S7EDjDNH4cQ=='
 * </pre>
 */
@DisplayName("File staging — SQL + document-store integration (gated by -Djans.it.sql.uri)")
@ExtendWith(SqlEntryManagerExtension.class)
public class FileStagingSqlIntegrationTests {

    private static final String STAGING_DIR = "/var/lib/jans/document-staging/";
    private static final String METADATA_DIR = "/opt/shibboleth-idp/metadata/";
    private static final byte[] BYTES = "<EntityDescriptor entityID=\"https://sp.example/\"/>".getBytes();

    private static Path baseDir;
    private static DocumentStore<?> documentStore;
    private static StagedFileRepositoryImpl repository;
    private static FileStagingService service;

    @BeforeAll
    static void connect(SqlEntryManager entryManager) throws Exception {

        baseDir = Files.createTempDirectory("staging-it");

        // The durable destination directory is a deployment precondition (the IdP metadata directory exists
        // in a real install); the local document store's rename does not create it. Mirror that here.
        Files.createDirectories(Path.of(baseDir.toString(), METADATA_DIR.substring(1)));

        DocumentStoreConfiguration configuration = new DocumentStoreConfiguration();
        configuration.setDocumentStoreType(DocumentStoreType.LOCAL);
        LocalDocumentStoreConfiguration local = new LocalDocumentStoreConfiguration();
        local.setBaseLocation(baseDir.toString());
        configuration.setLocalConfiguration(local);

        documentStore = new StandaloneDocumentStoreProviderFactory(null).getDocumentStoreProvider(configuration);

        repository = new StagedFileRepositoryImpl(entryManager,
            System.getProperty("jans.it.sql.baseDn", "ou=stagedFiles,o=jans"));
        DocumentStoreContentStore contentStore = new DocumentStoreContentStore(documentStore);
        DefaultFileStorageLayout layout =
            DefaultFileStorageLayout.withDefaults(Destination.of(STAGING_DIR).getValue());

        service = FileStagingService.create(repository, contentStore, layout,
            new SystemTimeSource(), new UuidTokenGenerator(), Duration.ofMinutes(10)).getValue();
    }

    @AfterAll
    static void cleanup() {

        if (baseDir != null) {

            FileUtils.deleteQuietly(baseDir.toFile());
        }
    }

    @Test
    @DisplayName("stage persists STAGED metadata; claim moves the content and marks it CLAIMED; the durable file reads back intact")
    public void stageThenClaimRoundTrips() throws Exception {

        StagedFile staged = service.stage(ContentSource.ofBytes(BYTES),
            ContentType.of("application/samlmetadata+xml")).getValue();
        Token token = staged.token();

        try {

            assertThat(repository.findByToken(token).getValue().status()).isEqualTo(StagedFileStatus.STAGED);

            Handle handle = service.claim(token, Destination.of(METADATA_DIR).getValue()).getValue();

            StagedFile reloaded = repository.findByToken(token).getValue();
            assertThat(reloaded.status().isClaimed()).isTrue();
            assertThat(reloaded.handle()).isEqualTo(handle);

            try (InputStream in = documentStore.readBinaryDocumentAsStream(handle.getValue())) {

                assertThat(in.readAllBytes()).isEqualTo(BYTES);
            }
        } finally {

            repository.delete(token);
        }
    }

    @Test
    @DisplayName("findExpiredUnclaimed returns STAGED entries past the cutoff and excludes CLAIMED ones")
    public void findExpiredUnclaimedExcludesClaimed() {

        StagedFile pending = service.stage(ContentSource.ofBytes(BYTES), ContentType.none()).getValue();
        StagedFile claimed = service.stage(ContentSource.ofBytes(BYTES), ContentType.none()).getValue();
        service.claim(claimed.token(), Destination.of(METADATA_DIR).getValue());

        try {

            Instant farFuture = Instant.parse("2100-01-01T00:00:00Z");

            assertThat(repository.findExpiredUnclaimed(farFuture).getValue())
                .extracting(file -> file.token().getValue())
                .contains(pending.token().getValue())
                .doesNotContain(claimed.token().getValue());
        } finally {

            repository.delete(pending.token());
            repository.delete(claimed.token());
        }
    }
}
