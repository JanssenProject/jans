/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.SpiffeTrustDomainConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.security.cert.TrustAnchor;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for SpiffeBundleService.
 *
 * @author Yuriy Zabrovarnyy
 */
@Listeners(MockitoTestNGListener.class)
public class SpiffeBundleServiceTest {

    // Raw base64 DER certificate (no PEM markers), exactly the shape of a JWK x5c entry.
    // Reused verbatim from CertUtilsTest.TEST_PEM_3 - known to parse successfully.
    private static final String CERT_BASE64 = "MIIBBjCBrAIBAjAKBggqhkjOPQQDAjAPMQ0wCwYDVQQDDARtdGxzMB4XDTE4MTAxODEyMzcwOVoXDTIyMDUwMjEyMzcwOVowDzENMAsGA1UEAwwEbXRsczBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABNcnyxwqV6hY8QnhxxzFQ03C7HKW9OylMbnQZjjJ/Au08/coZwxS7LfA4vOLS9WuneIXhbGGWvsDSb0tH6IxLm8wCgYIKoZIzj0EAwIDSQAwRgIhAP0RC1E+vwJD/D1AGHGzuri+hlV/PpQEKTWUVeORWz83AiEA5x2eXZOVbUlJSGQgjwD5vaUaKlLR50Q2DmFfQj1L+SY=";

    private static final String TRUST_DOMAIN = "example.org";
    private static final String BUNDLE_ENDPOINT_URL = "https://bundle.example.org/keys.json";

    @InjectMocks
    @Spy
    private SpiffeBundleService spiffeBundleService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void getX509TrustAnchors_blankTrustDomain_shouldReturnEmptySetWithoutFetching() {
        Set<TrustAnchor> result = spiffeBundleService.getX509TrustAnchors("");

        assertTrue(result.isEmpty());
        verify(spiffeBundleService, never()).fetchBundle(anyString());
    }

    @Test
    public void getX509TrustAnchors_nullTrustDomain_shouldReturnEmptySetWithoutFetching() {
        Set<TrustAnchor> result = spiffeBundleService.getX509TrustAnchors(null);

        assertTrue(result.isEmpty());
        verify(spiffeBundleService, never()).fetchBundle(anyString());
    }

