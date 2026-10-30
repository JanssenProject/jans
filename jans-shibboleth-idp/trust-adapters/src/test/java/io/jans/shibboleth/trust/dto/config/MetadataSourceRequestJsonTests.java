package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

/**
 * Verifies polymorphic deserialization of the metadata-source request: the {@code type}
 * discriminator selects the concrete shape, and the not-yet-supported types are rejected.
 */
class MetadataSourceRequestJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialisesNone() throws Exception {

        MetadataSourceRequest request = mapper.readValue("{\"type\":\"NONE\"}", MetadataSourceRequest.class);

        assertThat(request).isInstanceOf(NoneMetadataSourceRequest.class);
    }

    @Test
    void deserialisesUri() throws Exception {

        String body = "{\"type\":\"URI\",\"uri\":\"https://sp.example.org/metadata.xml\"}";

        MetadataSourceRequest request = mapper.readValue(body, MetadataSourceRequest.class);

        assertThat(request).isInstanceOf(UriMetadataSourceRequest.class);
        assertThat(((UriMetadataSourceRequest) request).getUri()).isEqualTo("https://sp.example.org/metadata.xml");
    }

    @Test
    void deserialisesUpstream() throws Exception {

        String body = "{\"type\":\"UPSTREAM\",\"parent_id\":\"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee\","
            + "\"entity_id\":\"https://sp.example.org/shibboleth\"}";

        MetadataSourceRequest request = mapper.readValue(body, MetadataSourceRequest.class);

        assertThat(request).isInstanceOf(UpstreamMetadataSourceRequest.class);
        UpstreamMetadataSourceRequest upstream = (UpstreamMetadataSourceRequest) request;
        assertThat(upstream.getParentId()).isEqualTo("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        assertThat(upstream.getEntityId()).isEqualTo("https://sp.example.org/shibboleth");
    }

    @Test
    void deserialisesMdq() throws Exception {

        String body = "{\"type\":\"MDQ\",\"base_url\":\"https://mdq.example.org\"}";

        MetadataSourceRequest request = mapper.readValue(body, MetadataSourceRequest.class);

        assertThat(request).isInstanceOf(MdqMetadataSourceRequest.class);
        assertThat(((MdqMetadataSourceRequest) request).getBaseUrl()).isEqualTo("https://mdq.example.org");
    }

    @Test
    void deserialisesFile() throws Exception {

        MetadataSourceRequest request =
            mapper.readValue("{\"type\":\"FILE\",\"token\":\"upload-abc123\"}", MetadataSourceRequest.class);

        assertThat(request).isInstanceOf(FileMetadataSourceRequest.class);
        assertThat(((FileMetadataSourceRequest) request).getToken()).isEqualTo("upload-abc123");
    }

    @Test
    void deserialisesManual() throws Exception {

        String body = "{\"type\":\"MANUAL\",\"entity_id\":\"https://sp.example.org/shibboleth\","
            + "\"valid_until\":\"2027-01-01T00:00:00Z\","
            + "\"assertion_consumer_service\":{\"location\":\"https://sp.example.org/acs\",\"binding\":\"HTTP_POST\"},"
            + "\"signing_certificate\":\"BASE64CERT\"}";

        MetadataSourceRequest request = mapper.readValue(body, MetadataSourceRequest.class);

        assertThat(request).isInstanceOf(ManualMetadataSourceRequest.class);
        ManualMetadataSourceRequest manual = (ManualMetadataSourceRequest) request;
        assertThat(manual.getEntityId()).isEqualTo("https://sp.example.org/shibboleth");
        assertThat(manual.getValidUntil()).isEqualTo("2027-01-01T00:00:00Z");
        assertThat(manual.getSigningCertificate()).isEqualTo("BASE64CERT");
        assertThat(manual.getAssertionConsumerService().getBinding())
            .isEqualTo(io.jans.shibboleth.trust.config.metadata.manual.SamlBinding.HTTP_POST);
    }

    @Test
    void rejectsUnknownType() {

        String body = "{\"type\":\"BOGUS\"}";

        assertThatThrownBy(() -> mapper.readValue(body, MetadataSourceRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.InvalidTypeIdException.class);
    }

    @Test
    void rejectsUnknownField() {

        String body = "{\"type\":\"URI\",\"uri\":\"https://sp.example.org/metadata.xml\",\"bogus\":\"x\"}";

        assertThatThrownBy(() -> mapper.readValue(body, MetadataSourceRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }
}
