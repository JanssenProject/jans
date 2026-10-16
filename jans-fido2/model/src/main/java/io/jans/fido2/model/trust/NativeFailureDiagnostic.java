/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.trust;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Internal diagnostic codes for failures that are hard to tell apart once they reach the metrics
 * store as free-text exception messages — the first of which, {@link #JFS_RPID_HASH_MISMATCH}, is a
 * common symptom of a native-app misconfiguration (a wrong Android asset-link or iOS AASA association
 * causing the platform to present the wrong RP ID to the authenticator). See issue #14608.
 * <p>
 * A code is written to {@code Fido2MetricsEntry.errorReason}, with {@link #CATEGORY} as the error
 * category, so failures can be counted by cause. These are strictly internal: the public FIDO
 * response envelope is unchanged and a code never reaches the client body.
 * <p>
 * A separate enum from {@link AttestationTrustDiagnostic} rather than an added value there: this
 * check runs on both the registration and authentication paths (not attestation-trust specific), and
 * {@link AttestationTrustDiagnostic#isDiagnosticCode(String)} deliberately matches against its own
 * enum values rather than the shared {@code JFS_} prefix, precisely so codes from this enum are never
 * misfiled under {@link AttestationTrustDiagnostic#CATEGORY}.
 *
 * @author Janssen Project
 */
public enum NativeFailureDiagnostic {

    /**
     * The RP ID hash the authenticator signed over does not match the hash of the RP ID this server
     * expected. Not exclusive to native clients in principle, but in practice a hallmark of a
     * misconfigured Android asset link or iOS AASA association presenting the wrong RP ID.
     */
    JFS_RPID_HASH_MISMATCH;

    /** Value written to {@code Fido2MetricsEntry.errorCategory} for every code in this enum. */
    public static final String CATEGORY = "NATIVE_FAILURE";

    /** Shared prefix for internal diagnostic codes, of which these are the native-failure subset. */
    public static final String CODE_PREFIX = "JFS_";

    private static final Set<String> CODE_NAMES;

    static {
        Set<String> names = new HashSet<>();
        for (NativeFailureDiagnostic diagnostic : values()) {
            names.add(diagnostic.name());
        }
        CODE_NAMES = Collections.unmodifiableSet(names);
    }

    /**
     * Whether a recorded {@code errorReason} is one of <em>these</em> diagnostic codes.
     * <p>
     * Deliberately matched against the enum values rather than the shared {@link #CODE_PREFIX} —
     * mirrors {@link AttestationTrustDiagnostic#isDiagnosticCode(String)}, for the same reason: a
     * prefix test would file {@code AttestationTrustDiagnostic} codes under this enum's
     * {@link #CATEGORY} too.
     *
     * @param errorReason the recorded reason; may be null
     * @return true when the reason is a native-failure diagnostic code
     */
    public static boolean isDiagnosticCode(String errorReason) {
        return errorReason != null && CODE_NAMES.contains(errorReason);
    }
}
