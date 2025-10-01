package io.jans.as.server.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;

import static io.jans.as.model.config.Constants.CORRELATION_ID_HEADER;

@WebFilter(filterName = "CorrelationIdFilter", asyncSupported = true, urlPatterns = {"/*"})
public class CorrelationIdFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // empty
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (StringUtils.isBlank(correlationId)) {
            correlationId = UUID.randomUUID().toString();

            Cookie[] cookies = httpRequest.getCookies();
            if (cookies == null || Stream.of(cookies).noneMatch(cookie -> cookie.getName().contains(CORRELATION_ID_HEADER))) {
                Cookie cookie = new Cookie(CORRELATION_ID_HEADER, correlationId);
                cookie.setSecure(true);
                cookie.setHttpOnly(true);
                httpResponse.addCookie(cookie);
            }
        }

        ThreadContext.put(CORRELATION_ID_HEADER, correlationId);

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // empty
    }
}
