/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.trust;

import io.jans.fido2.exception.Fido2NativeFailureException;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.trust.NativeFailureDiagnostic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeFailureDiagnosticsTest {

    @Test
    void resolveCode_ifNativeFailureThrownDirectly_returnsItsCode() {
        Fido2NativeFailureException error = new Fido2NativeFailureException(
                NativeFailureDiagnostic.JFS_RPID_HASH_MISMATCH, "Hashes don't match");

        assertEquals("JFS_RPID_HASH_MISMATCH", NativeFailureDiagnostics.resolveCode(error));
    }

    @Test
    void resolveCode_ifNestedDeeper_stillFindsIt() {
        Fido2NativeFailureException failure = new Fido2NativeFailureException(
                NativeFailureDiagnostic.JFS_RPID_HASH_MISMATCH, "Hashes don't match");
        Throwable wrapped = new IllegalStateException("outer", new RuntimeException("middle", failure));

        assertEquals("JFS_RPID_HASH_MISMATCH", NativeFailureDiagnostics.resolveCode(wrapped));
    }

    /**
     * A non-native-failure resolves to null, so the caller keeps the original message.
     * Classification must never swallow the detail of failures it does not understand.
     */
    @Test
    void resolveCode_ifNotNativeFailureRelated_returnsNull() {
        assertNull(NativeFailureDiagnostics.resolveCode(new Fido2RuntimeException("Challenge mismatch")));
        assertNull(NativeFailureDiagnostics.resolveCode(new IllegalArgumentException("bad input")));
        assertNull(NativeFailureDiagnostics.resolveCode(null));
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

        assertNull(NativeFailureDiagnostics.resolveCode(first));
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

        assertNull(NativeFailureDiagnostics.resolveCode(selfReferential));
    }

    @Test
    void resolve_returnsTheEnumConstant() {
        Fido2NativeFailureException error = new Fido2NativeFailureException(
                NativeFailureDiagnostic.JFS_RPID_HASH_MISMATCH, "Hashes don't match");

        assertEquals(NativeFailureDiagnostic.JFS_RPID_HASH_MISMATCH, NativeFailureDiagnostics.resolve(error));
    }

    /**
     * Every code shares the prefix, so the metrics layer can tell a deliberately recorded code from a
     * free-text message without enumerating the values.
     */
    @Test
    void isDiagnosticCode_recognisesEveryCodeAndRejectsMessages() {
        for (NativeFailureDiagnostic diagnostic : NativeFailureDiagnostic.values()) {
            assertTrue(NativeFailureDiagnostic.isDiagnosticCode(diagnostic.name()),
                    diagnostic + " must be recognised as a diagnostic code");
        }

        assertFalse(NativeFailureDiagnostic.isDiagnosticCode("Hashes don't match"));
        assertFalse(NativeFailureDiagnostic.isDiagnosticCode(null));
    }

    /**
     * The attestation-trust work writes its own JFS_ codes into the same errorReason field. Matching
     * on the shared prefix would file those under NATIVE_FAILURE and inflate this category's
     * analytics with rejections that have nothing to do with a native-client failure mode.
     */
    @Test
    void isDiagnosticCode_rejectsForeignCodesSharingThePrefix() {
        assertFalse(NativeFailureDiagnostic.isDiagnosticCode("JFS_AAGUID_NOT_IN_MDS"));
        assertFalse(NativeFailureDiagnostic.isDiagnosticCode(NativeFailureDiagnostic.CODE_PREFIX));
        assertFalse(NativeFailureDiagnostic.isDiagnosticCode("JFS_RPID_HASH_MISMATCH_EXTRA"));
    }
}
