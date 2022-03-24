/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.service;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

import io.jans.eleven.model.Configuration;
import io.jans.eleven.util.StringUtils;
import org.slf4j.Logger;

import com.google.common.base.Strings;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version March 20, 2017
 */
@WebFilter(asyncSupported = true, urlPatterns = {
		"/rest/generateKey", "/rest/sign", "/rest/verifySignature", "/rest/deleteKey"
		}, displayName = "oxEleven Test Mode Filter"
)
public class TestModeTokenFilter implements Filter {

	@Inject
	private Logger log;

	@Inject
	private Configuration configuration;

	private static final String oxElevenGenerateKeyEndpoint = "rest/generateKey";
    private static final String oxElevenSignEndpoint = "rest/sign";
    private static final String oxElevenVerifySignatureEndpoint = "rest/verifySignature";
    private static final String oxElevenDeleteKeyEndpoint = "rest/deleteKey";

	public void init(FilterConfig filterConfig) throws ServletException {
	}

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            String path = httpServletRequest.getRequestURL().toString();
            if (!Strings.isNullOrEmpty(path)) {
                if (path.endsWith(oxElevenGenerateKeyEndpoint)
                        || path.endsWith(oxElevenSignEndpoint)
                        || path.endsWith(oxElevenVerifySignatureEndpoint)
                        || path.endsWith(oxElevenDeleteKeyEndpoint)) {
                    if (httpServletRequest.getHeader("Authorization") != null) {
                        String header = httpServletRequest.getHeader("Authorization");
                        if (header.startsWith("Bearer ")) {
                            String accessToken = header.substring(7);
                            String testModeToken = configuration.getTestModeToken();
                            if (!Strings.isNullOrEmpty(accessToken) && !Strings.isNullOrEmpty(testModeToken)
                                    && accessToken.equals(testModeToken)) {
                                chain.doFilter(request, response);
                                return;
                            }
                        }
                    }

                    sendError((HttpServletResponse) response);
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse servletResponse) {
        PrintWriter out = null;
        try {
            out = servletResponse.getWriter();

            servletResponse.setStatus(401);
            servletResponse.addHeader("WWW-Authenticate", "Bearer");
            servletResponse.setContentType(MediaType.APPLICATION_JSON);
            out.write(StringUtils.getErrorResponse(
                    "unauthorized",
                    "The request is not authorized."
            ));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

	public void destroy() {
	}

}
