package org.xdi.oxd.server.persistence;

import org.xdi.oxd.server.service.Rp;

import java.util.Set;

/**
 * @author yuriyz
 */
public interface PersistenceService {

    void create();

    boolean create(Rp rp);

    boolean update(Rp rp);

    Rp getRp(String oxdId);

    boolean removeAllRps();

    Set<Rp> getRps();

    void destroy();
}
