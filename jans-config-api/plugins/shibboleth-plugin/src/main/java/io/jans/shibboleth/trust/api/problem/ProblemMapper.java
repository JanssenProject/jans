package io.jans.shibboleth.trust.api.problem;

import java.util.List;

import io.jans.kernel.DomainError;
import io.jans.kernel.RequiredValueMissing;
import io.jans.shibboleth.trust.activation.error.LeaseAlreadyHeld;
import io.jans.shibboleth.trust.activation.error.LeaseNotPresent;
import io.jans.shibboleth.trust.activation.error.LeaseStillValid;
import io.jans.shibboleth.trust.activation.error.NotLeaseHolder;
import io.jans.shibboleth.trust.activation.error.StaleReport;
import io.jans.shibboleth.trust.activation.error.WorkItemNotFound;
import io.jans.shibboleth.trust.activation.error.WorkItemTransitionNotAllowed;
import io.jans.shibboleth.trust.activation.error.WorkerNotAlive;
import io.jans.shibboleth.trust.activation.error.WorkerNotFound;
import io.jans.shibboleth.trust.config.error.DomainObjectConsistencyFailed;
import io.jans.shibboleth.trust.config.error.DomainObjectCreationFailed;
import io.jans.shibboleth.trust.config.error.DomainObjectUpdateFailed;
import io.jans.shibboleth.trust.config.error.IdNotAssigned;
import io.jans.shibboleth.trust.config.error.IncompatibleMetadataSourceForNature;
import io.jans.shibboleth.trust.config.error.InvalidDurationSyntax;
import io.jans.shibboleth.trust.config.error.InvalidStatusForOperation;
import io.jans.shibboleth.trust.config.error.InvalidTimestampSyntax;
import io.jans.shibboleth.trust.config.error.InvalidUriSyntax;
import io.jans.shibboleth.trust.config.error.InvalidUuidSyntax;
import io.jans.shibboleth.trust.config.error.InvalidVersion;
import io.jans.shibboleth.trust.config.error.OperationForbiddenFromStatus;
import io.jans.shibboleth.trust.config.error.OperationRestrictedToNature;
import io.jans.shibboleth.trust.config.error.TrustRelationshipNotFound;
import io.jans.shibboleth.trust.config.error.TrustTransitionError;
import io.jans.shibboleth.trust.config.error.UnsupportedOperation;

public final class ProblemMapper {

    private static final String TYPE_BASE = "https://jans.io/shibboleth-idp/problems/";

    private ProblemMapper() {
    }

    public static Problem toProblem(DomainError error) {

        return toProblem(error, null);
    }

