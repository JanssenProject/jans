/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.filter;

import java.io.IOException;
import java.util.Locale;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

/**
 * Negotiates GZIP compression of responses. Marks the response with
 * {@code Content-Encoding: gzip} only when the client sent {@code Accept-Encoding: gzip};
 * the RestEasy GZIPEncodingInterceptor then compresses the body. Other clients get
 * the plain response.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class GzipResponseFilter implements ContainerResponseFilter {

	private static final String GZIP = "gzip";

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		if (!responseContext.hasEntity() || responseContext.getHeaders().containsKey(HttpHeaders.CONTENT_ENCODING)) {
			return;
		}

		responseContext.getHeaders().add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
		if (acceptsGzip(requestContext.getHeaderString(HttpHeaders.ACCEPT_ENCODING))) {
			responseContext.getHeaders().putSingle(HttpHeaders.CONTENT_ENCODING, GZIP);
		}
	}

	static boolean acceptsGzip(String acceptEncoding) {
		if (acceptEncoding == null) {
			return false;
		}

		for (String token : acceptEncoding.split(",")) {
			String[] parts = token.trim().split(";");
			if (!GZIP.equals(parts[0].trim().toLowerCase(Locale.ROOT))) {
				continue;
			}

			for (int i = 1; i < parts.length; i++) {
				String param = parts[i].trim().toLowerCase(Locale.ROOT).replace(" ", "");
				if (param.startsWith("q=")) {
					try {
						return Double.parseDouble(param.substring(2)) > 0;
					} catch (NumberFormatException ex) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

}
