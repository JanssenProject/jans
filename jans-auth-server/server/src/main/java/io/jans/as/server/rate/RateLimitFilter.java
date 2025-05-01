package io.jans.as.server.rate;

import io.jans.as.client.RegisterRequest;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.error.ErrorResponseFactory;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * @author Yuriy Z
 */
@WebFilter(
        filterName = "RateLimitFilter",
        asyncSupported = true,
        urlPatterns = {
                "/restv1/register"
        },
        displayName = "RateLimitFilter")
@Priority(Priorities.AUTHENTICATION)
public class RateLimitFilter implements Filter {

    public static final String TOO_MANY_REQUESTS_JSON_ERROR = "{\"error\": \"Too many requests\"}";

    @Inject
    private Logger log;
    @Inject
    private RateLimitService rateLimitService;
    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            httpRequest = validateRateLimit(httpRequest);
            chain.doFilter(httpRequest, httpResponse);
        } catch (RateLimitedException e) {
            sendTooManyRequestsError(httpResponse);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            chain.doFilter(httpRequest, httpResponse);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            sendResponse(httpResponse, Response.Status.INTERNAL_SERVER_ERROR, "");
        }
    }

    private HttpServletRequest validateRateLimit(HttpServletRequest httpRequest) throws RateLimitedException, IOException {
        // if rate_limit flag is disabled immediately return
        if (!errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.RATE_LIMIT)){
            return httpRequest;
        }

        final String requestUrl = httpRequest.getRequestURL().toString();

        boolean isRegisterEndpoint = requestUrl.endsWith("/register");

        if (isRegisterEndpoint) {
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);
            final RegisterRequest registerRequest = rateLimitService.parseRegisterRequest(cachedRequest.getCachedBodyAsString());
            String key = "no_key";
            if (registerRequest != null) {
                final String ssa = registerRequest.getSoftwareStatement();
                final List<String> redirectUris = registerRequest.getRedirectUris();

                if (StringUtils.isNotBlank(ssa)) {
                    // hash ssa to save memory
                    key = DigestUtils.sha256Hex(ssa);
                } else if (CollectionUtils.isNotEmpty(redirectUris) && StringUtils.isNotBlank(redirectUris.get(0))) {
                    key = redirectUris.get(0);
                }
            }

            rateLimitService.validateRateLimitForRegister(key);
            return cachedRequest;
        }

        return httpRequest;
    }

    private void sendTooManyRequestsError(HttpServletResponse servletResponse) {
        sendResponse(servletResponse, Response.Status.TOO_MANY_REQUESTS, TOO_MANY_REQUESTS_JSON_ERROR);
    }

    private void sendResponse(HttpServletResponse servletResponse, Response.Status status, String payloadAsJson) {
        log.debug("send back response - status: {}, payload: {}", status.getStatusCode(), payloadAsJson);

        try (PrintWriter out = servletResponse.getWriter()) {
            servletResponse.setStatus(status.getStatusCode());
            if (StringUtils.isNotBlank(payloadAsJson)) {
                servletResponse.setContentType(Constants.CONTENT_TYPE_APPLICATION_JSON_UTF_8);
                out.write(payloadAsJson);
            }
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("Rate Limit Filter initialized.");
    }

    @Override
    public void destroy() {
        log.info("Rate Limit Filter destroyed.");
    }
}
