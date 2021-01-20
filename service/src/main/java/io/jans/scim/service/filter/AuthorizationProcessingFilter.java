/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.filter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

import io.jans.scim.auth.ProtectionService;
import io.jans.scim.auth.BindingUrls;

/**
 * A RestEasy filter to centralize protection of APIs based on path
 * pattern. Created by jgomer on 2017-11-25.
 * 
 * @author Yuriy Movchan Date: 02/14/2017
 */
// Note for developers: to protect methods with this filter just add the 
// @ProtectedApi annotation to them and ensure there is a proper subclass
// of {@link ProtectionService} that can handle specific protection logic
// for your particular case
@Provider
@ProtectedApi
@Priority(Priorities.AUTHENTICATION)
@RequestScoped
public class AuthorizationProcessingFilter implements ContainerRequestFilter {

	@Inject
	private Logger log;

	@Context
	private HttpHeaders httpHeaders;

	@Context
	private ResourceInfo resourceInfo;

	@Inject
	private Instance<ProtectionService> protectionServiceInstance;
	
	@Inject
	private BeanManager beanManager;

	private Map<String, Class<ProtectionService>> protectionMapping;

	/**
	 * This method performs the protection check of service invocations: it provokes
	 * returning an early error response if the underlying protection logic does not
	 * succeed, otherwise, makes the request flow to its destination service object
	 * 
	 * @param requestContext
	 *            The ContainerRequestContext associated to filter execution
	 * @throws IOException
	 *             In practice no exception is thrown here. It's present to conform
	 *             to interface implemented.
	 */
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String path = requestContext.getUriInfo().getPath();
		log.debug("REST call to '{}' intercepted", path);
		ProtectionService protectionService = null;
		for (String prefix : protectionMapping.keySet()) {
			if (path.startsWith(prefix)) {
				protectionService = protectionServiceInstance.select(protectionMapping.get(prefix)).get();
				break;
			}
		}
		if (protectionService == null) {
			log.warn("No concrete protection mechanism is associated to this path " +
				"(resource will be accessed anonymously)");
		} else {
			log.debug("Path is protected, proceeding with authorization processing...");
			Response authorizationResponse = protectionService.processAuthorization(httpHeaders, resourceInfo);
			if (authorizationResponse == null) {
				log.debug("Authorization passed"); // If authorization passed, proceed with actual processing of request
			} else {
				requestContext.abortWith(authorizationResponse);
			}
		}

	}

	/**
	 * Builds a map around url patterns and service beans that are aimed to perform
	 * actual protection
	 */
	@SuppressWarnings("unchecked")
	@PostConstruct
	private void init() {
		protectionMapping = new HashMap<String, Class<ProtectionService>>();
		Set<Bean<?>> beans = beanManager.getBeans(ProtectionService.class, Any.Literal.INSTANCE);
		
		for (Bean bean : beans) {
			Class beanClass = bean.getBeanClass();
			Annotation beanAnnotation = beanClass.getAnnotation(BindingUrls.class);
			if (beanAnnotation != null) {
				for (String pattern : ((BindingUrls) beanAnnotation).value()) {
					if (pattern.length() > 0) {
						protectionMapping.put(pattern, beanClass);
					}
				}
			}
		}
	}

}
