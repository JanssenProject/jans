/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.model.util;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
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
    public void isValidPresentedSpiffeId_withExplicitPort_returnsFalse() {
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://example.org:8080/my-workload"));
    }

    @Test
    public void isValidPresentedSpiffeId_withUppercaseTrustDomain_returnsFalse() {
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://Example.org/my-workload"));
    }

    @Test
    public void isValidPresentedSpiffeId_withUnderscoreInTrustDomain_returnsTrue() {
        assertTrue(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://exa_mple.org/my-workload"));
    }

    @Test
    public void isValidPresentedSpiffeId_withDisallowedTrustDomainCharacter_returnsFalse() {
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://exam!ple.org/my-workload"));
    }

    @Test
    public void isValidPresentedSpiffeId_withTrustDomainOver255Bytes_returnsFalse() {
        String tooLong = StringUtils.repeat('a', 256) + ".org";
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://" + tooLong + "/my-workload"));
    }

    @Test
    public void isValidPresentedSpiffeId_withTrustDomainAt255Bytes_returnsTrue() {
        String maxLength = StringUtils.repeat('a', 251) + ".org";
        assertTrue(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://" + maxLength + "/my-workload"));
    }

    @Test
    public void isValidPresentedSpiffeId_withTrailingSlash_returnsFalse() {
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://example.org/my-workload/"));
    }

    @Test
    public void isValidPresentedSpiffeId_withEmptyMiddleSegment_returnsFalse() {
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://example.org/foo//bar"));
    }

    @Test
    public void isValidPresentedSpiffeId_withPercentEncodedPathSegment_returnsFalse() {
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://example.org/%2e%2e/my-workload"));
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://example.org/foo%2Fbar"));
    }

    @Test
    public void isValidPresentedSpiffeId_withDisallowedPathCharacter_returnsFalse() {
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://example.org/foo bar"));
        assertFalse(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://example.org/foo!bar"));
    }

    @Test
    public void isValidPresentedSpiffeId_withUppercasePathSegment_returnsTrue() {
        // unlike the trust domain, path segment characters may be mixed-case per the SPIFFE spec.
        assertTrue(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://example.org/My-Workload"));
    }

    @Test
    public void isValidPresentedSpiffeId_withBareTrustDomainNoPath_returnsTrue() {
        assertTrue(SpiffeIdUtil.isValidPresentedSpiffeId("spiffe://example.org"));
    }

    @Test
    public void isValidRegisteredSpiffeId_withEmbeddedAsteriskNotAtEnd_returnsFalse() {
        assertFalse(SpiffeIdUtil.isValidRegisteredSpiffeId("spiffe://example.org/*/client"));
        assertFalse(SpiffeIdUtil.isValidRegisteredSpiffeId("spiffe://example.org/foo*bar"));
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
