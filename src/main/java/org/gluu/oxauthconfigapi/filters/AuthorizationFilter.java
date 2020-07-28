/**
 * 
 */
package org.gluu.oxauthconfigapi.filters;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

import io.vertx.core.http.HttpServerRequest;

/**
 * @author Mougang T.Gasmyr
 *
 */
@Provider
@PreMatching
@Priority(1)
public class AuthorizationFilter implements ContainerRequestFilter {
	private static final String AUTHENTICATION_SCHEME = "Bearer";

	@Context
	UriInfo info;

	@Context
	HttpServerRequest request;

	@Inject
	Logger logger;

	public void filter(ContainerRequestContext context) {
		logger.info("=======================================================================");
		logger.info("======" + context.getMethod() + " " + info.getPath() + " FROM IP "
				+ request.remoteAddress().toString());
		logger.info("======PERFORMING AUTHORIZATION=========================================");
		String authorizationHeader = context.getHeaderString(HttpHeaders.AUTHORIZATION);
		if (!isTokenBasedAuthentication(authorizationHeader)) {
			abortWithUnauthorized(context);
			logger.info("======ONLY TOKEN BASED AUTHORIZATION IS SUPPORTED======================");
			return;
		}
		String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();
		try {
			validateToken(token,context);
			logger.info("======AUTHORIZATION  GRANTED===========================================");	
		} catch (Exception e) {
			abortWithUnauthorized(context);
			logger.info("======INVALID AUTHORIZATION TOKEN======================================");
		}
        
	}

	private boolean isTokenBasedAuthentication(String authorizationHeader) {
		return authorizationHeader != null
				&& authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
	}

	private void abortWithUnauthorized(ContainerRequestContext requestContext) {
		requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
				.header(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME).build());
	}

	private void validateToken(String token, ContainerRequestContext context) throws Exception {
		// Check if the token was issued by the server and if it's not expired
		// Throw an Exception if the token is invalid
	}

}
