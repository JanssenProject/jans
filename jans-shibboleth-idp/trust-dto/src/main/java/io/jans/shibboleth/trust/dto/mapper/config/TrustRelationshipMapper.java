package io.jans.shibboleth.trust.dto.mapper.config;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.EntityId;
import io.jans.shibboleth.trust.config.EntityIds;
import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.ReleasedAttribute;
import io.jans.shibboleth.trust.config.ReleasedAttributes;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.error.InvalidTimestampSyntax;
import io.jans.shibboleth.trust.config.error.InvalidUriSyntax;
import io.jans.shibboleth.trust.config.error.InvalidUuidSyntax;
import io.jans.shibboleth.trust.config.metadata.FileMetadataSource;
import io.jans.shibboleth.trust.config.metadata.ManualMetadataSource;
import io.jans.shibboleth.trust.config.metadata.MdqMetadataSource;
import io.jans.shibboleth.trust.config.metadata.MetadataSource;
import io.jans.shibboleth.trust.config.metadata.NoMetadataSource;
import io.jans.shibboleth.trust.config.metadata.UpstreamMetadataSource;
import io.jans.shibboleth.trust.config.metadata.UriMetadataSource;
import io.jans.shibboleth.trust.config.metadata.manual.AssertionConsumerService;
import io.jans.shibboleth.trust.config.metadata.manual.CertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.NoCertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.SamlX509CertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.ValidityPeriod;
import io.jans.shibboleth.trust.config.profile.Saml2ArtifactResolutionProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.Saml2LogoutProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.common.InterceptorFlows;
import io.jans.shibboleth.trust.dto.config.ActivationDiagnosticsDto;
import io.jans.shibboleth.trust.dto.config.ActivationLogEntryDto;
import io.jans.shibboleth.trust.dto.config.AssertionConsumerServiceRequest;
import io.jans.shibboleth.trust.dto.config.CreateTrustRelationshipRequest;
import io.jans.shibboleth.trust.dto.config.FileMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.ManualMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.MdqMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.Saml2ArtifactResolutionProfileConfigurationRequest;
import io.jans.shibboleth.trust.dto.config.Saml2LogoutProfileConfigurationRequest;
import io.jans.shibboleth.trust.dto.config.MetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.MetadataSourceSummary;
import io.jans.shibboleth.trust.dto.config.NoneMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.ProfileSummary;
import io.jans.shibboleth.trust.dto.config.ReleasedAttributeDto;
import io.jans.shibboleth.trust.dto.config.TrustRelationshipDetail;
import io.jans.shibboleth.trust.dto.config.TrustRelationshipPage;
import io.jans.shibboleth.trust.dto.config.TrustRelationshipSummary;
import io.jans.shibboleth.trust.dto.config.UpdateBasicInfoRequest;
import io.jans.shibboleth.trust.dto.config.UpstreamMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.UriMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.shared.PageMetadata;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationLogEntry;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Translates between {@link TrustRelationship} and its config DTOs.
 *
 * <p>Mappers own translation only — they never carry domain logic. Domain construction returns a
 * {@link Result}, so {@code toDomain} surfaces that {@code Result} rather than throwing on a
 * domain-rule failure. Mapping <em>out</em> of the domain assumes a persisted aggregate; a
 * missing invariant of persistence (such as an unassigned id) is a programming error and is
 * raised as such rather than modelled as a recoverable failure.
 */
public final class TrustRelationshipMapper {

    private TrustRelationshipMapper() {
    }

    /**
     * Builds a new {@link TrustRelationship} from a create request. Returns the domain
     * {@link Result}: a failure carries the domain error (e.g. blank display name, missing nature).
     */
    public static Result<TrustRelationship> toDomain(CreateTrustRelationshipRequest request) {

        Result<DisplayName> displayName = DisplayName.of(request.getDisplayName());
        if (displayName.isFailure()) {

            return Result.failure(displayName.getError());
        }

        Description description = Description.of(request.getDescription());

        return TrustRelationship.create(displayName.getValue(), description, request.getNature());
    }

