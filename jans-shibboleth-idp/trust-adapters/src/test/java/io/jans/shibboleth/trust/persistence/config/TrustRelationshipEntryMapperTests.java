package io.jans.shibboleth.trust.persistence.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.EntityIds;
import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.ReleasedAttributes;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.config.metadata.NoMetadataSource;
import io.jans.shibboleth.trust.config.profile.SamlProfileConfigurationDefaults;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.shared.Version;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Minimal round-trip for the aggregate ⇄ entry mapper: a default-shaped trust relationship survives
 * {@code domain → entry → domain} unchanged. Proves the harness and the flat-column fidelity (id,
 * display name, description, nature, status, version). The rich sub-structure is default-seeded on read
 * in this slice; payload round-tripping is added as the matrix widens.
 */
@DisplayName("TrustRelationshipEntryMapper — minimal round-trip")
public class TrustRelationshipEntryMapperTests {

    @Test
    @DisplayName("GIVEN a default persisted trust relationship WHEN mapped to entry and back THEN it is unchanged")
    public void defaultAggregateRoundTrips() {

        TrustRelationship original = persisted(UUID.randomUUID(), "Acme SP", "", TrustNature.AGGREGATE, 1);

        Result<TrustRelationship> roundTripped =
            TrustRelationshipEntryMapper.toDomain(TrustRelationshipEntryMapper.toEntry(original));

        assertThat(roundTripped.isSuccess()).isTrue();
        assertThat(roundTripped.getValue()).isEqualTo(original);
    }

    @Test
    @DisplayName("GIVEN an unassigned-id trust relationship WHEN round-tripped THEN the id stays unassigned")
    public void unassignedIdRoundTrips() {

        TrustRelationship original = TrustRelationship.create(
            io.jans.shibboleth.trust.config.DisplayName.of("Draft SP").getValue(),
            Description.of("in progress"), TrustNature.INDIVIDUAL).getValue();

        Result<TrustRelationship> roundTripped =
            TrustRelationshipEntryMapper.toDomain(TrustRelationshipEntryMapper.toEntry(original));

        assertThat(roundTripped.isSuccess()).isTrue();
        assertThat(roundTripped.getValue().getId().isAssigned()).isFalse();
        assertThat(roundTripped.getValue()).isEqualTo(original);
    }

    @Test
    @DisplayName("GIVEN varied flat fields WHEN round-tripped THEN every flat column is carried, not hard-coded")
    public void flatColumnsAreCarried() {

        TrustRelationship original =
            persisted(UUID.randomUUID(), "Payments SP", "Handles payment flows", TrustNature.INDIVIDUAL, 7);

        Result<TrustRelationship> roundTripped =
            TrustRelationshipEntryMapper.toDomain(TrustRelationshipEntryMapper.toEntry(original));

        assertThat(roundTripped.isSuccess()).isTrue();
        assertThat(roundTripped.getValue()).isEqualTo(original);
    }

    /** A persisted (assigned-id) trust relationship with default sub-structure, built via the no-bump path. */
    private static TrustRelationship persisted(UUID id, String displayName, String description,
        TrustNature nature, int version) {

        return TrustRelationship.builder()
            .withId(Id.of(id))
            .withDisplayName(io.jans.shibboleth.trust.config.DisplayName.of(displayName).getValue())
            .withDescription(Description.of(description))
            .withNature(nature)
            .withVersion(Version.of(version))
            .withStatus(TrustStatus.DRAFT)
            .withMetadataSource(new NoMetadataSource())
            .withDiscoveredEntityIds(EntityIds.empty())
            .withShibbolethSsoProfileConfiguration(SamlProfileConfigurationDefaults.shibbolethSso())
            .withSaml2ArtifactResolutionProfileConfiguration(SamlProfileConfigurationDefaults.saml2ArtifactResolution())
            .withSaml2AttributeQueryProfileConfiguration(SamlProfileConfigurationDefaults.saml2AttributeQuery())
            .withSaml2EcpProfileConfiguration(SamlProfileConfigurationDefaults.saml2Ecp())
            .withSaml2SsoProfileConfiguration(SamlProfileConfigurationDefaults.saml2Sso())
            .withSaml2LogoutProfileConfiguration(SamlProfileConfigurationDefaults.saml2Logout())
            .withReleasedAttributes(ReleasedAttributes.empty())
            .withActivationDiagnostics(ActivationDiagnostics.none())
            .build()
            .getValue();
    }
}
