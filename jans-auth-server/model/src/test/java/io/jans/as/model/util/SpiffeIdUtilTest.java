/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.model.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
public class SpiffeIdUtilTest {

    @Test
    public void isValidPresentedSpiffeId_withValidId_returnsTrue() {
        assertTrue(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://example.org/my-workload"));
    }

    @Test
    public void isValidPresentedSpiffeId_withWildcard_returnsFalse() {
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://example.org/client/*"));
    }

    @Test
    public void isValidPresentedSpiffeId_withWrongScheme_returnsFalse() {
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("https://example.org/my-workload"));
    }

    @Test
    public void isValidPresentedSpiffeId_withNoTrustDomain_returnsFalse() {
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe:///my-workload"));
    }

    @Test
    public void isValidPresentedSpiffeId_withDotDotSegment_returnsFalse() {
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://example.org/../my-workload"));
    }

    @Test
    public void isValidPresentedSpiffeId_withBlank_returnsFalse() {
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId(""));
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId(null));
    }

    @Test
    public void isValidRegisteredSpiffeId_withWildcard_returnsTrue() {
        assertTrue(SpiffeIdUtil.isValidRegisteredSpiffeId("spiffe://example.org/client/*"));
    }

    @Test
    public void isValidRegisteredSpiffeId_withBareWildcard_matchesEntireTrustDomain() {
        // "spiffe://example.org/*" is well-formed: an empty prefix matches any path under the trust domain.
        assertTrue(SpiffeIdUtil.isValidRegisteredSpiffeId("spiffe://example.org/*"));
        assertTrue(SpiffeIdUtil.matches("spiffe://example.org/*", "spiffe://example.org/anything"));
    }

    @Test
    public void trustDomainOf_withExactId_returnsTrustDomain() {
        assertEquals(SpiffeIdUtil.trustDomainOf("spiffe://Example.ORG/my-workload"), "example.org");
    }

    @Test
    public void trustDomainOf_withWildcardId_returnsTrustDomain() {
        assertEquals(SpiffeIdUtil.trustDomainOf("spiffe://example.org/client/*"), "example.org");
    }

    @Test
    public void trustDomainOf_withInvalidId_returnsNull() {
        assertNull(SpiffeIdUtil.trustDomainOf("not-a-spiffe-id"));
        assertNull(SpiffeIdUtil.trustDomainOf(""));
    }

    @Test
    public void matches_withExactRegisteredId_requiresExactMatch() {
        assertTrue(SpiffeIdUtil.matches("spiffe://example.org/client", "spiffe://example.org/client"));
        assertFalse(SpiffeIdUtil.matches("spiffe://example.org/client", "spiffe://example.org/client/123"));
    }

    @Test
    public void matches_withWildcardRegisteredId_matchesPathSegmentPrefix() {
        assertTrue(SpiffeIdUtil.matches("spiffe://example.org/client/*", "spiffe://example.org/client/123"));
        assertFalse(SpiffeIdUtil.matches("spiffe://example.org/client/*", "spiffe://example.org/client123"));
    }

    @Test
    public void matches_withWildcardRegisteredId_requiresSameTrustDomain() {
        assertFalse(SpiffeIdUtil.matches("spiffe://example.org/client/*", "spiffe://evil.org/client/123"));
    }

    @Test
    public void matches_withBlankValues_returnsFalse() {
        assertFalse(SpiffeIdUtil.matches(null, "spiffe://example.org/client"));
        assertFalse(SpiffeIdUtil.matches("spiffe://example.org/client", null));
    }

    @Test
    public void isWildcard_detectsTrailingWildcardSuffix() {
        assertTrue(SpiffeIdUtil.isWildcard("spiffe://example.org/client/*"));
        assertFalse(SpiffeIdUtil.isWildcard("spiffe://example.org/client"));
        assertFalse(SpiffeIdUtil.isWildcard(null));
    }
}
