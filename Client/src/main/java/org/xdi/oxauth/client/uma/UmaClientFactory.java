package org.xdi.oxauth.client.uma;

import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;
import org.xdi.oxauth.model.uma.MetadataConfiguration;

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

    public ResourceSetRegistrationService createResourceSetRegistrationService(MetadataConfiguration metadataConfiguration) {
        return ProxyFactory.create(ResourceSetRegistrationService.class, metadataConfiguration.getResourceSetRegistrationEndpoint());
    }

    public ResourceSetRegistrationService createResourceSetRegistrationService(MetadataConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(ResourceSetRegistrationService.class, metadataConfiguration.getResourceSetRegistrationEndpoint(), clientExecutor);
    }

    public RequesterPermissionTokenService createRequesterPermissionTokenService(MetadataConfiguration metadataConfiguration) {
        return ProxyFactory.create(RequesterPermissionTokenService.class, metadataConfiguration.getRptEndpoint());
    }

    public RequesterPermissionTokenService createRequesterPermissionTokenService(MetadataConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(RequesterPermissionTokenService.class, metadataConfiguration.getRptEndpoint(), clientExecutor);
    }

    public ResourceSetPermissionRegistrationService createResourceSetPermissionRegistrationService(MetadataConfiguration metadataConfiguration) {
        return ProxyFactory.create(ResourceSetPermissionRegistrationService.class, metadataConfiguration.getPermissionRegistrationEndpoint());
    }

    public ResourceSetPermissionRegistrationService createResourceSetPermissionRegistrationService(MetadataConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(ResourceSetPermissionRegistrationService.class, metadataConfiguration.getPermissionRegistrationEndpoint(), clientExecutor);
    }

    public RptStatusService createRptStatusService(MetadataConfiguration metadataConfiguration) {
        return ProxyFactory.create(RptStatusService.class, metadataConfiguration.getIntrospectionEndpoint());
    }

    public RptStatusService createRptStatusService(MetadataConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(RptStatusService.class, metadataConfiguration.getIntrospectionEndpoint(), clientExecutor);
    }

    public AuthorizationRequestService createAuthorizationRequestService(MetadataConfiguration metadataConfiguration) {
        return ProxyFactory.create(AuthorizationRequestService.class, metadataConfiguration.getAuthorizationRequestEndpoint());
    }

    public AuthorizationRequestService createAuthorizationRequestService(MetadataConfiguration metadataConfiguration, ClientExecutor clientExecutor) {
        return ProxyFactory.create(AuthorizationRequestService.class, metadataConfiguration.getAuthorizationRequestEndpoint(), clientExecutor);
    }

    public MetaDataConfigurationService createMetaDataConfigurationService(String umaMetaDataUri) {
        return ProxyFactory.create(MetaDataConfigurationService.class, umaMetaDataUri);
    }

    public MetaDataConfigurationService createMetaDataConfigurationService(String umaMetaDataUri, ClientExecutor clientExecutor) {
        return ProxyFactory.create(MetaDataConfigurationService.class, umaMetaDataUri, clientExecutor);
    }

    public ScopeService createScopeService(MetadataConfiguration p_configuration) {
        return createScopeService(p_configuration.getScopeEndpoint());
    }

    public ScopeService createScopeService(String umaMetaDataUri) {
        return ProxyFactory.create(ScopeService.class, umaMetaDataUri);
    }

}