    /**
     * Projects a {@link TrustRelationship} onto its summary representation.
     *
     * @throws IllegalStateException if the trust relationship has no assigned id. An id is
     *     unassigned only inside the domain at construction; persistence assigns it, so any
     *     aggregate reaching this mapper is expected to already carry one.
     */
    public static TrustRelationshipSummary toSummary(TrustRelationship trustRelationship) {

        Result<UUID> id = trustRelationship.getId().getValue();
        if (id.isFailure()) {

            throw new IllegalStateException(
                "TrustRelationship reached the mapper without an assigned id; it must be persisted "
                    + "(which assigns the id) before being mapped to a DTO");
        }

        TrustRelationshipSummary summary = new TrustRelationshipSummary();
        summary.setId(id.getValue());
        summary.setDisplayName(trustRelationship.getDisplayName().getValue());
        summary.setDescription(trustRelationship.getDescription().getValue());
        summary.setNature(trustRelationship.getNature());
        summary.setStatus(trustRelationship.getStatus());
        summary.setVersion(trustRelationship.getVersion().getValue());
        return summary;
    }

    /**
     * Applies a basic-info update (display name + description) to an existing trust relationship,
     * returning the domain {@link Result}. A blank display name surfaces as a failure; a null
     * description is normalised to empty by the domain.
     */
    public static Result<TrustRelationship> updateBasicInfo(TrustRelationship existing, UpdateBasicInfoRequest request) {

        Result<DisplayName> displayName = DisplayName.of(request.getDisplayName());
        if (displayName.isFailure()) {

            return Result.failure(displayName.getError());
        }

        Description description = Description.of(request.getDescription());

        return existing.updateBasicInfo(displayName.getValue(), description);
    }

    /**
     * Applies a metadata-source update to an existing trust relationship. The request is translated
     * into a domain {@link MetadataSource}; nature and state restrictions are enforced by the domain
     * and surface as failures. FILE and MANUAL are not handled here — they await the out-of-band
     * file/certificate mechanism.
     */
    public static Result<TrustRelationship> updateMetadataSource(TrustRelationship existing, MetadataSourceRequest request) {

        Result<MetadataSource> source = toMetadataSource(request);
        if (source.isFailure()) {

            return Result.failure(source.getError());
        }

        return existing.updateMetadataSource(source.getValue());
    }

    /**
     * Applies a partial update to an existing trust relationship's SAML2 Logout profile: the builder
     * is seeded from the current profile and only the fields present in the request are overridden.
     * Nature and state restrictions are enforced by the domain.
     */
    public static Result<TrustRelationship> updateSaml2LogoutProfileConfiguration(
        TrustRelationship existing, Saml2LogoutProfileConfigurationRequest request) {

        Saml2LogoutProfileConfiguration.Builder builder =
            Saml2LogoutProfileConfiguration.from(existing.getSaml2LogoutProfileConfiguration());

        if (request.getStatus() != null) {

            builder.status(request.getStatus());
        }
        if (request.getInboundFlows() != null) {

            builder.inboundFlows(InterceptorFlows.of(request.getInboundFlows()));
        }
        if (request.getOutboundFlows() != null) {

            builder.outboundFlows(InterceptorFlows.of(request.getOutboundFlows()));
        }
        if (request.getMessageSigningPolicy() != null) {

            builder.messageSigningPolicy(request.getMessageSigningPolicy());
        }
        if (request.getRequestSignatureValidationPolicy() != null) {

            builder.requestSignatureValidationPolicy(request.getRequestSignatureValidationPolicy());
        }
        if (request.getEncryptionFallbackPolicy() != null) {

            builder.encryptionFallbackPolicy(request.getEncryptionFallbackPolicy());
        }
        if (request.getNameIdEncryptionPolicy() != null) {

            builder.nameIdEncryptionPolicy(request.getNameIdEncryptionPolicy());
        }

        Result<Saml2LogoutProfileConfiguration> built = builder.build();
        if (built.isFailure()) {

            return Result.failure(built.getError());
        }

        return existing.updateSaml2LogoutProfileConfiguration(built.getValue());
    }

