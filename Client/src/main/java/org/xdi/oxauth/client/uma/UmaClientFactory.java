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
 * Helper class which creates proxy UMA resource set description service
 *
 * @author Yuriy Movchan Date: 10/04/2012
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

    public RequesterPermissionTokenService createRequesterPermissionTokenService(UmaConfiguration metadataConfiguration) {
        return ProxyFactory.create(RequesterPermissionTokenService.class, metadataConfiguration.getRptEndpoint());
    }

    public RequesterPermissionTokenService createRequesterPermissionTokenService(UmaConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(RequesterPermissionTokenService.class, metadataConfiguration.getRptEndpoint(), clientExecutor);
    }

    public ResourceSetPermissionRegistrationService createResourceSetPermissionRegistrationService(UmaConfiguration metadataConfiguration) {
        return ProxyFactory.create(ResourceSetPermissionRegistrationService.class, metadataConfiguration.getPermissionRegistrationEndpoint());
    }

    public ResourceSetPermissionRegistrationService createResourceSetPermissionRegistrationService(UmaConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(ResourceSetPermissionRegistrationService.class, metadataConfiguration.getPermissionRegistrationEndpoint(), clientExecutor);
    }

    public RptStatusService createRptStatusService(UmaConfiguration metadataConfiguration) {
        return ProxyFactory.create(RptStatusService.class, metadataConfiguration.getIntrospectionEndpoint());
    }

    public RptStatusService createRptStatusService(UmaConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(RptStatusService.class, metadataConfiguration.getIntrospectionEndpoint(), clientExecutor);
    }

    public AuthorizationRequestService createAuthorizationRequestService(UmaConfiguration metadataConfiguration) {
        return ProxyFactory.create(AuthorizationRequestService.class, metadataConfiguration.getAuthorizationEndpoint());
    }

    public AuthorizationRequestService createAuthorizationRequestService(UmaConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(AuthorizationRequestService.class, metadataConfiguration.getAuthorizationEndpoint(), clientExecutor);
    }

    public MetaDataConfigurationService createMetaDataConfigurationService(String umaMetaDataUri) {
        return ProxyFactory.create(MetaDataConfigurationService.class, umaMetaDataUri);
    }

    public MetaDataConfigurationService createMetaDataConfigurationService(String umaMetaDataUri, ClientExecutor clientExecutor) {
        return ProxyFactory.create(MetaDataConfigurationService.class, umaMetaDataUri, clientExecutor);
    }

    public ScopeService createScopeService(UmaConfiguration p_configuration) {
        return createScopeService(p_configuration.getScopeEndpoint());
    }

    public ScopeService createScopeService(String umaMetaDataUri) {
        return ProxyFactory.create(ScopeService.class, umaMetaDataUri);
    }

}
