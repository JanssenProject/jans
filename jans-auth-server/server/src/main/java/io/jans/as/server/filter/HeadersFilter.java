package io.jans.as.server.filter;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.model.config.ConfigurationFactory;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static io.jans.as.model.config.Constants.X_FRAME_OPTIONS_HEADER;

/**
 * @author Yuriy Z
 */
@WebFilter(filterName = "HeadersFilter", asyncSupported = true, urlPatterns = {"/*"})
public class HeadersFilter implements Filter {

    @Inject
    private ConfigurationFactory configurationFactory;

    @Override
    public void init(FilterConfig filterConfig) {
        // empty
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        addXFrameOptionsResponseHeader(httpRequest, httpResponse, configurationFactory.getAppConfiguration());

        chain.doFilter(request, response);
    }

    public static void addXFrameOptionsResponseHeader(HttpServletRequest httpRequest, HttpServletResponse httpResponse, AppConfiguration appConfiguration) {
        final String requestURI = httpRequest.getRequestURI();
        final boolean hasAny = appConfiguration.getApplyXFrameOptionsHeaderIfUriContainsAny().stream().anyMatch(requestURI::contains);

        // add header for:
        // 1. any .htm page
        // 2. for any match of request uri to configured value
        if (requestURI.contains(".htm") || hasAny) {
            httpResponse.addHeader(X_FRAME_OPTIONS_HEADER, appConfiguration.getXframeOptionsHeaderValue().getValue());
        }
    }

    @Override
    public void destroy() {
        // empty
    }
}
