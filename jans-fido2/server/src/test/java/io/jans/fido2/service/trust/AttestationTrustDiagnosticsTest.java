/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.trust;

import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.exception.Fido2TrustException;
import io.jans.fido2.model.trust.AttestationTrustDiagnostic;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttestationTrustDiagnosticsTest {

    @Test
    void resolveCode_ifTrustFailureThrownDirectly_returnsItsCode() {
        Fido2TrustException error = new Fido2TrustException(AttestationTrustDiagnostic.JFS_AAGUID_NOT_IN_MDS,
                "1234-5678", "Authenticator not in TOC aaguid 1234-5678");

        assertEquals("JFS_AAGUID_NOT_IN_MDS", AttestationTrustDiagnostics.resolveCode(error));
        assertEquals("1234-5678", AttestationTrustDiagnostics.resolveAaguid(error));
    }

    /**
     * Trust failures that must surface as a FIDO error envelope are thrown as a
     * WebApplicationException with the diagnostic attached as the cause. That is the shape the
     * certificate, attestation-format and Apple paths produce.
     */
    @Test
    void resolveCode_ifTrustFailureIsTheCause_readsThroughTheEnvelope() {
        Fido2TrustException trustFailure = new Fido2TrustException(
                AttestationTrustDiagnostic.JFS_ROOT_CERT_NOT_TRUSTED, null, "chain did not verify", null);
        WebApplicationException thrown = new WebApplicationException(trustFailure,
                Response.status(Response.Status.BAD_REQUEST).build());

        assertEquals("JFS_ROOT_CERT_NOT_TRUSTED", AttestationTrustDiagnostics.resolveCode(thrown));
    }

    @Test
    void resolveCode_ifNestedDeeper_stillFindsIt() {
        Fido2TrustException trustFailure = new Fido2TrustException(
                AttestationTrustDiagnostic.JFS_APPLE_ROOT_CA_MISSING, null, "no Apple root", null);
        Throwable wrapped = new IllegalStateException("outer", new RuntimeException("middle", trustFailure));

        assertEquals("JFS_APPLE_ROOT_CA_MISSING", AttestationTrustDiagnostics.resolveCode(wrapped));
    }

    /**
     * A non-trust failure resolves to null, so the caller keeps the original message. Classification
     * must never swallow the detail of failures it does not understand.
     */
    @Test
    void resolveCode_ifNotTrustRelated_returnsNull() {
        assertNull(AttestationTrustDiagnostics.resolveCode(new Fido2RuntimeException("Challenge mismatch")));
        assertNull(AttestationTrustDiagnostics.resolveCode(new IllegalArgumentException("bad input")));
        assertNull(AttestationTrustDiagnostics.resolveCode(null));
        assertNull(AttestationTrustDiagnostics.resolveAaguid(new IllegalStateException("boom")));
    }

    /**
     * A cause chain that loops back on itself must not hang the metrics recorder, which runs on the
     * request path.
     */
    @Test
    void resolveCode_ifCauseChainIsCyclic_terminates() {
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second", first);
        first.initCause(second);

        assertNull(AttestationTrustDiagnostics.resolveCode(first));
    }

    @Test
    void resolveCode_ifSelfReferentialCause_terminates() {
        RuntimeException selfReferential = new RuntimeException("loop") {
            private static final long serialVersionUID = 1L;

            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };

        assertNull(AttestationTrustDiagnostics.resolveCode(selfReferential));
    }

    @Test
    void resolve_returnsTheEnumConstant() {
        Fido2TrustException error = new Fido2TrustException(
                AttestationTrustDiagnostic.JFS_AUTHENTICATOR_STATUS_UNACCEPTABLE, "aa", "revoked");

        assertEquals(AttestationTrustDiagnostic.JFS_AUTHENTICATOR_STATUS_UNACCEPTABLE,
                AttestationTrustDiagnostics.resolve(error));
    }

    /**
     * Every code shares the prefix, so the metrics layer can tell a deliberately recorded code from a
     * free-text message without enumerating the values — the native-metrics work writes into the same
     * field and must not be mistaken for one.
     */
    @Test
    void isDiagnosticCode_recognisesEveryCodeAndRejectsMessages() {
        for (AttestationTrustDiagnostic diagnostic : AttestationTrustDiagnostic.values()) {
            assertTrue(AttestationTrustDiagnostic.isDiagnosticCode(diagnostic.name()),
                    diagnostic + " must be recognised as a diagnostic code");
        }

        assertFalse(AttestationTrustDiagnostic.isDiagnosticCode("Authenticator not in TOC aaguid 1234"));
        assertFalse(AttestationTrustDiagnostic.isDiagnosticCode(null));
    }

    /**
     * The native-metrics work writes its own JFS_ codes into the same errorReason field. Matching on
     * the shared prefix would file those under ATTESTATION_TRUST and inflate the attestation-rejection
     * analytics with rejections that have nothing to do with attestation trust.
     */
    @Test
    void isDiagnosticCode_rejectsForeignCodesSharingThePrefix() {
        assertFalse(AttestationTrustDiagnostic.isDiagnosticCode("JFS_NATIVE_BROWSER_UNSUPPORTED"));
        assertFalse(AttestationTrustDiagnostic.isDiagnosticCode(AttestationTrustDiagnostic.CODE_PREFIX));
        assertFalse(AttestationTrustDiagnostic.isDiagnosticCode("JFS_AAGUID_NOT_IN_MDS_EXTRA"));
    }
}
