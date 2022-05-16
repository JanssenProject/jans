package io.jans.ca.server.service;

import io.jans.ca.server.op.OpClientFactoryImpl;
import io.jans.ca.server.persistence.service.MainPersistenceService;

public class ServiceProvider {

    private ValidationService validationService;
    private MainPersistenceService jansConfigurationService;
    private HttpService httpService;
    private RpSyncService rpSyncService;
    private DiscoveryService discoveryService;
    private IntrospectionService introspectionService;
    private RpService rpService;
    private StateService stateService;
    private UmaTokenService umaTokenService;

    private KeyGeneratorService keyGeneratorService;
    private PublicOpKeyService publicOpKeyService;

    private RequestObjectService requestObjectService;

    private OpClientFactoryImpl opClientFactory;



    public ValidationService getValidationService() {
        return validationService;
    }

    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
    }

    public void setConfigurationService(MainPersistenceService jansConfigurationService) {
        this.jansConfigurationService = jansConfigurationService;
    }

    public HttpService getHttpService() {
        return httpService;
    }

    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    public RpSyncService getRpSyncService() {
        return rpSyncService;
    }

    public void setRpSyncService(RpSyncService rpSyncService) {
        this.rpSyncService = rpSyncService;
    }

    public DiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    public void setDiscoveryService(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    public RpService getRpService() {
        return rpService;
    }

    public void setRpService(RpService rpService) {
        this.rpService = rpService;
    }

    public IntrospectionService getIntrospectionService() {
        return introspectionService;
    }

    public void setIntrospectionService(IntrospectionService introspectionService) {
        this.introspectionService = introspectionService;
    }

    public MainPersistenceService getJansConfigurationService() {
        return jansConfigurationService;
    }

    public void setJansConfigurationService(MainPersistenceService jansConfigurationService) {
        this.jansConfigurationService = jansConfigurationService;
    }

    public StateService getStateService() {
        return stateService;
    }

    public void setStateService(StateService stateService) {
        this.stateService = stateService;
    }

    public UmaTokenService getUmaTokenService() {
        return umaTokenService;
    }

    public void setUmaTokenService(UmaTokenService umaTokenService) {
        this.umaTokenService = umaTokenService;
    }

    public KeyGeneratorService getKeyGeneratorService() {
        return keyGeneratorService;
    }

    public void setKeyGeneratorService(KeyGeneratorService keyGeneratorService) {
        this.keyGeneratorService = keyGeneratorService;
    }

    public PublicOpKeyService getPublicOpKeyService() {
        return publicOpKeyService;
    }

    public void setPublicOpKeyService(PublicOpKeyService publicOpKeyService) {
        this.publicOpKeyService = publicOpKeyService;
    }

    public RequestObjectService getRequestObjectService() {
        return requestObjectService;
    }

    public void setRequestObjectService(RequestObjectService requestObjectService) {
        this.requestObjectService = requestObjectService;
    }

    public void setOpClientFactory(OpClientFactoryImpl opClientFactory) {
        this.opClientFactory = opClientFactory;
    }

    public OpClientFactoryImpl getOpClientFactory() {
        return opClientFactory;
    }
}
