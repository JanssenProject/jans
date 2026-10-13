package io.jans.shibboleth.trust.dto.error;

import io.jans.adapter.error.KernelErrorCodes;
import io.jans.adapter.error.ProblemTranslation;
import io.jans.adapter.error.Violation;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps each domain error type to the code, title, status and wording clients see.
 *
 * <p>Codes and statuses are declared here rather than derived on purpose: both are published API
 * contract, so renaming a domain class must not silently change them. The mapping is verified
 * exhaustive by test, so a new domain error type fails the build instead of degrading to a generic
 * response.
 *
 * <p>Only the code, status, title and field name reach the client. A domain error's own
 * {@link DomainError#getMessage()} is never forwarded — it names domain types and their internal
 * fields, which are implementation details.
 */
public final class ErrorCodes {

    /**
     * Code used when an error reaches the boundary without a registered mapping. Reaching it means
     * the registry below is out of date.
     */
    public static final String UNEXPECTED = KernelErrorCodes.UNEXPECTED.code();

    private static final Map<Class<? extends DomainError>, ProblemTranslation> TRANSLATIONS = translations();

    /**
     * Errors that describe one field of the request body. Only these become a {@link Violation};
     * every other error describes the request as a whole and carries no field to attach one to.
     */
    private static final Set<Class<? extends DomainError>> FIELD_SCOPED = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            RequiredValueMissing.class,
            InvalidUriSyntax.class,
            InvalidUuidSyntax.class,
            InvalidTimestampSyntax.class,
            InvalidDurationSyntax.class,
            InvalidVersion.class)));

    private ErrorCodes() {
    }

    public static ProblemTranslation translationFor(DomainError error) {

        return translationFor(error.getClass());
    }

    public static ProblemTranslation translationFor(Class<? extends DomainError> errorType) {

        return TRANSLATIONS.getOrDefault(errorType, KernelErrorCodes.UNEXPECTED);
    }

    public static String codeFor(DomainError error) {

        return translationFor(error).code();
    }

    public static String codeFor(Class<? extends DomainError> errorType) {

        return translationFor(errorType).code();
    }

    /**
     * Prose describing {@code error} as it applies to {@code field}, built from the registered
     * template rather than from the error's own message.
     */
    public static String messageFor(DomainError error, String field) {

        return translationFor(error).message(field);
    }

    /**
     * Whether {@code error} describes one field of the request body, and so can be reported as a
     * {@link Violation} against that field.
     */
    public static boolean isFieldScoped(DomainError error) {

        return FIELD_SCOPED.contains(error.getClass());
    }

    public static boolean isRegistered(Class<? extends DomainError> errorType) {

        return TRANSLATIONS.containsKey(errorType);
    }

    public static Set<Class<? extends DomainError>> registeredTypes() {

        return TRANSLATIONS.keySet();
    }

    private static Map<Class<? extends DomainError>, ProblemTranslation> translations() {

        Map<Class<? extends DomainError>, ProblemTranslation> map = new HashMap<>();

        // field-scoped: one field of the request body is wrong. Reported as violations, so the
        // status only surfaces when such an error escapes on its own.
        // The shared kernel's own error — code, title, status and wording fixed for every context.
        map.put(RequiredValueMissing.class, KernelErrorCodes.REQUIRED_VALUE_MISSING);
        map.put(InvalidUriSyntax.class, new ProblemTranslation(
            "invalid_uri_syntax", "Invalid URI syntax", 400, "'%s' is not a valid URI"));
        map.put(InvalidUuidSyntax.class, new ProblemTranslation(
            "invalid_uuid_syntax", "Invalid UUID syntax", 400, "'%s' is not a valid UUID"));
        map.put(InvalidTimestampSyntax.class, new ProblemTranslation(
            "invalid_timestamp_syntax", "Invalid timestamp syntax", 400,
            "'%s' is not a valid ISO-8601 timestamp"));
        map.put(InvalidDurationSyntax.class, new ProblemTranslation(
            "invalid_duration_syntax", "Invalid duration syntax", 400,
            "'%s' is not a valid ISO-8601 duration"));
        // a version below the stored minimum is a stale write, not malformed input
        map.put(InvalidVersion.class, new ProblemTranslation(
            "invalid_version", "Invalid version", 409,
            "'%s' is below the current version; re-read the resource and retry"));

        // the target does not exist
        map.put(TrustRelationshipNotFound.class, new ProblemTranslation(
            "trust_relationship_not_found", "Trust relationship not found", 404,
            "The requested trust relationship does not exist"));
        map.put(WorkItemNotFound.class, new ProblemTranslation(
            "work_item_not_found", "Work item not found", 404,
            "The requested work item does not exist"));
        map.put(WorkerNotFound.class, new ProblemTranslation(
            "worker_not_found", "Worker not found", 404, "The requested worker is not registered"));

        // well-formed and understood, but cannot be applied to this resource
        map.put(IncompatibleMetadataSourceForNature.class, new ProblemTranslation(
            "incompatible_metadata_source_for_nature", "Incompatible metadata source", 422,
            "The requested metadata source is not allowed for this trust relationship's nature"));
        map.put(OperationRestrictedToNature.class, new ProblemTranslation(
            "operation_restricted_to_nature", "Operation restricted by nature", 422,
            "This operation is not available for this trust relationship's nature"));
        map.put(DomainObjectCreationFailed.class, new ProblemTranslation(
            "creation_failed", "Creation failed", 422,
            "The request could not be used to create the resource"));
        map.put(DomainObjectUpdateFailed.class, new ProblemTranslation(
            "update_failed", "Update failed", 422,
            "The request could not be applied to the resource"));

        // conflicts with the resource's current state
        map.put(OperationForbiddenFromStatus.class, new ProblemTranslation(
            "operation_forbidden_from_status", "Operation forbidden from current status", 409,
            "This operation is not allowed while the trust relationship is in its current status"));
        map.put(InvalidStatusForOperation.class, new ProblemTranslation(
            "invalid_status_for_operation", "Invalid status for operation", 409,
            "The requested status change is not allowed from the current status"));
        map.put(WorkItemTransitionNotAllowed.class, new ProblemTranslation(
            "work_item_transition_not_allowed", "Work item transition not allowed", 409,
            "The requested work item transition is not allowed from its current state"));
        map.put(LeaseAlreadyHeld.class, new ProblemTranslation(
            "lease_already_held", "Lease already held", 409,
            "The work item is already leased by another worker"));
        map.put(LeaseNotPresent.class, new ProblemTranslation(
            "lease_not_present", "Lease not present", 409, "The work item has no active lease"));
        map.put(LeaseStillValid.class, new ProblemTranslation(
            "lease_still_valid", "Lease still valid", 409, "The existing lease has not yet expired"));
        map.put(NotLeaseHolder.class, new ProblemTranslation(
            "not_lease_holder", "Not the lease holder", 409,
            "The operation must be performed by the current lease holder"));
        map.put(WorkerNotAlive.class, new ProblemTranslation(
            "worker_not_alive", "Worker not alive", 409, "The claiming worker is not alive"));
        map.put(StaleReport.class, new ProblemTranslation(
            "stale_report", "Stale report", 409,
            "The report was superseded by a newer activation episode"));

        // not offered by this deployment
        map.put(UnsupportedOperation.class, new ProblemTranslation(
            "unsupported_operation", "Unsupported operation", 501, "This operation is not supported"));

        // our side is at fault, not the caller's
        map.put(DomainObjectConsistencyFailed.class, new ProblemTranslation(
            "consistency_failed", "Stored resource inconsistent", 500,
            "The stored resource is inconsistent and could not be read"));
        map.put(IdNotAssigned.class, new ProblemTranslation(
            "id_not_assigned", "Identifier not assigned", 500,
            "The resource has no assigned identifier"));
        map.put(TrustTransitionError.class, new ProblemTranslation(
            "trust_transition_failed", "Trust transition failed", 500,
            "The trust relationship state transition could not be run"));

        return Collections.unmodifiableMap(map);
    }
}
