package io.jans.shibboleth.trust.persistence.config;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.EntityId;
import io.jans.shibboleth.trust.config.EntityIds;
import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.ReleasedAttributes;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.config.metadata.FileMetadataSource;
import io.jans.shibboleth.trust.config.metadata.ManualMetadataSource;
import io.jans.shibboleth.trust.config.metadata.MdqMetadataSource;
import io.jans.shibboleth.trust.config.metadata.MetadataSource;
import io.jans.shibboleth.trust.config.metadata.MetadataSourceType;
import io.jans.shibboleth.trust.config.metadata.NoMetadataSource;
import io.jans.shibboleth.trust.config.metadata.UpstreamMetadataSource;
import io.jans.shibboleth.trust.config.metadata.UriMetadataSource;
import io.jans.shibboleth.trust.config.metadata.manual.AssertionConsumerService;
import io.jans.shibboleth.trust.config.metadata.manual.CertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.NoCertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.SamlBinding;
import io.jans.shibboleth.trust.config.metadata.manual.SamlX509CertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.ValidityPeriod;
import io.jans.shibboleth.trust.config.profile.SamlProfileConfigurationDefaults;
import io.jans.shibboleth.trust.persistence.config.payload.MetadataSourcePayload;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.shared.Version;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;

import java.net.URI;
import java.time.Instant;
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
        entry.setMetadataSource(toPayload(trustRelationship.getMetadataSource()));

        return entry;
    }

    public static Result<TrustRelationship> toDomain(TrustRelationshipEntry entry) {

        Result<DisplayName> displayName = DisplayName.of(entry.getDisplayName());
        if (displayName.isFailure()) {

            return Result.failure(displayName.getError());
        }

        Result<MetadataSource> metadataSource = toMetadataSource(entry.getMetadataSource());
        if (metadataSource.isFailure()) {

            return Result.failure(metadataSource.getError());
        }

        Id id = entry.getId() == null ? Id.unassigned() : Id.of(UUID.fromString(entry.getId()));

        return TrustRelationship.builder()
            .withId(id)
            .withDisplayName(displayName.getValue())
            .withDescription(Description.of(entry.getDescription()))
            .withNature(TrustNature.valueOf(entry.getNature()))
            .withVersion(Version.of(entry.getVersion()))
            .withStatus(TrustStatus.valueOf(entry.getStatus()))
            .withMetadataSource(metadataSource.getValue())
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

    private static MetadataSourcePayload toPayload(MetadataSource source) {

        MetadataSourcePayload payload = new MetadataSourcePayload();
        payload.setType(source.getType().name());

        switch (source.getType()) {

            case NONE:
                break;
            case FILE:
                payload.setFilePath(((FileMetadataSource) source).getFilePath());
                break;
            case URI:
                payload.setUri(((UriMetadataSource) source).getUri().toString());
                break;
            case MDQ:
                payload.setBaseUrl(((MdqMetadataSource) source).getBaseUrl().toString());
                break;
            case UPSTREAM:
                UpstreamMetadataSource upstream = (UpstreamMetadataSource) source;
                payload.setParentId(upstream.getParentId().getValue().getValue().toString());
                payload.setEntityId(upstream.getEntityId().getValue().toString());
                break;
            case MANUAL:
                ManualMetadataSource manual = (ManualMetadataSource) source;
                payload.setEntityId(manual.getEntityId().getValue().toString());
                payload.setValidUntil(manual.getValidUntil().getValidUntil().toString());
                payload.setAcs(toAcsPayload(manual.getAssertionConsumerService()));
                payload.setSigningCert(toCertPayload(manual.getSigningCertificate()));
                break;
            default:
                throw new IllegalStateException("unhandled metadata source type: " + source.getType());
        }

        return payload;
    }

    private static MetadataSourcePayload.Acs toAcsPayload(AssertionConsumerService acs) {

        MetadataSourcePayload.Acs payload = new MetadataSourcePayload.Acs();
        payload.setLocation(acs.getLocation().toString());
        payload.setBinding(acs.getBinding().name());
        payload.setIndex(acs.getIndex());
        payload.setDefaultEndpoint(acs.isDefault());
        return payload;
    }

    private static MetadataSourcePayload.Cert toCertPayload(CertificateInfo certificate) {

        MetadataSourcePayload.Cert payload = new MetadataSourcePayload.Cert();
        if (certificate.hasCertificateData()) {

            payload.setType("X509");
            payload.setData(certificate.getCertificateData().getValue());
        } else {

            payload.setType("NONE");
        }
        return payload;
    }

    private static Result<MetadataSource> toMetadataSource(MetadataSourcePayload payload) {

        MetadataSourceType type = MetadataSourceType.valueOf(payload.getType());

        switch (type) {

            case NONE:
                return Result.success(new NoMetadataSource());
            case FILE:
                return FileMetadataSource.of(payload.getFilePath());
            case URI:
                return UriMetadataSource.of(URI.create(payload.getUri()));
            case MDQ:
                return asMetadataSource(MdqMetadataSource.of(URI.create(payload.getBaseUrl())));
            case UPSTREAM:
                return toUpstream(payload);
            case MANUAL:
                return toManual(payload);
            default:
                throw new IllegalStateException("unhandled metadata source type: " + type);
        }
    }

    private static Result<MetadataSource> asMetadataSource(Result<MdqMetadataSource> result) {

        if (result.isFailure()) {

            return Result.failure(result.getError());
        }
        return Result.success(result.getValue());
    }

    private static Result<MetadataSource> toUpstream(MetadataSourcePayload payload) {

        Result<EntityId> entityId = EntityId.of(URI.create(payload.getEntityId()));
        if (entityId.isFailure()) {

            return Result.failure(entityId.getError());
        }

        Id parentId = Id.of(UUID.fromString(payload.getParentId()));
        return UpstreamMetadataSource.of(parentId, entityId.getValue());
    }

    private static Result<MetadataSource> toManual(MetadataSourcePayload payload) {

        Result<EntityId> entityId = EntityId.of(URI.create(payload.getEntityId()));
        if (entityId.isFailure()) {

            return Result.failure(entityId.getError());
        }

        Result<ValidityPeriod> validUntil = ValidityPeriod.until(Instant.parse(payload.getValidUntil()));
        if (validUntil.isFailure()) {

            return Result.failure(validUntil.getError());
        }

        MetadataSourcePayload.Acs acsPayload = payload.getAcs();
        Result<AssertionConsumerService> acs = AssertionConsumerService.of(
            URI.create(acsPayload.getLocation()), SamlBinding.valueOf(acsPayload.getBinding()),
            acsPayload.getIndex(), acsPayload.isDefaultEndpoint());
        if (acs.isFailure()) {

            return Result.failure(acs.getError());
        }

        Result<CertificateInfo> certificate = toCertificate(payload.getSigningCert());
        if (certificate.isFailure()) {

            return Result.failure(certificate.getError());
        }

        return ManualMetadataSource.builder()
            .entityId(entityId.getValue())
            .validUntil(validUntil.getValue())
            .assertionConsumerService(acs.getValue())
            .signingCertificate(certificate.getValue())
            .build();
    }

    private static Result<CertificateInfo> toCertificate(MetadataSourcePayload.Cert certificate) {

        if ("X509".equals(certificate.getType())) {

            return SamlX509CertificateInfo.fromBase64CertificateData(certificate.getData());
        }
        return Result.success(new NoCertificateInfo());
    }
}
