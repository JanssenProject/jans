package io.jans.core.cedarling.service;

import static io.jans.core.cedarling.service.CedarlingProtection.simpleResponse;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.core.cedarling.model.CedarlingPermission;
import io.jans.core.cedarling.model.OpenIDConnectConfig;
import io.jans.core.cedarling.service.security.api.ProtectedCedarlingApi;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

/**
 * @author Yuriy Movchan Date: 10/08/2022
 */
public abstract class CedarlingProtectionService implements CedarlingProtection {

	@Inject
    protected Logger log;

    @Inject
    protected OpenIDConnectConfig openIDConnectConfig;
    
    @Inject
    protected CedarlingAuthorizationService authorizationService;

    public abstract Response processAuthorization(String bearerToken, ResourceInfo resourceInfo);
    
    protected Response isValid(String bearerToken, ResourceInfo resourceInfo) {
        List<CedarlingPermission> requestedPermissions = getRequestedOperations(resourceInfo);
        log.info("Check access to requested opearations: {}", requestedPermissions);
        if (requestedPermissions.isEmpty()) {
            return simpleResponse(INTERNAL_SERVER_ERROR, "Access to operation is not correct");
        }

        boolean authorized = true;
        Map<String, String> tokens = getCedarlingTokens(bearerToken);
        for (CedarlingPermission requestedPermission : requestedPermissions) {
            authorized &= authorizationService.authorize(tokens, requestedPermission.getAction(),
                    getCedarlingResource(requestedPermission), getCedarlingContext());
            if (!authorized) {
                log.error("Insufficient permissions to access '{}'", requestedPermission);
                break;
            }
        }
        
        if (authorized) {
            return null;
        }

        return simpleResponse(FORBIDDEN, "Insufficient permissions to access requested operations");
    }

	private Map<String, String> getCedarlingTokens(String accessToken) {
		return Map.of(CedarlingAuthorizationService.CEDARLING_JANS_ACCESS_TOKEN, accessToken);
	}

	private Map<String, Object> getCedarlingResource(CedarlingPermission requestedPermission) {
		HashMap<String, Object> map = new HashMap<>();
		int id = requestedPermission.hashCode();
		id = id > 0 ? id : -id;
		map.putAll(
				Map.of("cedar_entity_mapping",
						Map.of("entity_type", requestedPermission.getResource(),
								"id", id)
					)
		);
		map.putAll(
				Map.of("url",
						Map.of("host", "", "path", requestedPermission.getPath(), "protocol", "")
					)
		);
		map.putAll(
				Map.of("header",
						Map.of()
					)
		);

		return map;
	}
	
	private Map<String, Object> getCedarlingContext() {
	    return new HashMap<>();
	}

    protected List<CedarlingPermission> getRequestedOperations(ResourceInfo resourceInfo) {
        List<CedarlingPermission> cedarlingPermissions = new ArrayList<>();
        addCedarlingPermission(cedarlingPermissions, getOperationFromAnnotation(resourceInfo.getResourceClass()));
        addCedarlingPermission(cedarlingPermissions, getOperationFromAnnotation(resourceInfo.getResourceMethod()));

        Method baseMethod = resourceInfo.getResourceMethod();
        for (Class<?> interfaces : resourceInfo.getResourceClass().getInterfaces()) {
        	addCedarlingPermission(cedarlingPermissions, getOperationFromAnnotation(interfaces));
            
            Method method = null;
			try {
				method = interfaces.getDeclaredMethod(baseMethod.getName(), baseMethod.getParameterTypes());
			} catch (NoSuchMethodException | SecurityException e) {
				// It's expected behavior
			}
            if (method != null) {
            	addCedarlingPermission(cedarlingPermissions, getOperationFromAnnotation(method));
            }

        }

        return cedarlingPermissions;
    }

	private void addCedarlingPermission(List<CedarlingPermission> cedarlingPermissions, CedarlingPermission permission) {
		if (permission != null) {
			cedarlingPermissions.add(permission);
		}
	}

	private CedarlingPermission getOperationFromAnnotation(AnnotatedElement elem) {
		Optional<ProtectedCedarlingApi> annotation = optAnnnotation(elem, ProtectedCedarlingApi.class);
		if (annotation.isPresent()) {
			ProtectedCedarlingApi cedarlingPermission = annotation.get();
			return new CedarlingPermission(cedarlingPermission.action(), cedarlingPermission.resource(), cedarlingPermission.id(), cedarlingPermission.path());
		} else {
			return null;
		}
	}

    private static <T extends Annotation> Optional<T> optAnnnotation(AnnotatedElement elem, Class<T> cls) {
        return Optional.ofNullable(elem.getAnnotation(cls));
    }

}
