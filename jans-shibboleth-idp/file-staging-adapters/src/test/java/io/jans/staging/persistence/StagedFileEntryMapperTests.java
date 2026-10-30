package io.jans.staging.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.staging.ContentHash;
import io.jans.staging.ContentType;
import io.jans.staging.Destination;
import io.jans.staging.FileName;
import io.jans.staging.StagedFile;
import io.jans.staging.Token;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StagedFileEntryMapper — entry ⇄ domain round-trips")
public class StagedFileEntryMapperTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String HASH = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

    private static StagedFile staged(ContentType contentType) {

        return StagedFile.stage(Token.of("tok-1").getValue(), FileName.of("tok-1.xml").getValue(),
            ContentHash.of(HASH).getValue(), 1024L, contentType, NOW, TTL).getValue();
    }

    @Test
    @DisplayName("GIVEN a STAGED file WHEN round-tripped THEN it is value-equal to the original")
    public void stagedRoundTrip() {

        StagedFile file = staged(ContentType.of("application/samlmetadata+xml"));

        StagedFile back = StagedFileEntryMapper.toDomain(StagedFileEntryMapper.toEntry(file)).getValue();

        assertThat(back).isEqualTo(file);
    }

    @Test
    @DisplayName("GIVEN a CLAIMED file WHEN round-tripped THEN status and handle survive")
    public void claimedRoundTrip() {

        StagedFile claimed = staged(ContentType.of("text/xml"))
            .claim(Destination.of("/opt/shibboleth-idp/metadata/").getValue(), NOW).getValue();

        StagedFile back = StagedFileEntryMapper.toDomain(StagedFileEntryMapper.toEntry(claimed)).getValue();

        assertThat(back).isEqualTo(claimed);
        assertThat(back.status().isClaimed()).isTrue();
        assertThat(back.handle().value()).isEqualTo("/opt/shibboleth-idp/metadata/tok-1.xml");
    }

    @Test
    @DisplayName("GIVEN an absent content type WHEN round-tripped THEN it reads back as none()")
    public void absentContentTypeRoundTrip() {

        StagedFile back = StagedFileEntryMapper.toDomain(StagedFileEntryMapper.toEntry(staged(ContentType.none())))
            .getValue();

        assertThat(back.contentType().isPresent()).isFalse();
    }
}
