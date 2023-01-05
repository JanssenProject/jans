/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.init;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import io.jans.scim.service.filter.AuthorizationProcessingFilter;
import io.jans.scim.service.scim2.interceptor.ServiceMetadataFilter;
import io.jans.scim.ws.rs.scim2.*;

@ApplicationPath("/restv1")
public class ResteasyInitializer extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		HashSet<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(ScimConfigurationWS.class);
		classes.add(SchemaWebService.class);
		classes.add(UserWebService.class);
		classes.add(GroupWebService.class);
		classes.add(ResourceTypeWS.class);
		classes.add(SearchResourcesWebService.class);
		classes.add(ScimResourcesUpdatedWebService.class);
		classes.add(ServiceProviderConfigWS.class);
		classes.add(BulkWebService.class);
		classes.add(FidoDeviceWebService.class);
		classes.add(Fido2DeviceWebService.class);

		// Providers
		classes.add(AuthorizationProcessingFilter.class);
		classes.add(ServiceMetadataFilter.class);		

		return classes;
	}

}
