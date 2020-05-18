package org.gluu.oxd.server.persistence.service;

import org.gluu.oxd.common.ExpiredObject;
import org.gluu.oxd.server.service.Rp;

import java.util.Map;
import java.util.Set;

/**
 * @author yuriyz
 */
public interface PersistenceService {

    void create();

    boolean create(Rp rp);

    boolean createExpiredObject(ExpiredObject obj);

    boolean update(Rp rp);

    Rp getRp(String oxdId);

    ExpiredObject getExpiredObject(String key);

    boolean isExpiredObjectPresent(String key);

    boolean removeAllRps();

    Set<Rp> getRps();

    void destroy();

    boolean remove(String oxdId);

    boolean deleteExpiredObjectsByKey(String key);

    boolean deleteAllExpiredObjects();
}
