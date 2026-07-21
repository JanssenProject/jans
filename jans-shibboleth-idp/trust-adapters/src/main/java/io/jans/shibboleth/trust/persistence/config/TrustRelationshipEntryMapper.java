package io.jans.shibboleth.trust.persistence.config;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
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

/**
 * Translates between the {@link TrustRelationship} aggregate and its {@link TrustRelationshipEntry}.
 *
 * <p>Rehydration ({@link #toDomain}) rebuilds a validated aggregate via the domain's no-bump
 * {@code builder()} path. This slice round-trips the flat columns and default-seeds the rich
 * sub-structure (metadata {@code NONE}, default profiles, empty released attributes, no diagnostics);
 * payload serialization for customized sub-structure is added as the round-trip matrix widens.
 */
public final class TrustRelationshipEntryMapper {

    private TrustRelationshipEntryMapper() {
    }

    public static TrustRelationshipEntry toEntry(TrustRelationship trustRelationship) {

        TrustRelationshipEntry entry = new TrustRelationshipEntry();

        Id id = trustRelationship.getId();
        entry.setId(id.isAssigned() ? id.getValue().getValue().toString() : null);
        entry.setDisplayName(trustRelationship.getDisplayName().getValue());
        entry.setDescription(trustRelationship.getDescription().getValue());
        entry.setNature(trustRelationship.getNature().name());
        entry.setStatus(trustRelationship.getStatus().name());
        entry.setVersion(trustRelationship.getVersion().getValue());

        return entry;
    }

    public static Result<TrustRelationship> toDomain(TrustRelationshipEntry entry) {

        Result<DisplayName> displayName = DisplayName.of(entry.getDisplayName());
        if (displayName.isFailure()) {

            return Result.failure(displayName.getError());
        }

        Id id = entry.getId() == null ? Id.unassigned() : Id.of(UUID.fromString(entry.getId()));

        return TrustRelationship.builder()
            .withId(id)
            .withDisplayName(displayName.getValue())
            .withDescription(Description.of(entry.getDescription()))
            .withNature(TrustNature.valueOf(entry.getNature()))
            .withVersion(Version.of(entry.getVersion()))
            .withStatus(TrustStatus.valueOf(entry.getStatus()))
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
            .build();
    }
}