    public static Problem toProblem(DomainError error, String instance) {

        // Wrapping errors carry the real reason as a nested cause; surface that instead of the wrapper.
        if (error instanceof DomainObjectCreationFailed) {

            DomainError cause = ((DomainObjectCreationFailed) error).getCause();
            return cause != null ? toProblem(cause, instance)
                : problem(400, "domain_object_creation_failed", "Could not create resource", error, instance, null);
        }
        if (error instanceof DomainObjectUpdateFailed) {

            DomainError cause = ((DomainObjectUpdateFailed) error).getCause();
            return cause != null ? toProblem(cause, instance)
                : problem(400, "domain_object_update_failed", "Could not update resource", error, instance, null);
        }
        if (error instanceof DomainObjectConsistencyFailed) {

            DomainError cause = ((DomainObjectConsistencyFailed) error).getCause();
            return cause != null ? toProblem(cause, instance)
                : problem(409, "domain_object_consistency_failed", "Resource is in an inconsistent state", error, instance, null);
        }

        // 404 - resource lookups
        if (error instanceof TrustRelationshipNotFound) {

            return problem(404, "trust_relationship_not_found", "Trust relationship not found", error, instance, null);
        }
        if (error instanceof WorkItemNotFound) {

            return problem(404, "work_item_not_found", "Work item not found", error, instance, null);
        }
        if (error instanceof WorkerNotFound) {

            return problem(404, "worker_not_found", "Worker not found", error, instance, null);
        }

        // 409 - state / concurrency conflicts
        if (error instanceof InvalidStatusForOperation) {

            return problem(409, "invalid_status_for_operation", "Operation not allowed in current state", error, instance, null);
        }
        if (error instanceof OperationForbiddenFromStatus) {

            return problem(409, "operation_forbidden_from_status", "Operation not allowed in current state", error, instance, null);
        }
        if (error instanceof OperationRestrictedToNature) {

            return problem(409, "operation_restricted_to_nature", "Operation not allowed for this trust relationship", error, instance, null);
        }
        if (error instanceof TrustTransitionError) {

            return problem(409, "trust_transition_failed", "Trust relationship state transition failed", error, instance, null);
        }
        if (error instanceof InvalidVersion) {

            return problem(409, "invalid_version", "Resource version is not acceptable", error, instance, null);
        }
        if (error instanceof LeaseAlreadyHeld) {

            return problem(409, "lease_already_held", "The activation lease is held by another worker", error, instance, null);
        }
        if (error instanceof LeaseStillValid) {

            return problem(409, "lease_still_valid", "The activation lease has not expired", error, instance, null);
        }
        if (error instanceof LeaseNotPresent) {

            return problem(409, "lease_not_present", "No activation lease is present", error, instance, null);
        }
        if (error instanceof NotLeaseHolder) {

            return problem(409, "not_lease_holder", "Operation must be performed by the current lease holder", error, instance, null);
        }
        if (error instanceof StaleReport) {

            return problem(409, "stale_report", "The report does not name the current work item", error, instance, null);
        }
        if (error instanceof WorkerNotAlive) {

            return problem(409, "worker_not_alive", "The claiming worker is not alive", error, instance, null);
        }
        if (error instanceof WorkItemTransitionNotAllowed) {

            return problem(409, "work_item_transition_not_allowed", "Work item state transition not allowed", error, instance, null);
        }

        // 400 - malformed / invalid input
        if (error instanceof RequiredValueMissing) {

            RequiredValueMissing missing = (RequiredValueMissing) error;
            List<Violation> violations = List.of(
                new Violation(missing.getFieldName(), "required_value_missing", missing.getMessage()));
            return problem(400, "required_value_missing", "Required value missing", error, instance, violations);
        }
        if (error instanceof IncompatibleMetadataSourceForNature) {

            return problem(400, "incompatible_metadata_source_for_nature", "Metadata source is incompatible with the trust relationship nature", error, instance, null);
        }
        if (error instanceof IdNotAssigned) {

            return problem(400, "id_not_assigned", "A required id is not assigned", error, instance, null);
        }
        if (error instanceof InvalidUuidSyntax) {

            return problem(400, "invalid_uuid_syntax", "Invalid UUID syntax", error, instance, null);
        }
        if (error instanceof InvalidUriSyntax) {

            return problem(400, "invalid_uri_syntax", "Invalid URI syntax", error, instance, null);
        }
        if (error instanceof InvalidDurationSyntax) {

            return problem(400, "invalid_duration_syntax", "Invalid duration syntax", error, instance, null);
        }
        if (error instanceof InvalidTimestampSyntax) {

            return problem(400, "invalid_timestamp_syntax", "Invalid timestamp syntax", error, instance, null);
        }
        if (error instanceof UnsupportedOperation) {

            return problem(400, "unsupported_operation", "Unsupported operation", error, instance, null);
        }

        // Unrecognized error: stay a documented 400 rather than leak as a 500.
        return problem(400, "invalid_request", "Invalid request", error, instance, null);
    }

    private static Problem problem(int status, String code, String title, DomainError error, String instance,
            List<Violation> violations) {

        return new Problem(TYPE_BASE + code, title, status, error.getMessage(), instance, code, violations);
    }
}
