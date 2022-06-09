package io.jans.ca.server.service;

import io.jans.ca.common.ExpiredObject;
import io.jans.ca.common.ExpiredObjectType;
import io.jans.ca.server.persistence.service.MainPersistenceService;
import io.jans.ca.server.persistence.service.PersistenceService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RequestObjectService {
    private static final Logger LOG = LoggerFactory.getLogger(RequestObjectService.class);

    @Inject
    PersistenceService persistenceService;
    @Inject
    MainPersistenceService configurationService;

    public void put(String requestUriId, String requestObject) {
        persistenceService.createExpiredObject(new ExpiredObject(requestUriId, requestObject, ExpiredObjectType.REQUEST_OBJECT, configurationService.find().getRequestObjectExpirationInMinutes()));
    }

    public ExpiredObject get(String requestUriId) {
        return persistenceService.getExpiredObject(requestUriId);
    }

}
