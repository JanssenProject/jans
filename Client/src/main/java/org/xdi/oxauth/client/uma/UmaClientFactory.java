/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.uma;

import org.gluu.oxauth.model.uma.UmaMetadata;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;

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

    public UmaResourceService createResourceService(UmaMetadata metadata) {
        return ProxyFactory.create(UmaResourceService.class, metadata.getResourceRegistrationEndpoint());
    }

    public UmaResourceService createResourceService(UmaMetadata metadata, ClientExecutor clientExecutor) {
        return ProxyFactory.create(UmaResourceService.class, metadata.getResourceRegistrationEndpoint(), clientExecutor);
    }

    public UmaPermissionService createPermissionService(UmaMetadata metadata) {
        return ProxyFactory.create(UmaPermissionService.class, metadata.getPermissionEndpoint());
    }

    public UmaPermissionService createPermissionService(UmaMetadata metadata, ClientExecutor clientExecutor) {
        return ProxyFactory.create(UmaPermissionService.class, metadata.getPermissionEndpoint(), clientExecutor);
    }

    public UmaRptIntrospectionService createRptStatusService(UmaMetadata metadata) {
        return ProxyFactory.create(UmaRptIntrospectionService.class, metadata.getIntrospectionEndpoint());
    }

    public UmaRptIntrospectionService createRptStatusService(UmaMetadata metadata, ClientExecutor clientExecutor) {
        return ProxyFactory.create(UmaRptIntrospectionService.class, metadata.getIntrospectionEndpoint(), clientExecutor);
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

    public UmaTokenService createTokenService(UmaMetadata metadata) {
        return ProxyFactory.create(UmaTokenService.class, metadata.getTokenEndpoint());
    }

    public UmaTokenService createTokenService(UmaMetadata metadata, ClientExecutor clientExecutor) {
        return ProxyFactory.create(UmaTokenService.class, metadata.getTokenEndpoint(), clientExecutor);
    }

}
