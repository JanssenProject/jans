package io.jans.ca.server.service;

import com.google.inject.Inject;
import io.jans.ca.common.ExpiredObject;
import io.jans.ca.common.ExpiredObjectType;
import io.jans.ca.server.persistence.service.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestObjectService {
    private static final Logger LOG = LoggerFactory.getLogger(RequestObjectService.class);

    private PersistenceService persistenceService;

    private ConfigurationService configurationService;

    @Inject
    public RequestObjectService(PersistenceService persistenceService, ConfigurationService configurationService) {
        this.persistenceService = persistenceService;
        this.configurationService = configurationService;
    }

    public void put(String requestUriId, String requestObject) {
        persistenceService.createExpiredObject(new ExpiredObject(requestUriId, requestObject, ExpiredObjectType.REQUEST_OBJECT, configurationService.get().getRequestObjectExpirationInMinutes()));
    }

    public ExpiredObject get(String requestUriId) {
        return persistenceService.getExpiredObject(requestUriId);
    }

}
