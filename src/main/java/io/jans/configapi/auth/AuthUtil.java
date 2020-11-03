package io.jans.configapi.auth;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.container.ResourceInfo;

import io.jans.configapi.filters.ProtectedApi;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.container.ResourceInfo;
import java.util.List;

@ApplicationScoped
public class AuthUtil {

	public static List<String> getRequestedScopes(ResourceInfo resourceInfo) {
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

	private static void addMethodScopes(ResourceInfo resourceInfo, List<String> scopes) {
		Method resourceMethod = resourceInfo.getResourceMethod();
		ProtectedApi methodAnnotation = resourceMethod.getAnnotation(ProtectedApi.class);
		if (methodAnnotation != null) {
			scopes.addAll(Stream.of(methodAnnotation.scopes()).collect(Collectors.toList()));
		}
	}

}