    /**
     * Applies a partial update to an existing trust relationship's SAML2 Artifact Resolution profile:
     * the builder is seeded from the current profile and only the fields present in the request are
     * overridden. Nature and state restrictions are enforced by the domain.
     */
    public static Result<TrustRelationship> updateSaml2ArtifactResolutionProfileConfiguration(
        TrustRelationship existing, Saml2ArtifactResolutionProfileConfigurationRequest request) {

        Saml2ArtifactResolutionProfileConfiguration.Builder builder =
            Saml2ArtifactResolutionProfileConfiguration.from(existing.getSaml2ArtifactResolutionProfileConfiguration());

        if (request.getStatus() != null) {

            builder.status(request.getStatus());
        }
        if (request.getInboundFlows() != null) {

            builder.inboundFlows(InterceptorFlows.of(request.getInboundFlows()));
        }
        if (request.getOutboundFlows() != null) {

            builder.outboundFlows(InterceptorFlows.of(request.getOutboundFlows()));
        }
        if (request.getMessageSigningPolicy() != null) {

            builder.messageSigningPolicy(request.getMessageSigningPolicy());
        }
        if (request.getRequestSignatureValidationPolicy() != null) {

            builder.requestSignatureValidationPolicy(request.getRequestSignatureValidationPolicy());
        }
        if (request.getEncryptionFallbackPolicy() != null) {

            builder.encryptionFallbackPolicy(request.getEncryptionFallbackPolicy());
        }
        if (request.getNameIdEncryptionPolicy() != null) {

            builder.nameIdEncryptionPolicy(request.getNameIdEncryptionPolicy());
        }
        if (request.getAssertionSigningPolicy() != null) {

            builder.assertionSigningPolicy(request.getAssertionSigningPolicy());
        }
        if (request.getAssertionEncryptionPolicy() != null) {

            builder.assertionEncryptionPolicy(request.getAssertionEncryptionPolicy());
        }
        if (request.getAttributeEncryptionPolicy() != null) {

            builder.attributeEncryptionPolicy(request.getAttributeEncryptionPolicy());
        }

        Result<Saml2ArtifactResolutionProfileConfiguration> built = builder.build();
        if (built.isFailure()) {

            return Result.failure(built.getError());
        }

        return existing.updateSaml2ArtifactResolutionProfileConfiguration(built.getValue());
    }

    private static Result<MetadataSource> toMetadataSource(MetadataSourceRequest request) {

        if (request instanceof NoneMetadataSourceRequest) {

            return Result.success(NoMetadataSource.getInstance());
        }

        if (request instanceof UriMetadataSourceRequest) {

            Result<URI> uri = parseUri(((UriMetadataSourceRequest) request).getUri(), "uri");
            if (uri.isFailure()) {

                return Result.failure(uri.getError());
            }
            return UriMetadataSource.of(uri.getValue());
        }

        if (request instanceof MdqMetadataSourceRequest) {

            Result<URI> baseUrl = parseUri(((MdqMetadataSourceRequest) request).getBaseUrl(), "base_url");
            if (baseUrl.isFailure()) {

                return Result.failure(baseUrl.getError());
            }
            Result<MdqMetadataSource> mdq = MdqMetadataSource.of(baseUrl.getValue());
            if (mdq.isFailure()) {

                return Result.failure(mdq.getError());
            }
            return Result.success(mdq.getValue());
        }

        if (request instanceof UpstreamMetadataSourceRequest) {

            UpstreamMetadataSourceRequest upstream = (UpstreamMetadataSourceRequest) request;

            Result<UUID> parentId = parseUuid(upstream.getParentId(), "parent_id");
            if (parentId.isFailure()) {

                return Result.failure(parentId.getError());
            }

            Result<URI> entityUri = parseUri(upstream.getEntityId(), "entity_id");
            if (entityUri.isFailure()) {

                return Result.failure(entityUri.getError());
            }

            Result<EntityId> entityId = EntityId.of(entityUri.getValue());
            if (entityId.isFailure()) {

                return Result.failure(entityId.getError());
            }

            return UpstreamMetadataSource.of(Id.of(parentId.getValue()), entityId.getValue());
        }

        if (request instanceof FileMetadataSourceRequest) {

            return FileMetadataSource.of(((FileMetadataSourceRequest) request).getToken());
        }

        if (request instanceof ManualMetadataSourceRequest) {

            return toManualMetadataSource((ManualMetadataSourceRequest) request);
        }

        return Result.failure(RequiredValueMissing.forField("type"));
    }

