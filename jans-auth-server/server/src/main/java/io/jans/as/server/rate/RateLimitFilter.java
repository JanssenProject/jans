package io.jans.as.server.rate;

import io.jans.as.model.config.Constants;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;

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

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            httpRequest = rateLimitService.validateRateLimit(httpRequest);
        } catch (RateLimitedException e) {
            sendTooManyRequestsError(httpResponse);
            return;
        } catch (RequestBodyTooLargeException e) {
            log.debug(e.getMessage());
            sendResponse(httpResponse, Response.Status.REQUEST_ENTITY_TOO_LARGE, "");
            return;
        } catch (Exception e) {
            // fail closed: a request whose applicable limits could not be evaluated is not forwarded
            log.error(e.getMessage(), e);
            sendResponse(httpResponse, Response.Status.INTERNAL_SERVER_ERROR, "");
            return;
        }

        chain.doFilter(httpRequest, httpResponse);
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
