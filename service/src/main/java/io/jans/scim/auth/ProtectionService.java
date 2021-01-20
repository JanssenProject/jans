package io.jans.scim.auth;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
		List<String> scopes = new ArrayList<>();
		scopes.addAll(getScopesFromAnnotation(resourceInfo.getResourceClass()));
		scopes.addAll(getScopesFromAnnotation(resourceInfo.getResourceMethod()));
		return scopes;
	}

	private List<String> getScopesFromAnnotation(AnnotatedElement elem) {		
		return Optional.ofNullable(elem.getAnnotation(ProtectedApi.class)).map(ProtectedApi::scopes)
		    .map(Arrays::asList).orElse(Collections.emptyList());
	}	

}