/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.init;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import io.jans.scim.ws.rs.scim2.BulkWebService;
import io.jans.scim.ws.rs.scim2.Fido2DeviceWebService;
import io.jans.scim.ws.rs.scim2.FidoDeviceWebService;
import io.jans.scim.ws.rs.scim2.GroupWebService;
import io.jans.scim.ws.rs.scim2.ResourceTypeWS;
import io.jans.scim.ws.rs.scim2.SchemaWebService;
import io.jans.scim.ws.rs.scim2.ScimConfigurationWS;
import io.jans.scim.ws.rs.scim2.ScimResourcesUpdatedWebService;
import io.jans.scim.ws.rs.scim2.SearchResourcesWebService;
import io.jans.scim.ws.rs.scim2.ServiceProviderConfigWS;
import io.jans.scim.ws.rs.scim2.UserWebService;

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

        return classes;
    }
}
