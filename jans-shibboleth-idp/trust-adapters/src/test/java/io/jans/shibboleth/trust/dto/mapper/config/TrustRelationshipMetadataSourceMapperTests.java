package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.error.DomainObjectUpdateFailed;
import io.jans.shibboleth.trust.config.error.InvalidTimestampSyntax;
import io.jans.shibboleth.trust.config.error.InvalidUriSyntax;
import io.jans.shibboleth.trust.config.error.InvalidUuidSyntax;
import io.jans.shibboleth.trust.config.metadata.FileMetadataSource;
import io.jans.shibboleth.trust.config.metadata.ManualMetadataSource;
import io.jans.shibboleth.trust.config.metadata.MdqMetadataSource;
import io.jans.shibboleth.trust.config.metadata.MetadataSource;
import io.jans.shibboleth.trust.config.metadata.MetadataSourceType;
import io.jans.shibboleth.trust.config.metadata.UpstreamMetadataSource;
import io.jans.shibboleth.trust.config.metadata.UriMetadataSource;
import io.jans.shibboleth.trust.config.metadata.manual.SamlBinding;
import io.jans.shibboleth.trust.dto.config.AssertionConsumerServiceRequest;
import io.jans.shibboleth.trust.dto.config.FileMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.ManualMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.MdqMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.NoneMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.UpstreamMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.UriMetadataSourceRequest;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrustRelationshipMetadataSourceMapperTests {

    private static final String SP_METADATA = "https://sp.example.org/metadata.xml";
    private static final String ENTITY_ID = "https://sp.example.org/shibboleth";
    private static final String MDQ_BASE = "https://mdq.example.org";
    private static final UUID PARENT_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void shouldSetNoneMetadataSource() {

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateMetadataSource(individual(), new NoneMetadataSourceRequest());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getMetadataSource().getType()).isEqualTo(MetadataSourceType.NONE);
    }

    @Test
    void shouldSetUriMetadataSource() {

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateMetadataSource(individual(), new UriMetadataSourceRequest(SP_METADATA));

        assertThat(result.isSuccess()).isTrue();
        MetadataSource source = result.getValue().getMetadataSource();
        assertThat(source.getType()).isEqualTo(MetadataSourceType.URI);
        assertThat(((UriMetadataSource) source).getUri()).isEqualTo(URI.create(SP_METADATA));
    }

    @Test
    void shouldSetUpstreamMetadataSourceForIndividual() {

        Result<TrustRelationship> result = TrustRelationshipMapper.updateMetadataSource(
            individual(), new UpstreamMetadataSourceRequest(PARENT_ID.toString(), ENTITY_ID));

        assertThat(result.isSuccess()).isTrue();
        MetadataSource source = result.getValue().getMetadataSource();
        assertThat(source.getType()).isEqualTo(MetadataSourceType.UPSTREAM);
        UpstreamMetadataSource upstream = (UpstreamMetadataSource) source;
        assertThat(upstream.getParentId().getValue().getValue()).isEqualTo(PARENT_ID);
        assertThat(upstream.getEntityId().getValue()).isEqualTo(URI.create(ENTITY_ID));
    }

    @Test
    void shouldSetMdqMetadataSourceForAggregate() {

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateMetadataSource(aggregate(), new MdqMetadataSourceRequest(MDQ_BASE));

        assertThat(result.isSuccess()).isTrue();
        MetadataSource source = result.getValue().getMetadataSource();
        assertThat(source.getType()).isEqualTo(MetadataSourceType.MDQ);
        assertThat(((MdqMetadataSource) source).getBaseUrl()).isEqualTo(URI.create(MDQ_BASE));
    }

    @Test
    void shouldFailWhenMdqUsedForIndividual() {

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateMetadataSource(individual(), new MdqMetadataSourceRequest(MDQ_BASE));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
    }

    @Test
    void shouldFailWhenUpstreamUsedForAggregate() {

        Result<TrustRelationship> result = TrustRelationshipMapper.updateMetadataSource(
            aggregate(), new UpstreamMetadataSourceRequest(PARENT_ID.toString(), ENTITY_ID));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
    }

    @Test
    void shouldFailWhenUriIsMalformed() {

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateMetadataSource(individual(), new UriMetadataSourceRequest("not a valid uri"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(InvalidUriSyntax.class);
    }

    @Test
    void shouldFailWhenUriIsMissing() {

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateMetadataSource(individual(), new UriMetadataSourceRequest(null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    void shouldFailWhenParentIdIsMalformed() {

        Result<TrustRelationship> result = TrustRelationshipMapper.updateMetadataSource(
            individual(), new UpstreamMetadataSourceRequest("not-a-uuid", ENTITY_ID));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(InvalidUuidSyntax.class);
    }

    @Test
    void shouldSetFileMetadataSourceFromToken() {

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateMetadataSource(individual(), new FileMetadataSourceRequest("upload-abc123"));

        assertThat(result.isSuccess()).isTrue();
        MetadataSource source = result.getValue().getMetadataSource();
        assertThat(source.getType()).isEqualTo(MetadataSourceType.FILE);
        assertThat(((FileMetadataSource) source).getFilePath()).isEqualTo("upload-abc123");
    }

    @Test
    void shouldFailWhenFileTokenIsMissing() {

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateMetadataSource(individual(), new FileMetadataSourceRequest(null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    void shouldSetManualMetadataSourceForIndividual() {

        Result<TrustRelationship> result = TrustRelationshipMapper.updateMetadataSource(individual(), manualRequest());

        assertThat(result.isSuccess()).isTrue();
        MetadataSource source = result.getValue().getMetadataSource();
        assertThat(source.getType()).isEqualTo(MetadataSourceType.MANUAL);

        ManualMetadataSource manual = (ManualMetadataSource) source;
        assertThat(manual.getEntityId().getValue()).isEqualTo(URI.create(ENTITY_ID));
        assertThat(manual.getValidUntil().getValidUntil()).isEqualTo(Instant.parse("2027-01-01T00:00:00Z"));
        assertThat(manual.getAssertionConsumerService().getLocation()).isEqualTo(URI.create("https://sp.example.org/acs"));
        assertThat(manual.getAssertionConsumerService().getBinding()).isEqualTo(SamlBinding.HTTP_POST);
        assertThat(manual.getSigningCertificate().hasCertificateData()).isTrue();
        assertThat(manual.getSigningCertificate().getCertificateData().getValue()).isEqualTo("BASE64CERT");
    }

    @Test
    void shouldApplyAssertionConsumerServiceDefaults() {

        MetadataSource source =
            TrustRelationshipMapper.updateMetadataSource(individual(), manualRequest()).getValue().getMetadataSource();

        assertThat(((ManualMetadataSource) source).getAssertionConsumerService().getIndex()).isEqualTo(1);
        assertThat(((ManualMetadataSource) source).getAssertionConsumerService().isDefault()).isTrue();
    }

    @Test
    void shouldSetManualMetadataSourceWithNoCertificate() {

        ManualMetadataSourceRequest request = manualRequest();
        request.setSigningCertificate(null);

        Result<TrustRelationship> result = TrustRelationshipMapper.updateMetadataSource(individual(), request);

        assertThat(result.isSuccess()).isTrue();
        ManualMetadataSource manual = (ManualMetadataSource) result.getValue().getMetadataSource();
        assertThat(manual.getSigningCertificate().hasCertificateData()).isFalse();
    }

    @Test
    void shouldFailWhenManualValidUntilIsMalformed() {

        ManualMetadataSourceRequest request = manualRequest();
        request.setValidUntil("not-a-timestamp");

        Result<TrustRelationship> result = TrustRelationshipMapper.updateMetadataSource(individual(), request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(InvalidTimestampSyntax.class);
    }

    @Test
    void shouldFailWhenManualEntityIdIsMissing() {

        ManualMetadataSourceRequest request = manualRequest();
        request.setEntityId(null);

        Result<TrustRelationship> result = TrustRelationshipMapper.updateMetadataSource(individual(), request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    void shouldFailWhenManualUsedForAggregate() {

        Result<TrustRelationship> result = TrustRelationshipMapper.updateMetadataSource(aggregate(), manualRequest());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
    }

    private static ManualMetadataSourceRequest manualRequest() {

        ManualMetadataSourceRequest request = new ManualMetadataSourceRequest();
        request.setEntityId(ENTITY_ID);
        request.setValidUntil("2027-01-01T00:00:00Z");
        request.setSigningCertificate("BASE64CERT");

        AssertionConsumerServiceRequest acs = new AssertionConsumerServiceRequest();
        acs.setLocation("https://sp.example.org/acs");
        acs.setBinding(SamlBinding.HTTP_POST);
        request.setAssertionConsumerService(acs);

        return request;
    }

    private static TrustRelationship individual() {

        return TrustRelationship
            .create(DisplayName.of("Portal SP").getValue(), Description.of("d"), TrustNature.INDIVIDUAL)
            .getValue();
    }

    private static TrustRelationship aggregate() {

        return TrustRelationship
            .create(DisplayName.of("Federation").getValue(), Description.of("d"), TrustNature.AGGREGATE)
            .getValue();
    }
}
