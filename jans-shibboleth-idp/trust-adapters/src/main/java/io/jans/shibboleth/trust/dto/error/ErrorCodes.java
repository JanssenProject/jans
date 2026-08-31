package io.jans.shibboleth.trust.dto.error;

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
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maps each domain error type to the stable code clients branch on, and to the prose template used
 * to describe it.
 *
 * <p>Codes are declared here rather than derived from class names on purpose: a code is a published
 * API contract, so renaming a domain class must not silently change it. The mapping is verified
 * exhaustive by test, so a new domain error type fails the build instead of degrading to a generic
 * response.
 *
 * <p>Only the code and the field name reach the client. A domain error's own
 * {@link DomainError#getMessage()} is never forwarded — it names domain types and their internal
 * fields, which are implementation details.
 */
public final class ErrorCodes {

    /**
     * Code used when an error reaches the boundary without a registered mapping. Reaching it means
     * the registry below is out of date.
     */
    public static final String UNEXPECTED = "unexpected_error";

    private static final class Translation {

        private final String code;
        private final String template;

        private Translation(String code, String template) {

            this.code = code;
            this.template = template;
        }
    }

    private static final Map<Class<? extends DomainError>, Translation> TRANSLATIONS = translations();

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

    public static String codeFor(DomainError error) {

        return codeFor(error.getClass());
    }

    public static String codeFor(Class<? extends DomainError> errorType) {

        Translation translation = TRANSLATIONS.get(errorType);
        return translation == null ? UNEXPECTED : translation.code;
    }

    /**
     * Prose describing {@code error} as it applies to {@code field}, built from the registered
     * template rather than from the error's own message.
     */
    public static String messageFor(DomainError error, String field) {

        Translation translation = TRANSLATIONS.get(error.getClass());

        if (translation == null) {

            return field.isEmpty()
                ? "The request could not be processed"
                : String.format("'%s' could not be processed", field);
        }

        return String.format(translation.template, field);
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

    private static Map<Class<? extends DomainError>, Translation> translations() {

        Map<Class<? extends DomainError>, Translation> map = new HashMap<>();

        // field-scoped: something about one field of the request body is wrong
        map.put(RequiredValueMissing.class,
            new Translation("required_value_missing", "'%s' is required"));
        map.put(InvalidUriSyntax.class,
            new Translation("invalid_uri_syntax", "'%s' is not a valid URI"));
        map.put(InvalidUuidSyntax.class,
            new Translation("invalid_uuid_syntax", "'%s' is not a valid UUID"));
        map.put(InvalidTimestampSyntax.class,
            new Translation("invalid_timestamp_syntax", "'%s' is not a valid ISO-8601 timestamp"));
        map.put(InvalidDurationSyntax.class,
            new Translation("invalid_duration_syntax", "'%s' is not a valid ISO-8601 duration"));
        map.put(InvalidVersion.class,
            new Translation("invalid_version", "'%s' is not a valid version"));

        // request-scoped: the request is well-formed but not allowed, or the target is absent
        map.put(TrustRelationshipNotFound.class,
            new Translation("trust_relationship_not_found", "The requested trust relationship does not exist"));
        map.put(IncompatibleMetadataSourceForNature.class,
            new Translation("incompatible_metadata_source_for_nature",
                "The requested metadata source is not allowed for this trust relationship's nature"));
        map.put(OperationRestrictedToNature.class,
            new Translation("operation_restricted_to_nature",
                "This operation is not available for this trust relationship's nature"));
        map.put(OperationForbiddenFromStatus.class,
            new Translation("operation_forbidden_from_status",
                "This operation is not allowed while the trust relationship is in its current status"));
        map.put(InvalidStatusForOperation.class,
            new Translation("invalid_status_for_operation",
                "The requested status change is not allowed from the current status"));
        map.put(UnsupportedOperation.class,
            new Translation("unsupported_operation", "This operation is not supported"));
        map.put(TrustTransitionError.class,
            new Translation("trust_transition_failed", "The trust relationship state transition could not be run"));
        map.put(DomainObjectCreationFailed.class,
            new Translation("creation_failed", "The request could not be used to create the resource"));
        map.put(DomainObjectUpdateFailed.class,
            new Translation("update_failed", "The request could not be applied to the resource"));
        map.put(DomainObjectConsistencyFailed.class,
            new Translation("consistency_failed", "The stored resource is inconsistent and could not be read"));
        map.put(IdNotAssigned.class,
            new Translation("id_not_assigned", "The resource has no assigned id"));

        // activation context
        map.put(WorkItemNotFound.class,
            new Translation("work_item_not_found", "The requested work item does not exist"));
        map.put(WorkItemTransitionNotAllowed.class,
            new Translation("work_item_transition_not_allowed",
                "The requested work item transition is not allowed from its current state"));
        map.put(WorkerNotFound.class,
            new Translation("worker_not_found", "The requested worker is not registered"));
        map.put(WorkerNotAlive.class,
            new Translation("worker_not_alive", "The requested worker is not alive"));
        map.put(LeaseAlreadyHeld.class,
            new Translation("lease_already_held", "The work item is already leased by another worker"));
        map.put(LeaseNotPresent.class,
            new Translation("lease_not_present", "The work item has no active lease"));
        map.put(LeaseStillValid.class,
            new Translation("lease_still_valid", "The existing lease has not yet expired"));
        map.put(NotLeaseHolder.class,
            new Translation("not_lease_holder", "The presented worker does not hold this work item's lease"));
        map.put(StaleReport.class,
            new Translation("stale_report", "The report was superseded by a newer lease generation"));

        return Collections.unmodifiableMap(map);
    }
}
