package io.jans.as.server.filter;

import io.jans.as.model.configuration.AppConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class HeadersFilterTest {

    @Test
    public void addXFrameOptionsResponseHeader_forHtmPageRequest_shouldAddXFrameOptionsHeader() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        AppConfiguration appConfiguration = new AppConfiguration();

        when(request.getRequestURI()).thenReturn("/jans-auth/authorize.htm");

        HeadersFilter.addXFrameOptionsResponseHeader(request, response, appConfiguration);

        verify(response, times(1)).addHeader("X-Frame-Options", "SAMEORIGIN");
    }

    @Test
    public void addXFrameOptionsResponseHeader_forDiscoveryRequest_shouldNotAddXFrameOptionsHeader() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        AppConfiguration appConfiguration = new AppConfiguration();

        when(request.getRequestURI()).thenReturn("/.well-known/openid-configuration");

        HeadersFilter.addXFrameOptionsResponseHeader(request, response, appConfiguration);

        verify(response, times(0)).addHeader("X-Frame-Options", "SAMEORIGIN");
    }

    @Test
    public void addSecurityResponseHeaders_forTokenRequest_shouldAddSecurityHeaders() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        AppConfiguration appConfiguration = new AppConfiguration();

        when(request.getRequestURI()).thenReturn("/jans-auth/restv1/token");

        HeadersFilter.addSecurityResponseHeaders(request, response, appConfiguration);

        verify(response).addHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        verify(response).addHeader("X-Content-Type-Options", "nosniff");
        verify(response).addHeader("Referrer-Policy", "no-referrer");
    }

    @Test
    public void addSecurityResponseHeaders_forAuthorizeRequest_shouldNotAddSecurityHeaders() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        AppConfiguration appConfiguration = new AppConfiguration();

        when(request.getRequestURI()).thenReturn("/jans-auth/restv1/authorize");

        HeadersFilter.addSecurityResponseHeaders(request, response, appConfiguration);

        verify(response, never()).addHeader(anyString(), anyString());
    }

    @Test
    public void addSecurityResponseHeaders_whenDisabled_shouldNotAddSecurityHeaders() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setSecurityHeadersEnabled(false);

        HeadersFilter.addSecurityResponseHeaders(request, response, appConfiguration);

        verify(response, never()).addHeader(anyString(), anyString());
    }

    @Test
    public void addSecurityResponseHeaders_withCustomUriList_shouldUseConfiguredList() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setApplySecurityHeadersIfUriContainsAny(Collections.singletonList("/restv1/par"));

        when(request.getRequestURI()).thenReturn("/jans-auth/restv1/token");

        HeadersFilter.addSecurityResponseHeaders(request, response, appConfiguration);

        verify(response, never()).addHeader(anyString(), anyString());
    }
}
