/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.trust;

import io.jans.fido2.model.trust.AttestationTrustDiagnostic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttestationTrustDiagnosticTest {

    // The expected inputs are the messages the failure sites actually raise, not invented strings.

    @Test
    void resolveCode_ifAuthenticatorNotInToc_returnsAaguidNotInMds() {
        // MdsService: "Authenticator not in TOC aaguid " + aaguid
        assertEquals("JFS_AAGUID_NOT_IN_MDS",
                AttestationTrustDiagnostic.resolveCode("Authenticator not in TOC aaguid 83c44309"));
    }

    @Test
    void resolveCode_ifStatusUndesirable_returnsStatusUnacceptable() {
        // MdsService: "Authenticator " + aaguid + "status undesirable " + status
        assertEquals("JFS_AUTHENTICATOR_STATUS_UNACCEPTABLE",
                AttestationTrustDiagnostic.resolveCode("Authenticator 83c44309 status undesirable REVOKED"));
    }

    @Test
    void resolveCode_ifStatusReportRejection_returnsStatusUnacceptable() {
        // CertificateVerifier.verifyStatusAcceptable: "Ignore entry AAGUID: %s due to status: %s"
        assertEquals("JFS_AUTHENTICATOR_STATUS_UNACCEPTABLE",
                AttestationTrustDiagnostic.resolveCode("Ignore entry AAGUID: 83c44309 due to status: USER_VERIFICATION_BYPASS"));
    }

    @Test
    void resolveCode_ifCertificateChainProblem_returnsRootCertNotTrusted() {
        // CertificateVerifier.verifyAttestationCertificates: "Problem with certificate: ..."
        assertEquals("JFS_ROOT_CERT_NOT_TRUSTED",
                AttestationTrustDiagnostic.resolveCode("Problem with certificate: unable to find valid certification path"));
    }

    @Test
    void resolveCode_ifNotTrustRelated_returnsNull() {
        // A non-trust failure must keep its original message, so nothing is lost for other errors.
        assertNull(AttestationTrustDiagnostic.resolveCode("Challenge is not valid"));
        assertNull(AttestationTrustDiagnostic.resolveCode(""));
        assertNull(AttestationTrustDiagnostic.resolveCode(null));
    }

    @Test
    void isDiagnosticCode_recognisesOnlyPrefixedCodes() {
        assertTrue(AttestationTrustDiagnostic.isDiagnosticCode("JFS_AAGUID_NOT_IN_MDS"));
        assertFalse(AttestationTrustDiagnostic.isDiagnosticCode("Problem with certificate"));
        assertFalse(AttestationTrustDiagnostic.isDiagnosticCode(null));
    }

    @Test
    void everyCodeUsesTheSharedPrefix() {
        // The category lookup in MetricService keys off this prefix.
        for (AttestationTrustDiagnostic diagnostic : AttestationTrustDiagnostic.values()) {
            assertTrue(diagnostic.name().startsWith(AttestationTrustDiagnostic.CODE_PREFIX),
                    diagnostic.name() + " must start with " + AttestationTrustDiagnostic.CODE_PREFIX);
            assertFalse(diagnostic.getMessageMarkers().isEmpty(),
                    diagnostic.name() + " must declare at least one message marker");
        }
    }
}
