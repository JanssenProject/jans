package io.jans.shibboleth.trust.dto.mapper.config;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.dto.config.CreateTrustRelationshipRequest;
import io.jans.shibboleth.trust.dto.config.TrustRelationshipSummary;
import io.jans.shibboleth.trust.shared.Result;

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
}
