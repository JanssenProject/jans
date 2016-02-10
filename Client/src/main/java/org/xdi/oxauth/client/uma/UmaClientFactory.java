/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.uma;

import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;
import org.xdi.oxauth.model.uma.UmaConfiguration;

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

    public ResourceSetRegistrationService createResourceSetRegistrationService(UmaConfiguration metadataConfiguration) {
        return ProxyFactory.create(ResourceSetRegistrationService.class, metadataConfiguration.getResourceSetRegistrationEndpoint());
    }

    public ResourceSetRegistrationService createResourceSetRegistrationService(UmaConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(ResourceSetRegistrationService.class, metadataConfiguration.getResourceSetRegistrationEndpoint(), clientExecutor);
    }

    public CreateRptService createRequesterPermissionTokenService(UmaConfiguration metadataConfiguration) {
        return ProxyFactory.create(CreateRptService.class, metadataConfiguration.getRptEndpoint());
    }

    public CreateRptService createRequesterPermissionTokenService(UmaConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(CreateRptService.class, metadataConfiguration.getRptEndpoint(), clientExecutor);
    }

    public CreateGatService createGatService(UmaConfiguration metadataConfiguration) {
        return ProxyFactory.create(CreateGatService.class, metadataConfiguration.getGatEndpoint());
    }

    public CreateGatService createGatService(UmaConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(CreateGatService.class, metadataConfiguration.getGatEndpoint(), clientExecutor);
    }

    public PermissionRegistrationService createResourceSetPermissionRegistrationService(UmaConfiguration metadataConfiguration) {
        return ProxyFactory.create(PermissionRegistrationService.class, metadataConfiguration.getPermissionRegistrationEndpoint());
    }

    public PermissionRegistrationService createResourceSetPermissionRegistrationService(UmaConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(PermissionRegistrationService.class, metadataConfiguration.getPermissionRegistrationEndpoint(), clientExecutor);
    }

    public RptStatusService createRptStatusService(UmaConfiguration metadataConfiguration) {
        return ProxyFactory.create(RptStatusService.class, metadataConfiguration.getIntrospectionEndpoint());
    }

    public RptStatusService createRptStatusService(UmaConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(RptStatusService.class, metadataConfiguration.getIntrospectionEndpoint(), clientExecutor);
    }

    public RptAuthorizationRequestService createAuthorizationRequestService(UmaConfiguration metadataConfiguration) {
        return ProxyFactory.create(RptAuthorizationRequestService.class, metadataConfiguration.getAuthorizationEndpoint());
    }

    public RptAuthorizationRequestService createAuthorizationRequestService(UmaConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(RptAuthorizationRequestService.class, metadataConfiguration.getAuthorizationEndpoint(), clientExecutor);
    }

    public UmaConfigurationService createMetaDataConfigurationService(String umaMetaDataUri) {
        return ProxyFactory.create(UmaConfigurationService.class, umaMetaDataUri);
    }

    public UmaConfigurationService createMetaDataConfigurationService(String umaMetaDataUri, ClientExecutor clientExecutor) {
        return ProxyFactory.create(UmaConfigurationService.class, umaMetaDataUri, clientExecutor);
    }

    public ScopeService createScopeService(UmaConfiguration p_configuration) {
        return createScopeService(p_configuration.getScopeEndpoint());
    }

    public ScopeService createScopeService(String umaMetaDataUri) {
        return ProxyFactory.create(ScopeService.class, umaMetaDataUri);
    }

}
