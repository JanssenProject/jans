package io.jans.shibboleth.trust.dto.activation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.activation.model.WorkItemType;

import java.util.Objects;
import java.util.UUID;

/**
 * Read view of a unit of activation work. {@code lease_expires_at} is an ISO-8601 date-time when the
 * item is leased (ASSIGNED), and omitted otherwise (PENDING / terminal with no active lease).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkItemView {

    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("type")
    private final WorkItemType type;

    @JsonProperty("trust_relationship_ref")
    private final UUID trustRelationshipRef;

    @JsonProperty("state")
    private final WorkItemState state;

    @JsonProperty("lease_expires_at")
    private final String leaseExpiresAt;

    public WorkItemView(UUID id, WorkItemType type, UUID trustRelationshipRef, WorkItemState state,
        String leaseExpiresAt) {

        this.id = id;
        this.type = type;
        this.trustRelationshipRef = trustRelationshipRef;
        this.state = state;
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public UUID getId() {

        return id;
    }

    public WorkItemType getType() {

        return type;
    }

    public UUID getTrustRelationshipRef() {

        return trustRelationshipRef;
    }

    public WorkItemState getState() {

        return state;
    }

    public String getLeaseExpiresAt() {

        return leaseExpiresAt;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkItemView that = (WorkItemView) o;
        return Objects.equals(id, that.id)
            && type == that.type
            && Objects.equals(trustRelationshipRef, that.trustRelationshipRef)
            && state == that.state
            && Objects.equals(leaseExpiresAt, that.leaseExpiresAt);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, type, trustRelationshipRef, state, leaseExpiresAt);
    }

    @Override
    public String toString() {

        return "WorkItemView{id=" + id + ", type=" + type + ", trustRelationshipRef=" + trustRelationshipRef
            + ", state=" + state + ", leaseExpiresAt='" + leaseExpiresAt + "'}";
    }
}
