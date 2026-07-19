package io.jans.shibboleth.trust.dto.mapper.config;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.EntityId;
import io.jans.shibboleth.trust.config.EntityIds;
import io.jans.shibboleth.trust.config.ReleasedAttribute;
import io.jans.shibboleth.trust.config.ReleasedAttributes;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.dto.config.ActivationDiagnosticsDto;
import io.jans.shibboleth.trust.dto.config.ActivationLogEntryDto;
import io.jans.shibboleth.trust.dto.config.CreateTrustRelationshipRequest;
import io.jans.shibboleth.trust.dto.config.MetadataSourceSummary;
import io.jans.shibboleth.trust.dto.config.ProfileSummary;
import io.jans.shibboleth.trust.dto.config.ReleasedAttributeDto;
import io.jans.shibboleth.trust.dto.config.TrustRelationshipDetail;
import io.jans.shibboleth.trust.dto.config.TrustRelationshipPage;
import io.jans.shibboleth.trust.dto.config.TrustRelationshipSummary;
import io.jans.shibboleth.trust.dto.shared.PageMetadata;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationLogEntry;

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
