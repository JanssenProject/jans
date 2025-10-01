package io.jans.as.server.filter;

import io.jans.as.model.configuration.AppConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

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
}
