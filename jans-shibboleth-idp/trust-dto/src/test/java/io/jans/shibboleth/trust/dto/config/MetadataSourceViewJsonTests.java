package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.config.metadata.manual.SamlBinding;

import org.junit.jupiter.api.Test;

/**
 * Verifies the polymorphic metadata-source read view serialises with the {@code type} discriminator
 * and snake_case fields.
 */
class MetadataSourceViewJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void noneSerialisesWithTypeOnly() throws Exception {

        JsonNode json = serialise(new NoneMetadataSourceView());

        assertThat(fieldNames(json)).containsExactly("type");
        assertThat(json.get("type").asText()).isEqualTo("NONE");
    }

    @Test
    void uriSerialisesWithType() throws Exception {

        JsonNode json = serialise(new UriMetadataSourceView("https://sp.example.org/metadata.xml"));

        assertThat(json.get("type").asText()).isEqualTo("URI");
        assertThat(json.get("uri").asText()).isEqualTo("https://sp.example.org/metadata.xml");
    }

    @Test
    void fileSerialisesFilePath() throws Exception {

        JsonNode json = serialise(new FileMetadataSourceView("upload-token-123"));

        assertThat(json.get("type").asText()).isEqualTo("FILE");
        assertThat(json.get("file_path").asText()).isEqualTo("upload-token-123");
    }

    @Test
    void manualSerialisesNestedAcsAndCertificate() throws Exception {

        ManualMetadataSourceView view = new ManualMetadataSourceView(
            "https://sp.example.org/shibboleth", "2027-01-01T00:00:00Z",
            new AssertionConsumerServiceView("https://sp.example.org/acs", SamlBinding.HTTP_POST, 1, true),
            "BASE64CERT");

        JsonNode json = serialise(view);

        assertThat(json.get("type").asText()).isEqualTo("MANUAL");
        assertThat(json.get("valid_until").asText()).isEqualTo("2027-01-01T00:00:00Z");
        assertThat(json.get("signing_certificate").asText()).isEqualTo("BASE64CERT");

        JsonNode acs = json.get("assertion_consumer_service");
        assertThat(fieldNames(acs)).containsExactlyInAnyOrder("location", "binding", "index", "is_default");
        assertThat(acs.get("binding").asText()).isEqualTo("HTTP_POST");
        assertThat(acs.get("is_default").asBoolean()).isTrue();
    }

    private JsonNode serialise(MetadataSourceView view) throws Exception {

        return mapper.readTree(mapper.writeValueAsString(view));
    }

    private static Iterable<String> fieldNames(JsonNode node) {

        return () -> node.fieldNames();
    }
}
