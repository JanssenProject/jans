package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.EntityId;
import io.jans.shibboleth.trust.config.EntityIds;
import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.ReleasedAttribute;
import io.jans.shibboleth.trust.config.ReleasedAttributes;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.config.metadata.MetadataSourceType;
import io.jans.shibboleth.trust.config.profile.Saml2SsoProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.SamlProfileConfigurationDefaults;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;
import io.jans.shibboleth.trust.dto.config.ActivationDiagnosticsDto;
import io.jans.shibboleth.trust.dto.config.ProfileSummary;
import io.jans.shibboleth.trust.dto.config.TrustRelationshipDetail;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationLogEntry;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationStatus;
import io.jans.shibboleth.trust.shared.diagnostics.LogLevel;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrustRelationshipDetailMapperTests {

    private static final UUID SOME_ID = UUID.fromString("7f3a9c2e-4b1d-4c8a-9e2f-1a2b3c4d5e6f");

    @Test
    void shouldProjectDefaultDraftToDetail() {

        TrustRelationship tr = draft().build().getValue();

        TrustRelationshipDetail detail = TrustRelationshipMapper.toDetail(tr);

        assertThat(detail.getId()).isEqualTo(SOME_ID);
        assertThat(detail.getDisplayName()).isEqualTo("Portal SP");
        assertThat(detail.getNature()).isEqualTo(TrustNature.INDIVIDUAL);
        assertThat(detail.getStatus()).isEqualTo(TrustStatus.DRAFT);
        assertThat(detail.getVersion()).isEqualTo(1);

        assertThat(detail.getMetadataSource().getType()).isEqualTo(MetadataSourceType.NONE);
        assertThat(detail.getReleasedAttributes()).isEmpty();
        assertThat(detail.getDiscoveredEntityIds()).isEmpty();

        assertThat(detail.getProfiles())
            .extracting(ProfileSummary::getType)
            .containsExactlyInAnyOrder(ProfileType.values());
        assertThat(detail.getProfiles())
            .allSatisfy(p -> assertThat(p.getStatus()).isEqualTo(ProfileStatus.INACTIVE));

        ActivationDiagnosticsDto diagnostics = detail.getActivationDiagnostics();
        assertThat(diagnostics.getStatus()).isEqualTo(ActivationStatus.NO_DATA);
        assertThat(diagnostics.getOrigin()).isEmpty();
        assertThat(diagnostics.getStartedAt()).isEqualTo("1970-01-01T00:00:00Z");
        assertThat(diagnostics.getCompletedAt()).isEqualTo("1970-01-01T00:00:00Z");
        assertThat(diagnostics.getLogEntries()).isEmpty();
    }

    @Test
    void shouldListDiscoveredEntityIds() {

        EntityIds discovered = EntityIds.builder()
            .add(EntityId.of(URI.create("https://sp.example.org/shibboleth")).getValue())
            .build().getValue();

        TrustRelationship tr = draft().withDiscoveredEntityIds(discovered).build().getValue();

        TrustRelationshipDetail detail = TrustRelationshipMapper.toDetail(tr);

        assertThat(detail.getDiscoveredEntityIds())
            .containsExactly("https://sp.example.org/shibboleth");
    }

    @Test
    void shouldListReleasedAttributes() {

        UUID attributeId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        ReleasedAttributes attributes = ReleasedAttributes.builder()
            .add(ReleasedAttribute.of(Id.of(attributeId), "givenName").getValue())
            .build().getValue();

        TrustRelationship tr = draft().withReleasedAttributes(attributes).build().getValue();

        TrustRelationshipDetail detail = TrustRelationshipMapper.toDetail(tr);

        assertThat(detail.getReleasedAttributes()).hasSize(1);
        assertThat(detail.getReleasedAttributes().get(0).getId()).isEqualTo(attributeId);
        assertThat(detail.getReleasedAttributes().get(0).getDisplayName()).isEqualTo("givenName");
    }

    @Test
    void shouldReflectActiveProfileStatus() {

        Saml2SsoProfileConfiguration activeSso = Saml2SsoProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2Sso())
            .status(ProfileStatus.ACTIVE)
            .build().getValue();

        TrustRelationship tr = draft().withSaml2SsoProfileConfiguration(activeSso).build().getValue();

        TrustRelationshipDetail detail = TrustRelationshipMapper.toDetail(tr);

        assertThat(detail.getProfiles())
            .contains(new ProfileSummary(ProfileType.SAML2_SSO, ProfileStatus.ACTIVE));
    }

    @Test
    void shouldMapActivationDiagnosticsWithData() {

        Instant startedAt = Instant.parse("2026-07-19T10:15:00Z");
        Instant completedAt = Instant.parse("2026-07-19T10:15:30Z");
        ActivationLogEntry entry = ActivationLogEntry
            .of(Instant.parse("2026-07-19T10:15:10Z"), LogLevel.INFO, "metadata processed")
            .getValue();
        ActivationDiagnostics diagnostics = ActivationDiagnostics
            .of(ActivationStatus.SUCCEEDED, Origin.of("worker-1@host"), List.of(entry), startedAt, completedAt)
            .getValue();

        TrustRelationship tr = draft().withActivationDiagnostics(diagnostics).build().getValue();

        ActivationDiagnosticsDto dto = TrustRelationshipMapper.toDetail(tr).getActivationDiagnostics();

        assertThat(dto.getStatus()).isEqualTo(ActivationStatus.SUCCEEDED);
        assertThat(dto.getOrigin()).isEqualTo("worker-1@host");
        assertThat(dto.getStartedAt()).isEqualTo("2026-07-19T10:15:00Z");
        assertThat(dto.getCompletedAt()).isEqualTo("2026-07-19T10:15:30Z");
        assertThat(dto.getLogEntries()).hasSize(1);
        assertThat(dto.getLogEntries().get(0).getTimestamp()).isEqualTo("2026-07-19T10:15:10Z");
        assertThat(dto.getLogEntries().get(0).getLevel()).isEqualTo(LogLevel.INFO);
        assertThat(dto.getLogEntries().get(0).getMessage()).isEqualTo("metadata processed");
    }

    @Test
    void shouldRejectUnassignedIdAtTheDtoBoundary() {

        TrustRelationship unassigned = TrustRelationship
            .create(DisplayName.of("Portal SP").getValue(), Description.of("desc"), TrustNature.INDIVIDUAL)
            .getValue();

        assertThatThrownBy(() -> TrustRelationshipMapper.toDetail(unassigned))
            .isInstanceOf(IllegalStateException.class);
    }

    /**
     * A persisted-looking DRAFT (assigned id) that individual tests further tailor before build().
     */
    private static TrustRelationship.Builder draft() {

        TrustRelationship created = TrustRelationship
            .create(DisplayName.of("Portal SP").getValue(), Description.of("desc"), TrustNature.INDIVIDUAL)
            .getValue();

        return TrustRelationship.from(created).withId(Id.of(SOME_ID));
    }
}
