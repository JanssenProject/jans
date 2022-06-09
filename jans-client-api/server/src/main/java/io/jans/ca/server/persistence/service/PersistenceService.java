package io.jans.ca.server.persistence.service;

import io.jans.ca.common.ExpiredObject;
import io.jans.ca.server.configuration.model.Rp;

import java.util.Set;

/**
 * @author yuriyz
 */
public interface PersistenceService {

    void create();

    boolean create(Rp rp);

    boolean createExpiredObject(ExpiredObject obj);

    boolean update(Rp rp);

    Rp getRp(String rpId);

    ExpiredObject getExpiredObject(String key);

    boolean isExpiredObjectPresent(String key);

    boolean removeAllRps();

    Set<Rp> getRps();

    void destroy();

    boolean remove(String rpId);

    boolean deleteExpiredObjectsByKey(String key);

    boolean deleteAllExpiredObjects();
}
