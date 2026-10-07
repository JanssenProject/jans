package io.jans.staging.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.staging.ContentType;
import io.jans.staging.Destination;
import io.jans.staging.Token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DefaultFileStorageLayout — token-based names with content-type extensions")
public class DefaultFileStorageLayoutTests {

    private static final DefaultFileStorageLayout LAYOUT =
        DefaultFileStorageLayout.withDefaults(Destination.of("/var/lib/jans/staging/").getValue());

    private static Token token() {

        return Token.of("tok-1").getValue();
    }

    @Test
    @DisplayName("GIVEN an XML metadata content type WHEN naming THEN the file gets an .xml extension")
    public void xmlContentTypeGivesXmlExtension() {

        assertThat(LAYOUT.fileNameFor(token(), ContentType.of("application/samlmetadata+xml")).getValue())
            .isEqualTo("tok-1.xml");
    }

    @Test
    @DisplayName("GIVEN an absent or unmapped content type WHEN naming THEN the default extension is used")
    public void unknownContentTypeFallsBackToDefault() {

        assertThat(LAYOUT.fileNameFor(token(), ContentType.none()).getValue()).isEqualTo("tok-1.bin");
        assertThat(LAYOUT.fileNameFor(token(), ContentType.of("application/pdf")).getValue())
            .isEqualTo("tok-1.bin");
    }

    @Test
    @DisplayName("GIVEN a configured staging area WHEN asked THEN it is returned")
    public void exposesStagingArea() {

        assertThat(LAYOUT.stagingArea().getValue()).isEqualTo("/var/lib/jans/staging/");
    }
}
