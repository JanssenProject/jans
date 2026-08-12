/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.metric;

import java.util.List;

import org.slf4j.Logger;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.metric.Fido2CeremonyOutcomeReport;
import io.jans.fido2.model.metric.Fido2MetricsConstants;
import io.jans.fido2.service.persist.AuthenticationPersistenceService;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2AuthenticationEntry;
import io.jans.orm.model.fido2.Fido2AuthenticationStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Annotates an assertion ceremony with what the browser observed when it ended.
 * <p>
 * The server can see that a ceremony was abandoned but never why: user verification happens inside
 * the authenticator, and the rejection that ends the ceremony is raised entirely in the browser. This
 * records the browser's account of it so a deliberate cancellation stops being indistinguishable from
 * a user who fought with their authenticator and gave up.
 * <p>
 * The report is untrusted input and is treated as an annotation only. It never creates a ceremony,
 * never changes a status, and never affects whether an authentication succeeds — the worst a forged
 * report can do is mislabel why one already-doomed ceremony was given up on.
 *
 * @author Janssen Project
 */
@ApplicationScoped
public class CeremonyOutcomeService {

    /** Matches the value the metrics store already uses when a reason cannot be determined. */
    private static final String UNKNOWN_CLIENT_ERROR = "UNKNOWN";

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private AuthenticationPersistenceService authenticationPersistenceService;

    /**
     * Records how a ceremony ended, if the report can be matched to one.
     * <p>
     * Deliberately silent about the outcome. The caller answers the same way whether the challenge
     * matched a ceremony or not, so the endpoint cannot be used to test whether a challenge exists.
     */
    public void report(Fido2CeremonyOutcomeReport outcomeReport) {
        if (!isReportingEnabled() || outcomeReport == null) {
            return;
        }

        String challenge = outcomeReport.getChallenge();
        if (challenge == null || challenge.trim().isEmpty()) {
            return;
        }

        try {
            List<Fido2AuthenticationEntry> ceremonies = authenticationPersistenceService.findByChallenge(challenge);
            if (ceremonies.isEmpty()) {
                log.debug("No assertion ceremony matches the reported challenge");
                return;
            }

            annotate(ceremonies.get(0), outcomeReport);
        } catch (Exception e) {
            // A report is telemetry. Losing one must never surface to the caller as a failure.
            log.warn("Failed to record ceremony outcome report: {}", e.getMessage());
        }
    }

    private void annotate(Fido2AuthenticationEntry ceremony, Fido2CeremonyOutcomeReport outcomeReport) {
        Fido2AuthenticationData authenticationData = ceremony.getAuthenticationData();
        if (authenticationData == null) {
            return;
        }

        // A report describes giving up, so it only applies to a ceremony that has not been resolved by
        // the server. `pending` is the normal case: the report arrives when the user gives up, which is
        // well before the sweep relabels the ceremony as `abandoned`. The annotation survives that
        // transition because the sweep writes only the status. Anything already `authenticated` or
        // `failed` was decided by evidence the server actually saw, which a client report cannot revise.
        Fido2AuthenticationStatus status = authenticationData.getStatus();
        if (status != Fido2AuthenticationStatus.pending && status != Fido2AuthenticationStatus.abandoned) {
            log.debug("Ignoring ceremony outcome report for a ceremony already resolved as {}", status);
            return;
        }

        // First report wins. Without this, anyone able to replay a challenge could keep rewriting how a
        // ceremony was characterised.
        if (authenticationData.getErrorReason() != null) {
            log.debug("Ignoring repeat ceremony outcome report");
            return;
        }

        authenticationData.setErrorReason(sanitizeErrorName(outcomeReport.getErrorName()));
        authenticationData.setErrorCategory(classify(outcomeReport.getElapsedMs()));

        authenticationPersistenceService.update(ceremony);
    }

    /**
     * Reads the elapsed time as a reason for giving up.
     * <p>
     * A rejection that arrives almost immediately means the user dismissed the prompt without trying.
     * One that arrives many seconds later means they were doing something in the meantime, and the only
     * thing available to do is attempt verification — repeatedly, and unsuccessfully. This is a
     * heuristic, and the boundary is configurable because how long a real attempt takes varies by
     * authenticator and by user.
     */
    private String classify(long elapsedMs) {
        if (elapsedMs <= 0) {
            // Absent or nonsensical. Recorded as reported-but-unclassifiable rather than silently
            // bucketed as a deliberate cancellation, which is what a zero would otherwise look like.
            return Fido2MetricsConstants.CLIENT_ABANDONED_UNCLASSIFIED;
        }

        int threshold = appConfiguration.getFido2Configuration().getDeliberateCancellationThresholdMs();
        return elapsedMs < threshold ? Fido2MetricsConstants.CLIENT_CANCELLED
                : Fido2MetricsConstants.CLIENT_VERIFICATION_ABANDONED;
    }

    /**
     * The error name is written by the browser and reaches persistence and logs, so it is bounded and
     * stripped of anything that is not a plain DOMException-style identifier.
     */
    private String sanitizeErrorName(String errorName) {
        if (errorName == null) {
            return UNKNOWN_CLIENT_ERROR;
        }

        String sanitized = errorName.replaceAll("[^A-Za-z0-9_]", "");
        if (sanitized.isEmpty()) {
            return UNKNOWN_CLIENT_ERROR;
        }
        if (sanitized.length() > Fido2MetricsConstants.MAX_LENGTH_CLIENT_ERROR_NAME) {
            return sanitized.substring(0, Fido2MetricsConstants.MAX_LENGTH_CLIENT_ERROR_NAME);
        }
        return sanitized;
    }

    private boolean isReportingEnabled() {
        return appConfiguration.getFido2Configuration() != null
                && appConfiguration.getFido2Configuration().isAcceptCeremonyOutcomeReports();
    }
}
