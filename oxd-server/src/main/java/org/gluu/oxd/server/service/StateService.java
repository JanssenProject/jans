package org.gluu.oxd.server.service;

import com.google.inject.Inject;
import org.gluu.oxd.common.ExpiredObject;
import org.gluu.oxd.common.ExpiredObjectType;
import org.gluu.oxd.server.persistence.service.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * @author Yuriy Zabrovarnyy
 */

public class StateService {

    private static final Logger LOG = LoggerFactory.getLogger(StateService.class);

    private PersistenceService persistenceService;

    private ConfigurationService configurationService;

    private final SecureRandom random = new SecureRandom();

    @Inject
    public StateService(PersistenceService persistenceService, ConfigurationService configurationService) {
        this.persistenceService = persistenceService;
        this.configurationService = configurationService;
    }

    public String generateState() {
        return putState(generateSecureString());
    }

    public String generateNonce() {
        return putNonce(generateSecureString());
    }

    public String generateSecureString() {
        return new BigInteger(130, random).toString(32);
    }

    public boolean isExpiredObjectPresent(String key) {
        return persistenceService.isExpiredObjectPresent(key);
    }

    public void deleteExpiredObjectsByKey(String key) {
        persistenceService.deleteExpiredObjectsByKey(key);
    }

    public String putState(String state) {
        persistenceService.createExpiredObject(new ExpiredObject(state, state, ExpiredObjectType.STATE, configurationService.get().getStateExpirationInMinutes()));
        return state;
    }

    public String putNonce(String nonce) {
        persistenceService.createExpiredObject(new ExpiredObject(nonce, nonce, ExpiredObjectType.NONCE, configurationService.get().getNonceExpirationInMinutes()));
        return nonce;
    }
}
