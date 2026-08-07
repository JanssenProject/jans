package io.jans.staging;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.kernel.RequiredValueMissing;
import io.jans.staging.error.AlreadyClaimed;
import io.jans.staging.error.ContentUnreadable;
import io.jans.staging.error.TokenExpired;
import io.jans.staging.error.TokenNotFound;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FileStagingService — stage, claim and reap over the ports")
public class FileStagingServiceTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String DIR = "/opt/shibboleth-idp/metadata/";
    private static final String STAGING_DIR = "/var/lib/jans/staging/";
    private static final byte[] BYTES = "<EntityDescriptor/>".getBytes();

    private InMemoryStagedFileRepository repository;
    private InMemoryContentStore contentStore;
    private FixedStorageLayout layout;
    private FixedTimeSource timeSource;
    private FileStagingService service;

    @BeforeEach
    void setUp() {

        repository = new InMemoryStagedFileRepository();
        contentStore = new InMemoryContentStore();
        layout = new FixedStorageLayout(Destination.of(STAGING_DIR).getValue());
        timeSource = new FixedTimeSource(NOW);
        service = FileStagingService.create(repository, contentStore, layout, timeSource,
            new SequentialTokenGenerator(), TTL).getValue();
    }

    private static Destination destination(String dir) {

        return Destination.of(dir).getValue();
    }

    private static ContentSource content() {

        return ContentSource.ofBytes(BYTES);
    }

    private Handle stagingLocationOf(StagedFile file) {

        return layout.stagingArea().resolve(file.fileName());
    }

    @Test
    @DisplayName("GIVEN a null collaborator WHEN creating the service THEN it fails with RequiredValueMissing")
    public void createGuardsCollaborators() {

        assertThat(FileStagingService.create(null, contentStore, layout, timeSource,
            new SequentialTokenGenerator(), TTL).getError()).isInstanceOf(RequiredValueMissing.class);
        assertThat(FileStagingService.create(repository, contentStore, null, timeSource,
            new SequentialTokenGenerator(), TTL).getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN bytes WHEN staged THEN a STAGED record is persisted and the bytes land at the staging path with their content type")
    public void stagePersistsRecordAndBytes() {

        StagedFile file = service.stage(content(), ContentType.of("application/samlmetadata+xml")).getValue();

        assertThat(file.status().isStaged()).isTrue();
        assertThat(file.size()).isEqualTo(BYTES.length);
        assertThat(file.expiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(file.fileName().getValue()).isEqualTo(file.token().getValue() + ".xml");
        assertThat(repository.contains(file.token())).isTrue();

        Handle stagingLocation = stagingLocationOf(file);
        assertThat(contentStore.has(stagingLocation)).isTrue();
        assertThat(contentStore.contentTypeAt(stagingLocation).getValue()).isEqualTo("application/samlmetadata+xml");
    }

    @Test
    @DisplayName("GIVEN empty content WHEN staged THEN it fails with RequiredValueMissing")
    public void stageRejectsEmptyContent() {

        assertThat(service.stage(ContentSource.ofBytes(new byte[0]), ContentType.none()).getError())
            .isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a content source that cannot be read WHEN staged THEN it fails with ContentUnreadable")
    public void stageFailsWhenContentUnreadable() {

        ContentSource broken = () -> {

            throw new IOException("stream aborted");
        };

        assertThat(service.stage(broken, ContentType.none()).getError()).isInstanceOf(ContentUnreadable.class);
    }

    @Test
    @DisplayName("GIVEN a staged file WHEN claimed THEN the file moves to the durable handle and the record is CLAIMED")
    public void claimMovesBytesAndMarksClaimed() {

        StagedFile staged = service.stage(content(), ContentType.of("text/xml")).getValue();
        Handle stagingLocation = stagingLocationOf(staged);

        Handle handle = service.claim(staged.token(), destination(DIR)).getValue();

        assertThat(handle.getValue()).isEqualTo(DIR + staged.fileName().getValue());
        assertThat(contentStore.has(handle)).isTrue();
        assertThat(contentStore.has(stagingLocation)).isFalse();
        assertThat(repository.get(staged.token()).status().isClaimed()).isTrue();
    }

    @Test
    @DisplayName("GIVEN an unknown token WHEN claimed THEN it fails with TokenNotFound")
    public void claimUnknownTokenFails() {

        assertThat(service.claim(Token.of("nope").getValue(), destination(DIR)).getError())
            .isInstanceOf(TokenNotFound.class);
    }

    @Test
    @DisplayName("GIVEN a staged file past expiry WHEN claimed THEN it fails with TokenExpired")
    public void claimExpiredFails() {

        StagedFile staged = service.stage(content(), ContentType.none()).getValue();
        timeSource.set(NOW.plus(TTL));

        assertThat(service.claim(staged.token(), destination(DIR)).getError())
            .isInstanceOf(TokenExpired.class);
    }

    @Test
    @DisplayName("GIVEN a claimed file WHEN re-claimed to the same destination THEN it is idempotent")
    public void claimIsIdempotentToSameDestination() {

        StagedFile staged = service.stage(content(), ContentType.none()).getValue();
        Handle first = service.claim(staged.token(), destination(DIR)).getValue();

        Handle again = service.claim(staged.token(), destination(DIR)).getValue();

        assertThat(again).isEqualTo(first);
        assertThat(contentStore.has(first)).isTrue();
    }

    @Test
    @DisplayName("GIVEN a claimed file WHEN re-claimed elsewhere THEN it fails with AlreadyClaimed")
    public void claimElsewhereFails() {

        StagedFile staged = service.stage(content(), ContentType.none()).getValue();
        service.claim(staged.token(), destination(DIR));

        assertThat(service.claim(staged.token(), destination("/opt/shibboleth-idp/other/")).getError())
            .isInstanceOf(AlreadyClaimed.class);
    }

    @Test
    @DisplayName("GIVEN expired unclaimed and a claimed file WHEN reaped THEN only the unclaimed expired are removed")
    public void reapRemovesOnlyUnclaimedExpired() {

        StagedFile toReapA = service.stage(content(), ContentType.none()).getValue();
        StagedFile toReapB = service.stage(content(), ContentType.none()).getValue();
        StagedFile claimed = service.stage(content(), ContentType.none()).getValue();
        service.claim(claimed.token(), destination(DIR));

        timeSource.set(NOW.plus(TTL).plusSeconds(1));
        int reaped = service.reapExpired().getValue();

        assertThat(reaped).isEqualTo(2);
        assertThat(repository.contains(toReapA.token())).isFalse();
        assertThat(repository.contains(toReapB.token())).isFalse();
        assertThat(contentStore.has(stagingLocationOf(toReapA))).isFalse();
        assertThat(repository.get(claimed.token()).status().isClaimed()).isTrue();
    }
}