    private static Result<MetadataSource> toManualMetadataSource(ManualMetadataSourceRequest request) {

        Result<URI> entityUri = parseUri(request.getEntityId(), "entity_id");
        if (entityUri.isFailure()) {

            return Result.failure(entityUri.getError());
        }

        Result<EntityId> entityId = EntityId.of(entityUri.getValue());
        if (entityId.isFailure()) {

            return Result.failure(entityId.getError());
        }

        Result<Instant> instant = parseInstant(request.getValidUntil(), "valid_until");
        if (instant.isFailure()) {

            return Result.failure(instant.getError());
        }

        Result<ValidityPeriod> validUntil = ValidityPeriod.until(instant.getValue());
        if (validUntil.isFailure()) {

            return Result.failure(validUntil.getError());
        }

        Result<AssertionConsumerService> acs = toAssertionConsumerService(request.getAssertionConsumerService());
        if (acs.isFailure()) {

            return Result.failure(acs.getError());
        }

        Result<CertificateInfo> certificate = toCertificateInfo(request.getSigningCertificate());
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

    private static Result<AssertionConsumerService> toAssertionConsumerService(AssertionConsumerServiceRequest request) {

        if (request == null) {

            return Result.failure(RequiredValueMissing.forField("assertion_consumer_service"));
        }

        Result<URI> location = parseUri(request.getLocation(), "location");
        if (location.isFailure()) {

            return Result.failure(location.getError());
        }

        int index = request.getIndex() == null ? 1 : request.getIndex();
        boolean isDefault = request.getIsDefault() == null ? true : request.getIsDefault();

        return AssertionConsumerService.of(location.getValue(), request.getBinding(), index, isDefault);
    }

    private static Result<CertificateInfo> toCertificateInfo(String certificateData) {

        if (certificateData == null || certificateData.isBlank()) {

            return Result.success(new NoCertificateInfo());
        }

        return SamlX509CertificateInfo.fromBase64CertificateData(certificateData);
    }

    private static Result<URI> parseUri(String value, String field) {

        if (value == null || value.isBlank()) {

            return Result.failure(RequiredValueMissing.forField(field));
        }
        try {

            return Result.success(new URI(value));
        } catch (URISyntaxException e) {

            return Result.failure(InvalidUriSyntax.forValue(value));
        }
    }

    private static Result<UUID> parseUuid(String value, String field) {

        if (value == null || value.isBlank()) {

            return Result.failure(RequiredValueMissing.forField(field));
        }
        try {

            return Result.success(UUID.fromString(value));
        } catch (IllegalArgumentException e) {

            return Result.failure(InvalidUuidSyntax.forValue(value));
        }
    }

    private static Result<Instant> parseInstant(String value, String field) {

        if (value == null || value.isBlank()) {

            return Result.failure(RequiredValueMissing.forField(field));
        }
        try {

            return Result.success(Instant.parse(value));
        } catch (DateTimeParseException e) {

            return Result.failure(InvalidTimestampSyntax.forValue(value));
        }
    }

    /**
     * Projects a {@link TrustRelationship} onto its full view: its own fields, plus compact views of
     * the metadata source (kind only) and each profile (kind and status only), and the full released
     * attributes, activation diagnostics and discovered entity IDs.
     *
     * @throws IllegalStateException if the trust relationship, or any released attribute it holds,
     *     has no assigned id (see {@link #toSummary}).
     */
    public static TrustRelationshipDetail toDetail(TrustRelationship trustRelationship) {

        Result<UUID> id = trustRelationship.getId().getValue();
        if (id.isFailure()) {

            throw new IllegalStateException(
                "TrustRelationship reached the mapper without an assigned id; it must be persisted "
                    + "(which assigns the id) before being mapped to a DTO");
        }

        TrustRelationshipDetail detail = new TrustRelationshipDetail();
        detail.setId(id.getValue());
        detail.setDisplayName(trustRelationship.getDisplayName().getValue());
        detail.setDescription(trustRelationship.getDescription().getValue());
        detail.setNature(trustRelationship.getNature());
        detail.setStatus(trustRelationship.getStatus());
        detail.setVersion(trustRelationship.getVersion().getValue());
        detail.setMetadataSource(new MetadataSourceSummary(trustRelationship.getMetadataSource().getType()));
        detail.setProfiles(profileSummaries(trustRelationship));
        detail.setReleasedAttributes(releasedAttributes(trustRelationship.getReleasedAttributes()));
        detail.setActivationDiagnostics(activationDiagnostics(trustRelationship.getActivationDiagnostics()));
        detail.setDiscoveredEntityIds(entityIds(trustRelationship.getDiscoveredEntityIds()));
        return detail;
    }

    /**
     * Wraps one already-filtered, already-paged slice of trust relationships into a page of
     * summaries. Filtering and paging are the caller's (persistence layer's) responsibility; this
     * only shapes the envelope and derives {@code total_pages} and {@code number_of_elements}.
     *
     * @param trustRelationships the items in this page (in the order to present them)
     * @param number             the 1-based number of this page
     * @param size               the requested page size
     * @param totalElements      the total count across all pages matching the filters
     */
    public static TrustRelationshipPage toPage(List<TrustRelationship> trustRelationships,
        int number, int size, long totalElements) {

        List<TrustRelationshipSummary> items = new ArrayList<>();
        for (TrustRelationship trustRelationship : trustRelationships) {

            items.add(toSummary(trustRelationship));
        }

        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        PageMetadata page = new PageMetadata(size, number, totalElements, totalPages, items.size());
        return new TrustRelationshipPage(items, page);
    }

    private static List<ProfileSummary> profileSummaries(TrustRelationship tr) {

        return List.of(
            new ProfileSummary(
                tr.getShibbolethSsoProfileConfiguration().getType(),
                tr.getShibbolethSsoProfileConfiguration().getStatus()),
            new ProfileSummary(
                tr.getSaml2ArtifactResolutionProfileConfiguration().getType(),
                tr.getSaml2ArtifactResolutionProfileConfiguration().getStatus()),
            new ProfileSummary(
                tr.getSaml2AttributeQueryProfileConfiguration().getType(),
                tr.getSaml2AttributeQueryProfileConfiguration().getStatus()),
            new ProfileSummary(
                tr.getSaml2EcpProfileConfiguration().getType(),
                tr.getSaml2EcpProfileConfiguration().getStatus()),
            new ProfileSummary(
                tr.getSaml2SsoProfileConfiguration().getType(),
                tr.getSaml2SsoProfileConfiguration().getStatus()),
            new ProfileSummary(
                tr.getSaml2LogoutProfileConfiguration().getType(),
                tr.getSaml2LogoutProfileConfiguration().getStatus()));
    }

    private static List<ReleasedAttributeDto> releasedAttributes(ReleasedAttributes attributes) {

        List<ReleasedAttributeDto> out = new ArrayList<>();
        for (ReleasedAttribute attribute : attributes.getAttributes()) {

            Result<UUID> attributeId = attribute.getId().getValue();
            if (attributeId.isFailure()) {

                throw new IllegalStateException(
                    "Released attribute reached the mapper without an assigned id");
            }
            out.add(new ReleasedAttributeDto(attributeId.getValue(), attribute.getDisplayName()));
        }
        return out;
    }

    private static ActivationDiagnosticsDto activationDiagnostics(ActivationDiagnostics diagnostics) {

        List<ActivationLogEntryDto> logEntries = new ArrayList<>();
        for (ActivationLogEntry entry : diagnostics.getLogEntries()) {

            logEntries.add(new ActivationLogEntryDto(
                entry.getTimestamp().toString(), entry.getLevel(), entry.getMessage()));
        }

        return new ActivationDiagnosticsDto(
            diagnostics.getStatus(),
            diagnostics.getOrigin().getValue(),
            diagnostics.getStartedAt().toString(),
            diagnostics.getCompletedAt().toString(),
            logEntries);
    }

    private static List<String> entityIds(EntityIds entityIds) {

        List<String> out = new ArrayList<>();
        for (EntityId entityId : entityIds.getEntityIds()) {

            out.add(entityId.getValue().toString());
        }
        return out;
    }
}
