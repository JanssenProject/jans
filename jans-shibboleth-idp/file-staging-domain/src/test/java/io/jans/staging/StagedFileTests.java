package io.jans.staging;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.kernel.RequiredValueMissing;
import io.jans.staging.error.AlreadyClaimed;
import io.jans.staging.error.TokenExpired;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StagedFile — staging, claiming and expiry")
public class StagedFileTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String HASH = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
    private static final String DIR = "/opt/shibboleth-idp/metadata/";

    private static Token token(String value) {

        return Token.of(value).getValue();
    }

    private static StagedFile staged() {

        return StagedFile.stage(token("tok-1"), ContentHash.of(HASH).getValue(), 1024L,
            ContentType.of("application/samlmetadata+xml"), NOW, TTL).getValue();
    }

    @Test
    @DisplayName("GIVEN a fresh upload WHEN staged THEN it is STAGED, unclaimed, and expires at now + ttl")
    public void stageRecordsStagedFileWithTtlExpiry() {

        StagedFile file = staged();

        assertThat(file.status().isStaged()).isTrue();
        assertThat(file.handle().isPresent()).isFalse();
        assertThat(file.stagedAt()).isEqualTo(NOW);
        assertThat(file.expiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(file.size()).isEqualTo(1024L);
        assertThat(file.contentHash().getValue()).isEqualTo(HASH);
        assertThat(file.contentType().getValue()).isEqualTo("application/samlmetadata+xml");
    }

    @Test
    @DisplayName("GIVEN a missing required value WHEN staging THEN it fails with RequiredValueMissing")
    public void stageRejectsMissingRequired() {

        assertThat(StagedFile.stage(null, ContentHash.of(HASH).getValue(), 1L,
            ContentType.none(), NOW, TTL).getError()).isInstanceOf(RequiredValueMissing.class);
        assertThat(StagedFile.stage(token("t"), ContentHash.of(HASH).getValue(), 1L,
            ContentType.none(), null, TTL).getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a staged file WHEN the clock reaches expiry THEN isExpired flips inclusive of the boundary")
    public void isExpiredBoundary() {

        StagedFile file = staged();

        assertThat(file.isExpired(NOW)).isFalse();
        assertThat(file.isExpired(NOW.plus(TTL).minusMillis(1))).isFalse();
        assertThat(file.isExpired(NOW.plus(TTL))).isTrue();
        assertThat(file.isExpired(NOW.plus(TTL).plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("GIVEN a staged, unexpired file WHEN claimed THEN it becomes CLAIMED with the resolved handle")
    public void claimOnStagedProducesClaimedWithHandle() {

        StagedFile file = staged();
        Destination destination = Destination.of(DIR).getValue();

        StagedFile claimed = file.claim(destination, NOW).getValue();

        assertThat(claimed.status().isClaimed()).isTrue();
        assertThat(claimed.handle()).isEqualTo(destination.resolve(file.token()));
        assertThat(claimed.handle().getValue()).isEqualTo(DIR + "tok-1");
    }

    @Test
    @DisplayName("GIVEN a staged file past expiry WHEN claimed THEN it fails with TokenExpired")
    public void claimExpiredFails() {

        StagedFile file = staged();

        assertThat(file.claim(Destination.of(DIR).getValue(), NOW.plus(TTL)).getError())
            .isInstanceOf(TokenExpired.class);
    }

    @Test
    @DisplayName("GIVEN a claimed file WHEN re-claimed to the same destination THEN it is idempotent")
    public void claimIsIdempotentToSameDestination() {

        Destination destination = Destination.of(DIR).getValue();
        StagedFile claimed = staged().claim(destination, NOW).getValue();

        StagedFile again = claimed.claim(destination, NOW.plusSeconds(5)).getValue();

        assertThat(again.status().isClaimed()).isTrue();
        assertThat(again.handle()).isEqualTo(claimed.handle());
    }

    @Test
    @DisplayName("GIVEN a claimed file WHEN re-claimed to a different destination THEN it fails with AlreadyClaimed")
    public void claimToDifferentDestinationAfterClaimFails() {

        StagedFile claimed = staged().claim(Destination.of(DIR).getValue(), NOW).getValue();

        assertThat(claimed.claim(Destination.of("/opt/shibboleth-idp/other/").getValue(), NOW).getError())
            .isInstanceOf(AlreadyClaimed.class);
    }

    @Test
    @DisplayName("GIVEN a stored claimed file WHEN rehydrated THEN every field is restored")
    public void rehydrateRoundTrips() {

        Handle handle = Handle.of(DIR + "tok-1");

        StagedFile file = StagedFile.rehydrate(token("tok-1"), ContentHash.of(HASH).getValue(), 2048L,
            ContentType.of("text/xml"), NOW, NOW.plus(TTL), StagedFileStatus.CLAIMED, handle);

        assertThat(file.status().isClaimed()).isTrue();
        assertThat(file.handle()).isEqualTo(handle);
        assertThat(file.size()).isEqualTo(2048L);
        assertThat(file.contentType().getValue()).isEqualTo("text/xml");
    }
}
