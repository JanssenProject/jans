/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.uma;

import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;
import org.xdi.oxauth.model.uma.UmaMetadata;

/**
 * Helper class which creates proxied UMA services
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 */
public class UmaClientFactory {

    private final static UmaClientFactory instance = new UmaClientFactory();

    private UmaClientFactory() {
    }

    public static UmaClientFactory instance() {
        return instance;
    }

    public UmaResourceService createResourceService(UmaMetadata metadataConfiguration) {
        return ProxyFactory.create(UmaResourceService.class, metadataConfiguration.getResourceRegistrationEndpoint());
    }

    public UmaResourceService createResourceService(UmaMetadata metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(UmaResourceService.class, metadataConfiguration.getResourceRegistrationEndpoint(), clientExecutor);
    }

    public UmaPermissionService createPermissionService(UmaMetadata metadataConfiguration) {
        return ProxyFactory.create(UmaPermissionService.class, metadataConfiguration.getPermissionEndpoint());
    }

    public UmaPermissionService createPermissionService(UmaMetadata metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(UmaPermissionService.class, metadataConfiguration.getPermissionEndpoint(), clientExecutor);
    }

    public UmaRptStatusService createRptStatusService(UmaMetadata metadataConfiguration) {
        return ProxyFactory.create(UmaRptStatusService.class, metadataConfiguration.getIntrospectionEndpoint());
    }

    public UmaRptStatusService createRptStatusService(UmaMetadata metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(UmaRptStatusService.class, metadataConfiguration.getIntrospectionEndpoint(), clientExecutor);
    }

    public UmaRptAuthorizationService createAuthorizationRequestService(UmaMetadata metadataConfiguration) {
        return ProxyFactory.create(UmaRptAuthorizationService.class, metadataConfiguration.getAuthorizationEndpoint());
    }

    public UmaRptAuthorizationService createAuthorizationRequestService(UmaMetadata metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(UmaRptAuthorizationService.class, metadataConfiguration.getAuthorizationEndpoint(), clientExecutor);
    }

    public UmaMetadataService createMetadataService(String umaMetadataUri) {
        return ProxyFactory.create(UmaMetadataService.class, umaMetadataUri);
    }

    public UmaMetadataService createMetadataService(String umaMetadataUri, ClientExecutor clientExecutor) {
        return ProxyFactory.create(UmaMetadataService.class, umaMetadataUri, clientExecutor);
    }

    public UmaScopeService createScopeService(String scopeEndpointUri) {
        return ProxyFactory.create(UmaScopeService.class, scopeEndpointUri);
    }

}
