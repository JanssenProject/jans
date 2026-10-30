package io.jans.staging;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.kernel.RequiredValueMissing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Staging value objects — validation and absence")
public class ValueObjectsTests {

    @Test
    @DisplayName("GIVEN a blank token WHEN parsed THEN it fails with RequiredValueMissing naming the owning type")
    public void tokenRejectsBlank() {

        assertThat(Token.of("  ").getError()).isInstanceOf(RequiredValueMissing.class);

        RequiredValueMissing error = (RequiredValueMissing) Token.of(null).getError();
        assertThat(error.getOwner()).isEqualTo(Token.class);
        assertThat(error.namesField()).isFalse();

        assertThat(Token.of("tok-1").getValue().getValue()).isEqualTo("tok-1");
    }

    @Test
    @DisplayName("GIVEN a blank content hash WHEN parsed THEN it fails with RequiredValueMissing")
    public void contentHashRejectsBlank() {

        assertThat(ContentHash.of("").getError()).isInstanceOf(RequiredValueMissing.class);
        assertThat(ContentHash.of("abc123").getValue().getValue()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("GIVEN a blank file name WHEN parsed THEN it fails with RequiredValueMissing")
    public void fileNameRejectsBlank() {

        assertThat(FileName.of(" ").getError()).isInstanceOf(RequiredValueMissing.class);
        assertThat(FileName.of("tok-1.xml").getValue().getValue()).isEqualTo("tok-1.xml");
    }

    @Test
    @DisplayName("GIVEN an absent content type WHEN parsed THEN it is the none() null-object")
    public void contentTypeModelsAbsenceAsNullObject() {

        assertThat(ContentType.of(null).isPresent()).isFalse();
        assertThat(ContentType.of("   ").isPresent()).isFalse();
        assertThat(ContentType.of(null)).isEqualTo(ContentType.none());
        assertThat(ContentType.of("text/xml").isPresent()).isTrue();
        assertThat(ContentType.of("text/xml").value()).isEqualTo("text/xml");
    }

    @Test
    @DisplayName("GIVEN a handle WHEN none vs present THEN isPresent reflects it")
    public void handleModelsAbsence() {

        assertThat(Handle.none().isPresent()).isFalse();
        assertThat(Handle.of("/opt/shibboleth-idp/metadata/tok-1").isPresent()).isTrue();
        assertThat(Handle.of("/opt/shibboleth-idp/metadata/tok-1").value())
            .isEqualTo("/opt/shibboleth-idp/metadata/tok-1");
    }
}
