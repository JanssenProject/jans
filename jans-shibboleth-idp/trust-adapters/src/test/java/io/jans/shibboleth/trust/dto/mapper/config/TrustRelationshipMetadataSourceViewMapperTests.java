package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.metadata.manual.SamlBinding;
import io.jans.shibboleth.trust.dto.config.AssertionConsumerServiceRequest;
import io.jans.shibboleth.trust.dto.config.FileMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.FileMetadataSourceView;
import io.jans.shibboleth.trust.dto.config.ManualMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.ManualMetadataSourceView;
import io.jans.shibboleth.trust.dto.config.MdqMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.MdqMetadataSourceView;
import io.jans.shibboleth.trust.dto.config.MetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.MetadataSourceView;
import io.jans.shibboleth.trust.dto.config.NoneMetadataSourceView;
import io.jans.shibboleth.trust.dto.config.UpstreamMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.UpstreamMetadataSourceView;
import io.jans.shibboleth.trust.dto.config.UriMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.UriMetadataSourceView;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrustRelationshipMetadataSourceViewMapperTests {

    private static final UUID PARENT_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void shouldViewNone() {

        MetadataSourceView view = TrustRelationshipMapper.toMetadataSourceView(individual());

        assertThat(view).isInstanceOf(NoneMetadataSourceView.class);
    }

    @Test
    void shouldViewUri() {

        TrustRelationship tr = withSource(individual(), new UriMetadataSourceRequest("https://sp.example.org/metadata.xml"));

        MetadataSourceView view = TrustRelationshipMapper.toMetadataSourceView(tr);

        assertThat(view).isInstanceOf(UriMetadataSourceView.class);
        assertThat(((UriMetadataSourceView) view).getUri()).isEqualTo("https://sp.example.org/metadata.xml");
    }

    @Test
    void shouldViewUpstream() {

        TrustRelationship tr = withSource(individual(),
            new UpstreamMetadataSourceRequest(PARENT_ID.toString(), "https://sp.example.org/shibboleth"));

        MetadataSourceView view = TrustRelationshipMapper.toMetadataSourceView(tr);

        assertThat(view).isInstanceOf(UpstreamMetadataSourceView.class);
        UpstreamMetadataSourceView upstream = (UpstreamMetadataSourceView) view;
        assertThat(upstream.getParentId()).isEqualTo(PARENT_ID.toString());
        assertThat(upstream.getEntityId()).isEqualTo("https://sp.example.org/shibboleth");
    }

    @Test
    void shouldViewMdq() {

        TrustRelationship tr = withSource(aggregate(), new MdqMetadataSourceRequest("https://mdq.example.org"));

        MetadataSourceView view = TrustRelationshipMapper.toMetadataSourceView(tr);

        assertThat(view).isInstanceOf(MdqMetadataSourceView.class);
        assertThat(((MdqMetadataSourceView) view).getBaseUrl()).isEqualTo("https://mdq.example.org");
    }

    @Test
    void shouldViewFileExposingStoredReference() {

        TrustRelationship tr = withSource(individual(), new FileMetadataSourceRequest("upload-token-123"));

        MetadataSourceView view = TrustRelationshipMapper.toMetadataSourceView(tr);

        assertThat(view).isInstanceOf(FileMetadataSourceView.class);
        assertThat(((FileMetadataSourceView) view).getFilePath()).isEqualTo("upload-token-123");
    }

    @Test
    void shouldViewManualWithCertificate() {

        TrustRelationship tr = withSource(individual(), manualRequest("BASE64CERT"));

        MetadataSourceView view = TrustRelationshipMapper.toMetadataSourceView(tr);

        assertThat(view).isInstanceOf(ManualMetadataSourceView.class);
        ManualMetadataSourceView manual = (ManualMetadataSourceView) view;
        assertThat(manual.getEntityId()).isEqualTo("https://sp.example.org/shibboleth");
        assertThat(manual.getValidUntil()).isEqualTo("2027-01-01T00:00:00Z");
        assertThat(manual.getSigningCertificate()).isEqualTo("BASE64CERT");
        assertThat(manual.getAssertionConsumerService().getLocation()).isEqualTo("https://sp.example.org/acs");
        assertThat(manual.getAssertionConsumerService().getBinding()).isEqualTo(SamlBinding.HTTP_POST);
        assertThat(manual.getAssertionConsumerService().getIndex()).isEqualTo(1);
        assertThat(manual.getAssertionConsumerService().getIsDefault()).isTrue();
    }

    @Test
    void shouldViewManualWithoutCertificate() {

        TrustRelationship tr = withSource(individual(), manualRequest(null));

        ManualMetadataSourceView view = (ManualMetadataSourceView) TrustRelationshipMapper.toMetadataSourceView(tr);

        assertThat(view.getSigningCertificate()).isNull();
    }

    private static TrustRelationship withSource(TrustRelationship base, MetadataSourceRequest request) {

        return TrustRelationshipMapper.updateMetadataSource(base, request).getValue();
    }

    private static ManualMetadataSourceRequest manualRequest(String certificate) {

        ManualMetadataSourceRequest request = new ManualMetadataSourceRequest();
        request.setEntityId("https://sp.example.org/shibboleth");
        request.setValidUntil("2027-01-01T00:00:00Z");
        request.setSigningCertificate(certificate);

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
