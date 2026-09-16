package io.jans.as.server.service;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.external.ExternalCookieService;
import jakarta.servlet.http.HttpServletResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class CookieServiceTest {

    @InjectMocks
    private CookieService cookieService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private Logger log;

    @Mock
    private ExternalCookieService externalCookieService;

    @Mock
    private HttpServletResponse httpResponse;

    @Test
    public void createCookie_whenSameSiteIsNull_shouldDefaultToNone() {
        when(appConfiguration.getCookieSameSite()).thenReturn(null);
        stubModifyCookieHeaderPassThrough();

        cookieService.createCookie("foo", "foo=bar", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=None");
    }

    @Test
    public void createCookie_whenSameSiteIsBlank_shouldDefaultToNone() {
        when(appConfiguration.getCookieSameSite()).thenReturn(" ");
        stubModifyCookieHeaderPassThrough();

        cookieService.createCookie("foo", "foo=bar", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=None");
    }

    @Test
    public void createCookie_whenSameSiteIsNone_shouldUseNone() {
        when(appConfiguration.getCookieSameSite()).thenReturn("None");
        stubModifyCookieHeaderPassThrough();

        cookieService.createCookie("foo", "foo=bar", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=None");
    }

    @Test
    public void createCookie_whenSameSiteIsLax_shouldUseLax() {
        when(appConfiguration.getCookieSameSite()).thenReturn("Lax");
        stubModifyCookieHeaderPassThrough();

        cookieService.createCookie("foo", "foo=bar", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=Lax");
    }

    @Test
    public void createCookie_whenSameSiteIsStrict_shouldUseStrict() {
        when(appConfiguration.getCookieSameSite()).thenReturn("Strict");
        stubModifyCookieHeaderPassThrough();

        cookieService.createCookie("foo", "foo=bar", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=Strict");
    }

    @Test
    public void createCookie_whenSameSiteIsLowercaseLax_shouldNormalizeToCanonicalLax() {
        when(appConfiguration.getCookieSameSite()).thenReturn("lax");
        stubModifyCookieHeaderPassThrough();

        cookieService.createCookie("foo", "foo=bar", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=Lax");
    }

    @Test
    public void createCookie_whenSameSiteIsUppercaseStrict_shouldNormalizeToCanonicalStrict() {
        when(appConfiguration.getCookieSameSite()).thenReturn("STRICT");
        stubModifyCookieHeaderPassThrough();

        cookieService.createCookie("foo", "foo=bar", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=Strict");
    }

    @Test
    public void createCookie_whenSameSiteIsInvalid_shouldFallBackToNone() {
        when(appConfiguration.getCookieSameSite()).thenReturn("invalid-value");
        stubModifyCookieHeaderPassThrough();

        cookieService.createCookie("foo", "foo=bar", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=None");
    }

    @Test
    public void createCookie_whenSameInvalidValueUsedRepeatedly_shouldLogItOnlyOnce() {
        // LOGGED_INVALID_SAME_SITE_VALUES is static, so use a value unique to this test
        // to avoid interference from other tests exercising the same invalid value.
        String invalidValue = "repeated-invalid-value";
        when(appConfiguration.getCookieSameSite()).thenReturn(invalidValue);
        stubModifyCookieHeaderPassThrough();

        cookieService.createCookie("foo", "foo=bar", httpResponse);
        cookieService.createCookie("foo", "foo=bar", httpResponse);

        verify(log, times(1)).error(anyString(), eq(invalidValue));
    }

    @Test
    public void createCookie_whenNewDistinctInvalidValue_shouldStillLogIt() {
        String invalidValue = "distinct-invalid-value";
        when(appConfiguration.getCookieSameSite()).thenReturn(invalidValue);
        stubModifyCookieHeaderPassThrough();

        cookieService.createCookie("foo", "foo=bar", httpResponse);

        verify(log, times(1)).error(anyString(), eq(invalidValue));
    }

    @Test
    public void createOPBrowserStateCookie_whenSameSiteIsNull_shouldDefaultToNone() {
        when(appConfiguration.getCookieSameSite()).thenReturn(null);

        cookieService.createOPBrowserStateCookie("opbsValue", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=None");
    }

    @Test
    public void createOPBrowserStateCookie_whenSameSiteIsBlank_shouldDefaultToNone() {
        when(appConfiguration.getCookieSameSite()).thenReturn("");

        cookieService.createOPBrowserStateCookie("opbsValue", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=None");
    }

    @Test
    public void createOPBrowserStateCookie_whenSameSiteIsNone_shouldUseNone() {
        when(appConfiguration.getCookieSameSite()).thenReturn("None");

        cookieService.createOPBrowserStateCookie("opbsValue", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=None");
    }

    @Test
    public void createOPBrowserStateCookie_whenSameSiteIsLax_shouldUseLax() {
        when(appConfiguration.getCookieSameSite()).thenReturn("Lax");

        cookieService.createOPBrowserStateCookie("opbsValue", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=Lax");
    }

    @Test
    public void createOPBrowserStateCookie_whenSameSiteIsStrict_shouldUseStrict() {
        when(appConfiguration.getCookieSameSite()).thenReturn("Strict");

        cookieService.createOPBrowserStateCookie("opbsValue", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=Strict");
    }

    @Test
    public void createOPBrowserStateCookie_whenSameSiteIsLowercaseNone_shouldNormalizeToCanonicalNone() {
        when(appConfiguration.getCookieSameSite()).thenReturn("none");

        cookieService.createOPBrowserStateCookie("opbsValue", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=None");
    }

    @Test
    public void createOPBrowserStateCookie_whenSameSiteIsInvalid_shouldFallBackToNone() {
        when(appConfiguration.getCookieSameSite()).thenReturn("invalid-value");

        cookieService.createOPBrowserStateCookie("opbsValue", httpResponse);

        assertLastSetCookieHeaderContains("SameSite=None");
    }

    private void stubModifyCookieHeaderPassThrough() {
        when(externalCookieService.modifyCookieHeader(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    private void assertLastSetCookieHeaderContains(String expected) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(httpResponse).addHeader(eq("Set-Cookie"), captor.capture());
        assertTrue(captor.getValue().contains(expected), "Expected header to contain '" + expected + "' but was: " + captor.getValue());
    }
}
