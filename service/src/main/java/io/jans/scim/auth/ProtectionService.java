package io.jans.scim.auth;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import io.jans.scim.service.filter.ProtectedApi;

public abstract class ProtectionService implements Serializable {

	private static final long serialVersionUID = -1147131971095468865L;

    public abstract Response processAuthorization(HttpHeaders headers, ResourceInfo resourceInfo);

	protected Response getErrorResponse(Response.Status status, String detail) {
		return Response.status(status).entity(detail).build();
	}

	protected List<String> getRequestedScopes(ResourceInfo resourceInfo) {
		Class<?> resourceClass = resourceInfo.getResourceClass();
		ProtectedApi typeAnnotation = resourceClass.getAnnotation(ProtectedApi.class);
		List<String> scopes = new ArrayList<String>();
		if (typeAnnotation == null) {
			addMethodScopes(resourceInfo, scopes);
		} else {
			scopes.addAll(Stream.of(typeAnnotation.scopes()).collect(Collectors.toList()));
			addMethodScopes(resourceInfo, scopes);
		}
		return scopes;
	}

	private void addMethodScopes(ResourceInfo resourceInfo, List<String> scopes) {
		Method resourceMethod = resourceInfo.getResourceMethod();
		ProtectedApi methodAnnotation = resourceMethod.getAnnotation(ProtectedApi.class);
		if (methodAnnotation != null) {
			scopes.addAll(Stream.of(methodAnnotation.scopes()).collect(Collectors.toList()));
		}
	}	

}