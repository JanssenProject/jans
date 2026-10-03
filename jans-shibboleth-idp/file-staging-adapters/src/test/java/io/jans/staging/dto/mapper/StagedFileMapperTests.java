package io.jans.staging.dto.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.staging.ContentHash;
import io.jans.staging.ContentType;
import io.jans.staging.Destination;
import io.jans.staging.FileName;
import io.jans.staging.StagedFile;
import io.jans.staging.Token;
import io.jans.staging.dto.ClaimRequest;
import io.jans.staging.dto.ClaimResult;
import io.jans.staging.dto.StagedFileView;
import io.jans.staging.error.InvalidDestination;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StagedFileMapper — domain ⇄ file-staging DTOs")
public class StagedFileMapperTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String HASH = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

    private static StagedFile staged(ContentType contentType) {

        return StagedFile.stage(Token.of("tok-1").getValue(), FileName.of("tok-1.xml").getValue(),
            ContentHash.of(HASH).getValue(), 19L, contentType, NOW, TTL).getValue();
    }

    @Test
    @DisplayName("GIVEN a staged file WHEN projected to a view THEN token, size, content type, hash and expiry map")
    public void toViewMapsFields() {

        StagedFileView view = StagedFileMapper.toView(staged(ContentType.of("application/samlmetadata+xml")));

        assertThat(view.getToken()).isEqualTo("tok-1");
        assertThat(view.getSize()).isEqualTo(19L);
        assertThat(view.getContentType()).isEqualTo("application/samlmetadata+xml");
        assertThat(view.getSha256()).isEqualTo(HASH);
        assertThat(view.getExpiresAt()).isEqualTo(NOW.plus(TTL).toString());
    }

    @Test
    @DisplayName("GIVEN a staged file with no content type WHEN viewed THEN content_type is null (omitted)")
    public void toViewOmitsAbsentContentType() {

        assertThat(StagedFileMapper.toView(staged(ContentType.none())).getContentType()).isNull();
    }

    @Test
    @DisplayName("GIVEN a claimed file WHEN projected to a claim result THEN the durable handle and metadata map")
    public void toClaimResultMapsHandleAndMetadata() {

        StagedFile claimed = staged(ContentType.of("text/xml"))
            .claim(Destination.of("/opt/shibboleth-idp/metadata/").getValue(), NOW).getValue();

        ClaimResult result = StagedFileMapper.toClaimResult(claimed);

        assertThat(result.getHandle()).isEqualTo("/opt/shibboleth-idp/metadata/tok-1.xml");
        assertThat(result.getSize()).isEqualTo(19L);
        assertThat(result.getContentType()).isEqualTo("text/xml");
        assertThat(result.getSha256()).isEqualTo(HASH);
    }

    @Test
    @DisplayName("GIVEN a valid destination WHEN parsed THEN it yields a Destination")
    public void toDestinationParsesValid() {

        assertThat(StagedFileMapper.toDestination(new ClaimRequest("/opt/shibboleth-idp/metadata/"))
            .getValue().getValue()).isEqualTo("/opt/shibboleth-idp/metadata/");
    }

    @Test
    @DisplayName("GIVEN a blank or absent destination WHEN parsed THEN it fails with InvalidDestination")
    public void toDestinationRejectsBlank() {

        assertThat(StagedFileMapper.toDestination(new ClaimRequest("  ")).getError())
            .isInstanceOf(InvalidDestination.class);
        assertThat(StagedFileMapper.toDestination(null).getError()).isInstanceOf(InvalidDestination.class);
    }
}