    @Test
    public void getX509TrustAnchors_noConfigForTrustDomain_shouldReturnEmptySet() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor("other.org", BUNDLE_ENDPOINT_URL)));

        Set<TrustAnchor> result = spiffeBundleService.getX509TrustAnchors(TRUST_DOMAIN);

        assertTrue(result.isEmpty());
        verify(spiffeBundleService, never()).fetchBundle(anyString());
    }

    @Test
    public void getX509TrustAnchors_configWithBlankBundleEndpointUrl_shouldReturnEmptySet() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor(TRUST_DOMAIN, "")));

        Set<TrustAnchor> result = spiffeBundleService.getX509TrustAnchors(TRUST_DOMAIN);

        assertTrue(result.isEmpty());
        verify(spiffeBundleService, never()).fetchBundle(anyString());
    }

    @Test
    public void getX509TrustAnchors_caseInsensitiveTrustDomainMatch_shouldFindConfig() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor("Example.ORG", BUNDLE_ENDPOINT_URL)));
        doReturn(bundleWithKeys(x509SvidKey(CERT_BASE64))).when(spiffeBundleService).fetchBundle(BUNDLE_ENDPOINT_URL);

        Set<TrustAnchor> result = spiffeBundleService.getX509TrustAnchors(TRUST_DOMAIN);

        assertEquals(result.size(), 1);
    }

    @Test
    public void getX509TrustAnchors_bundleWithNoKeysArray_shouldReturnEmptySet() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor(TRUST_DOMAIN, BUNDLE_ENDPOINT_URL)));
        doReturn(new JSONObject()).when(spiffeBundleService).fetchBundle(BUNDLE_ENDPOINT_URL);

        Set<TrustAnchor> result = spiffeBundleService.getX509TrustAnchors(TRUST_DOMAIN);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getX509TrustAnchors_filtersOutNonX509SvidKeys() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor(TRUST_DOMAIN, BUNDLE_ENDPOINT_URL)));
        doReturn(bundleWithKeys(jwtSvidKey("kid1"), x509SvidKey(CERT_BASE64))).when(spiffeBundleService).fetchBundle(BUNDLE_ENDPOINT_URL);

        Set<TrustAnchor> result = spiffeBundleService.getX509TrustAnchors(TRUST_DOMAIN);

        assertEquals(result.size(), 1);
    }

    @Test
    public void getX509TrustAnchors_keyWithoutX5c_shouldBeSkipped() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor(TRUST_DOMAIN, BUNDLE_ENDPOINT_URL)));
        doReturn(bundleWithKeys(x509SvidKey(null))).when(spiffeBundleService).fetchBundle(BUNDLE_ENDPOINT_URL);

        Set<TrustAnchor> result = spiffeBundleService.getX509TrustAnchors(TRUST_DOMAIN);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getX509TrustAnchors_multipleX509SvidKeys_shouldReturnAllAsAnchors() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor(TRUST_DOMAIN, BUNDLE_ENDPOINT_URL)));
        doReturn(bundleWithKeys(x509SvidKey(CERT_BASE64), x509SvidKey(CERT_BASE64))).when(spiffeBundleService).fetchBundle(BUNDLE_ENDPOINT_URL);

        Set<TrustAnchor> result = spiffeBundleService.getX509TrustAnchors(TRUST_DOMAIN);

        // TrustAnchor has no value-based equals/hashCode, so both entries are added as distinct
        // Set elements even though they wrap an identical certificate - this proves both x509-svid
        // keys in the bundle were processed rather than the loop stopping after the first.
        assertEquals(result.size(), 2);
    }

    @Test
    public void getX509TrustAnchors_fetchFailsWithNoCache_shouldFailClosedToEmptySet() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor(TRUST_DOMAIN, BUNDLE_ENDPOINT_URL)));
        doThrow(new RuntimeException("connection refused")).when(spiffeBundleService).fetchBundle(BUNDLE_ENDPOINT_URL);

        Set<TrustAnchor> result = spiffeBundleService.getX509TrustAnchors(TRUST_DOMAIN);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getX509TrustAnchors_secondCallWithinTtl_shouldNotRefetch() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor(TRUST_DOMAIN, BUNDLE_ENDPOINT_URL)));
        doReturn(bundleWithKeys(x509SvidKey(CERT_BASE64))).when(spiffeBundleService).fetchBundle(BUNDLE_ENDPOINT_URL);

        Set<TrustAnchor> firstResult = spiffeBundleService.getX509TrustAnchors(TRUST_DOMAIN);
        assertEquals(firstResult.size(), 1);

        Set<TrustAnchor> secondResult = spiffeBundleService.getX509TrustAnchors(TRUST_DOMAIN);
        assertEquals(secondResult.size(), 1);
        verify(spiffeBundleService, times(1)).fetchBundle(BUNDLE_ENDPOINT_URL);
    }

    @Test
    public void getX509TrustAnchors_fetchFailsAfterCacheExpired_shouldServeStaleCache() throws Exception {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor(TRUST_DOMAIN, BUNDLE_ENDPOINT_URL)));
        doReturn(bundleWithKeys(x509SvidKey(CERT_BASE64))).when(spiffeBundleService).fetchBundle(BUNDLE_ENDPOINT_URL);

        Set<TrustAnchor> firstResult = spiffeBundleService.getX509TrustAnchors(TRUST_DOMAIN);
        assertEquals(firstResult.size(), 1);

        forceCacheEntryExpired(TRUST_DOMAIN);
        doThrow(new RuntimeException("bundle endpoint unreachable")).when(spiffeBundleService).fetchBundle(BUNDLE_ENDPOINT_URL);

        Set<TrustAnchor> secondResult = spiffeBundleService.getX509TrustAnchors(TRUST_DOMAIN);

        // fetch was retried (cache had expired) but failed, so the stale-but-last-known-good
        // bundle is served rather than failing closed to an empty set.
        assertEquals(secondResult.size(), 1);
        verify(spiffeBundleService, times(2)).fetchBundle(BUNDLE_ENDPOINT_URL);
    }

    @Test
    public void getJwtSvidJwks_noConfigForTrustDomain_shouldReturnNull() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.emptyList());

        JSONObject result = spiffeBundleService.getJwtSvidJwks(TRUST_DOMAIN);

        assertNull(result);
    }

    @Test
    public void getJwtSvidJwks_bundleWithOnlyX509SvidKeys_shouldReturnNull() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor(TRUST_DOMAIN, BUNDLE_ENDPOINT_URL)));
        doReturn(bundleWithKeys(x509SvidKey(CERT_BASE64))).when(spiffeBundleService).fetchBundle(BUNDLE_ENDPOINT_URL);

        JSONObject result = spiffeBundleService.getJwtSvidJwks(TRUST_DOMAIN);

        assertNull(result);
    }

    @Test
    public void getJwtSvidJwks_filtersToJwtSvidKeysOnly() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor(TRUST_DOMAIN, BUNDLE_ENDPOINT_URL)));
        doReturn(bundleWithKeys(x509SvidKey(CERT_BASE64), jwtSvidKey("kid1"), jwtSvidKey("kid2")))
                .when(spiffeBundleService).fetchBundle(BUNDLE_ENDPOINT_URL);

        JSONObject result = spiffeBundleService.getJwtSvidJwks(TRUST_DOMAIN);

        assertEquals(result.getJSONArray("keys").length(), 2);
        assertEquals(result.getJSONArray("keys").getJSONObject(0).getString("kid"), "kid1");
        assertEquals(result.getJSONArray("keys").getJSONObject(1).getString("kid"), "kid2");
    }

    @Test
    public void getJwtSvidJwks_fetchFailsWithNoCache_shouldReturnNull() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor(TRUST_DOMAIN, BUNDLE_ENDPOINT_URL)));
        doThrow(new RuntimeException("timeout")).when(spiffeBundleService).fetchBundle(BUNDLE_ENDPOINT_URL);

        JSONObject result = spiffeBundleService.getJwtSvidJwks(TRUST_DOMAIN);

        assertNull(result);
    }

    @Test
    public void getX509TrustAnchorsAndGetJwtSvidJwks_shareOneCachedFetch() {
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Collections.singletonList(configFor(TRUST_DOMAIN, BUNDLE_ENDPOINT_URL)));
        doReturn(bundleWithKeys(x509SvidKey(CERT_BASE64), jwtSvidKey("kid1"))).when(spiffeBundleService).fetchBundle(BUNDLE_ENDPOINT_URL);

        Set<TrustAnchor> anchors = spiffeBundleService.getX509TrustAnchors(TRUST_DOMAIN);
        JSONObject jwks = spiffeBundleService.getJwtSvidJwks(TRUST_DOMAIN);

        assertEquals(anchors.size(), 1);
        assertEquals(jwks.getJSONArray("keys").length(), 1);
        verify(spiffeBundleService, times(1)).fetchBundle(BUNDLE_ENDPOINT_URL);
    }

    @Test
    public void differentTrustDomains_shouldBeFetchedAndCachedIndependently() {
        SpiffeTrustDomainConfiguration configA = configFor("a.org", "https://bundle.a.org/keys.json");
        SpiffeTrustDomainConfiguration configB = configFor("b.org", "https://bundle.b.org/keys.json");
        when(appConfiguration.getSpiffeTrustDomains()).thenReturn(Arrays.asList(configA, configB));
        doReturn(bundleWithKeys(x509SvidKey(CERT_BASE64))).when(spiffeBundleService).fetchBundle("https://bundle.a.org/keys.json");
        doReturn(bundleWithKeys()).when(spiffeBundleService).fetchBundle("https://bundle.b.org/keys.json");

        Set<TrustAnchor> anchorsA = spiffeBundleService.getX509TrustAnchors("a.org");
        Set<TrustAnchor> anchorsB = spiffeBundleService.getX509TrustAnchors("b.org");

        assertEquals(anchorsA.size(), 1);
        assertTrue(anchorsB.isEmpty());
        verify(spiffeBundleService, times(1)).fetchBundle(eq("https://bundle.a.org/keys.json"));
        verify(spiffeBundleService, times(1)).fetchBundle(eq("https://bundle.b.org/keys.json"));
    }

    private SpiffeTrustDomainConfiguration configFor(String trustDomain, String url) {
        return new SpiffeTrustDomainConfiguration(trustDomain, url, 60);
    }

    private JSONObject bundleWithKeys(JSONObject... keys) {
        JSONArray array = new JSONArray();
        for (JSONObject key : keys) {
            array.put(key);
        }
        return new JSONObject().put("keys", array);
    }

    private JSONObject x509SvidKey(String x5cBase64) {
        JSONObject key = new JSONObject().put("use", "x509-svid");
        if (x5cBase64 != null) {
            key.put("x5c", new JSONArray().put(x5cBase64));
        }
        return key;
    }

    private JSONObject jwtSvidKey(String kid) {
        return new JSONObject().put("use", "jwt-svid").put("kid", kid).put("kty", "RSA");
    }

    /**
     * Reflectively back-dates the cached entry's expiry so the next call is forced to attempt a
     * re-fetch, without sleeping past the real (minutes-scale) cache TTL.
     */
    private void forceCacheEntryExpired(String trustDomain) throws Exception {
        java.lang.reflect.Field cacheField = SpiffeBundleService.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        java.util.concurrent.ConcurrentHashMap<?, ?> cache = (java.util.concurrent.ConcurrentHashMap<?, ?>) cacheField.get(spiffeBundleService);
        Object cacheEntry = cache.get(trustDomain);
        java.lang.reflect.Field expiresAtField = cacheEntry.getClass().getDeclaredField("expiresAtMillis");
        expiresAtField.setAccessible(true);
        expiresAtField.set(cacheEntry, 0L);
    }
}
