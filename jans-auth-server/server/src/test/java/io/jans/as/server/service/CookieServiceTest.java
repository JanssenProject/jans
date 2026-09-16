package io.jans.as.server.service;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.external.ExternalCookieService;
import jakarta.servlet.http.HttpServletResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
