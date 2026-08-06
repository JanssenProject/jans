package io.jans.staging;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.staging.error.InvalidDestination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Destination — hygiene validation and handle resolution")
public class DestinationTests {

    private static Token token() {

        return Token.of("tok-1").getValue();
    }

    @Test
    @DisplayName("GIVEN a well-formed absolute path WHEN parsed THEN it is accepted")
    public void acceptsAbsolutePath() {

        assertThat(Destination.of("/opt/shibboleth-idp/metadata/").isSuccess()).isTrue();
    }

    @Test
    @DisplayName("GIVEN a blank, relative, or traversal-bearing path WHEN parsed THEN it is rejected")
    public void rejectsIllFormedPaths() {

        assertThat(Destination.of("   ").getError()).isInstanceOf(InvalidDestination.class);
        assertThat(Destination.of("opt/shibboleth-idp/metadata/").getError()).isInstanceOf(InvalidDestination.class);
        assertThat(Destination.of("/opt/../etc/").getError()).isInstanceOf(InvalidDestination.class);
    }

    @Test
    @DisplayName("GIVEN a directory WHEN a token is resolved THEN the handle is the token-named file under it")
    public void resolvesTokenNamedFile() {

        assertThat(Destination.of("/opt/shibboleth-idp/metadata/").getValue().resolve(token()).getValue())
            .isEqualTo("/opt/shibboleth-idp/metadata/tok-1");
    }

    @Test
    @DisplayName("GIVEN a directory without a trailing slash WHEN resolved THEN exactly one separator is inserted")
    public void normalizesMissingTrailingSlash() {

        assertThat(Destination.of("/opt/shibboleth-idp/metadata").getValue().resolve(token()).getValue())
            .isEqualTo("/opt/shibboleth-idp/metadata/tok-1");
    }
}
