package io.jans.as.server.service.net;

import io.jans.as.model.configuration.AppConfiguration;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class UriServiceTest {

    @InjectMocks
    private UriService uriService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void isExternalUriWhitelisted_whenExternalUriWhiteListIsBlank_shouldReturnTrue() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(new ArrayList<>());

        assertTrue(uriService.isExternalUriWhitelisted("http://example.com"));
    }

    @Test
    public void isExternalUriWhitelisted_whenExternalUriWhiteListIsNull_shouldReturnTrue() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(null);

        assertTrue(uriService.isExternalUriWhitelisted("http://example.com"));
    }

    @Test
    public void isExternalUriWhitelisted_whenUriAllowedByExternalUriWhiteList_shouldReturnTrue() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("example.com"));

        assertTrue(uriService.isExternalUriWhitelisted("http://example.com"));
        assertTrue(uriService.isExternalUriWhitelisted("https://example.com"));
        assertTrue(uriService.isExternalUriWhitelisted("http://example.com/path"));
        assertTrue(uriService.isExternalUriWhitelisted("https://example.com/path"));
    }

    @Test
    public void isExternalUriWhitelisted_whenUriNotAllowedByExternalUriWhiteList_shouldReturnFalse() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("my.com"));

        assertFalse(uriService.isExternalUriWhitelisted("http://example.com"));
    }

    @Test
    public void isExternalUriWhitelisted_whenUriIsNull_shouldReturnFalse() {
        assertFalse(uriService.isExternalUriWhitelisted(null));
    }

    @Test
    public void isExternalUriWhitelisted_whenUriIsBlank_shouldReturnFalse() {
        assertFalse(uriService.isExternalUriWhitelisted("   "));
    }

    @Test
    public void isExternalUriWhitelisted_whenWildcardSubdomainPattern_shouldAllowBareDomainAndAnySubdomain() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("*.example.com/*"));

        assertTrue(uriService.isExternalUriWhitelisted("http://example.com"));
        assertTrue(uriService.isExternalUriWhitelisted("https://example.com/foo"));
        assertTrue(uriService.isExternalUriWhitelisted("https://www.example.com/foo"));
        assertTrue(uriService.isExternalUriWhitelisted("https://a.b.example.com/foo"));
    }

    @Test
    public void isExternalUriWhitelisted_whenWildcardSubdomainPattern_shouldRejectUnrelatedOrSpoofedDomain() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("*.example.com/*"));

        assertFalse(uriService.isExternalUriWhitelisted("http://notexample.com"));
        assertFalse(uriService.isExternalUriWhitelisted("http://example.com.attacker.com"));
        assertFalse(uriService.isExternalUriWhitelisted("http://attacker.com/example.com"));
    }

    @Test
    public void isExternalUriWhitelisted_whenPatternHasNoScheme_shouldAllowOnlyHttpAndHttps() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("example.com"));

        assertTrue(uriService.isExternalUriWhitelisted("http://example.com"));
        assertTrue(uriService.isExternalUriWhitelisted("https://example.com"));
        assertFalse(uriService.isExternalUriWhitelisted("ftp://example.com"));
    }

    @Test
    public void isExternalUriWhitelisted_whenPatternHasWildcardScheme_shouldAllowAnyScheme() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("*://example.com/*"));

        assertTrue(uriService.isExternalUriWhitelisted("ftp://example.com/file"));
        assertTrue(uriService.isExternalUriWhitelisted("ws://example.com/socket"));
    }

    @Test
    public void isExternalUriWhitelisted_whenPatternHasNoPath_shouldAllowAnyPath() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("https://example.com"));

        assertTrue(uriService.isExternalUriWhitelisted("https://example.com/secret/admin"));
    }

    @Test
    public void isExternalUriWhitelisted_whenPatternPathIsTrailingWildcard_shouldAllowAnyPath() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("https://example.com/*"));

        assertTrue(uriService.isExternalUriWhitelisted("https://example.com"));
        assertTrue(uriService.isExternalUriWhitelisted("https://example.com/anything/nested/path"));
    }

    @Test
    public void isExternalUriWhitelisted_whenPatternPathWildcardIsMidSegment_shouldMatchAcrossMultipleSegments() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("https://example.com/api/*"));

        assertTrue(uriService.isExternalUriWhitelisted("https://example.com/api/v1/users/123"));
        assertFalse(uriService.isExternalUriWhitelisted("https://example.com/other/path"));
    }

    @Test
    public void isExternalUriWhitelisted_whenPathDoesNotMatchPattern_shouldReturnFalse() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("https://example.com/allowed"));

        assertTrue(uriService.isExternalUriWhitelisted("https://example.com/allowed"));
        assertFalse(uriService.isExternalUriWhitelisted("https://example.com/other"));
    }

    @Test
    public void isExternalUriWhitelisted_pathMatchingIsCaseSensitive() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("https://example.com/API/*"));

        assertTrue(uriService.isExternalUriWhitelisted("https://example.com/API/keys"));
        assertFalse(uriService.isExternalUriWhitelisted("https://example.com/api/keys"));
    }

    @Test
    public void isExternalUriWhitelisted_hostAndSchemeMatchingIsCaseInsensitive() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("https://Example.COM/*"));

        assertTrue(uriService.isExternalUriWhitelisted("https://example.com/x"));
        assertTrue(uriService.isExternalUriWhitelisted("HTTPS://EXAMPLE.COM/X"));
    }

    @Test
    public void isExternalUriWhitelisted_whenPatternHasExplicitPort_shouldRequireExactPortMatch() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("https://example.com:8443/*"));

        assertTrue(uriService.isExternalUriWhitelisted("https://example.com:8443/x"));
        assertFalse(uriService.isExternalUriWhitelisted("https://example.com:9999/x"));
        assertFalse(uriService.isExternalUriWhitelisted("https://example.com/x"));
    }

    @Test
    public void isExternalUriWhitelisted_whenPatternHasNoPort_shouldAllowAnyPort() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("https://example.com/*"));

        assertTrue(uriService.isExternalUriWhitelisted("https://example.com/x"));
        assertTrue(uriService.isExternalUriWhitelisted("https://example.com:1234/x"));
    }

    @Test
    public void isExternalUriWhitelisted_whenMultipleWhitelistEntries_shouldAllowIfAnyEntryMatches() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Arrays.asList("my.com/*", "example.com/*"));

        assertTrue(uriService.isExternalUriWhitelisted("https://my.com/x"));
        assertTrue(uriService.isExternalUriWhitelisted("https://example.com/x"));
        assertFalse(uriService.isExternalUriWhitelisted("https://other.com/x"));
    }

    @Test
    public void isExternalUriWhitelisted_whenUriHasNoScheme_shouldReturnFalse() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("example.com/*"));

        assertFalse(uriService.isExternalUriWhitelisted("example.com/x"));
    }

    @Test
    public void isExplicitlyWhitelisted_whenExternalUriWhiteListIsEmpty_shouldReturnFalse() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(new ArrayList<>());

        assertFalse(uriService.isExplicitlyWhitelisted("https://example.com"));
    }

    @Test
    public void isExplicitlyWhitelisted_whenExternalUriWhiteListIsNull_shouldReturnFalse() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(null);

        assertFalse(uriService.isExplicitlyWhitelisted("https://example.com"));
    }

    @Test
    public void isExplicitlyWhitelisted_whenUriMatchesEntry_shouldReturnTrue() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("jenkins-build.jans.io/*"));

        assertTrue(uriService.isExplicitlyWhitelisted("https://jenkins-build.jans.io/jans-auth/sectoridentifier/x"));
    }

    @Test
    public void isExplicitlyWhitelisted_whenUriDoesNotMatchAnyEntry_shouldReturnFalse() {
        when(appConfiguration.getExternalUriWhiteList()).thenReturn(Collections.singletonList("jenkins-build.jans.io/*"));

        assertFalse(uriService.isExplicitlyWhitelisted("https://attacker.example/x"));
    }

    @Test
    public void isExplicitlyWhitelisted_whenUriIsBlank_shouldReturnFalse() {
        assertFalse(uriService.isExplicitlyWhitelisted("   "));
    }
}
