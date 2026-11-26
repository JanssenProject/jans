/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.model.attestation;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public class AttestationStatusTest {

    @Test
    public void min_isNeverBetterThanEitherInput() {
        assertEquals(AttestationStatus.min(AttestationStatus.AFFIRMING, AttestationStatus.AFFIRMING), AttestationStatus.AFFIRMING);
        assertEquals(AttestationStatus.min(AttestationStatus.AFFIRMING, AttestationStatus.WARNING), AttestationStatus.WARNING);
        assertEquals(AttestationStatus.min(AttestationStatus.WARNING, AttestationStatus.AFFIRMING), AttestationStatus.WARNING);
        assertEquals(AttestationStatus.min(AttestationStatus.AFFIRMING, AttestationStatus.CONTRAINDICATED), AttestationStatus.CONTRAINDICATED);
        assertEquals(AttestationStatus.min(AttestationStatus.CONTRAINDICATED, AttestationStatus.WARNING), AttestationStatus.CONTRAINDICATED);
    }

    @Test
    public void min_withNoVerdictOrNull_isNone() {
        assertEquals(AttestationStatus.min(AttestationStatus.AFFIRMING, AttestationStatus.NONE), AttestationStatus.NONE);
        assertEquals(AttestationStatus.min(AttestationStatus.NONE, AttestationStatus.CONTRAINDICATED), AttestationStatus.NONE);
        assertEquals(AttestationStatus.min(null, AttestationStatus.AFFIRMING), AttestationStatus.NONE);
    }

    @Test
    public void fromString_isCaseInsensitiveAndUnknownIsNull() {
        assertEquals(AttestationStatus.fromString("Affirming"), AttestationStatus.AFFIRMING);
        assertEquals(AttestationStatus.fromString("contraindicated"), AttestationStatus.CONTRAINDICATED);
        assertNull(AttestationStatus.fromString("approved"));
        assertNull(AttestationStatus.fromString(null));
    }

    @Test
    public void isVerdict_onlyNoneIsNotAVerdict() {
        assertTrue(AttestationStatus.AFFIRMING.isVerdict());
        assertTrue(AttestationStatus.CONTRAINDICATED.isVerdict());
        assertFalse(AttestationStatus.NONE.isVerdict());
    }
}
