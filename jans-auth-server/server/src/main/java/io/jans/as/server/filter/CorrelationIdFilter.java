package io.jans.as.server.filter;

import io.jans.as.server.uma.authorization.UmaWebException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;

import static io.jans.as.model.config.Constants.CORRELATION_ID_HEADER;

@WebFilter(filterName = "CorrelationIdFilter", asyncSupported = true, urlPatterns = {"/*"})
public class CorrelationIdFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(UmaWebException.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
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
    }

}
