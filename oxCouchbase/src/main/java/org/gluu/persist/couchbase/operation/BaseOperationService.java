/*
 /*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.operation;

import java.util.List;

import org.gluu.persist.couchbase.impl.CouchbaseBatchOperationWraper;
import org.gluu.persist.couchbase.operation.impl.CouchbaseConnectionProvider;
import org.gluu.persist.exception.operation.AuthenticationException;
import org.gluu.persist.exception.operation.DuplicateEntryException;
import org.gluu.persist.exception.operation.PersistenceException;
import org.gluu.persist.exception.operation.SearchException;
import org.gluu.persist.model.ListViewResponse;
import org.gluu.persist.model.SearchScope;

/**
 * Base low level methods which operation service implementation should provide
 *
 * @author Yuriy Movchan Date: 05/14/2018
 */
public interface BaseOperationService<P, T, M, E, S> {

    P getConnectionProvider();
    void setConnectionProvider(P connectionProvider);

    boolean authenticate(String key, String password) throws SearchException, AuthenticationException ;

    boolean addEntry(String key, T atts) throws DuplicateEntryException, PersistenceException;

    boolean updateEntry(String key, T attrs) throws UnsupportedOperationException, PersistenceException;
    boolean updateEntry(String key, List<M> mods) throws UnsupportedOperationException, PersistenceException;

    boolean delete(String key) throws PersistenceException;
    boolean deleteRecursively(String key) throws PersistenceException;

    T lookup(String key, String... attributes) throws SearchException;

    <O> ListViewResponse<T> search(String key, E expression, SearchScope scope,
            int startIndex, int pageLimit, int count, S[] orderBy,
            CouchbaseBatchOperationWraper<O> batchOperationWraper, boolean returnCount, String... attributes) throws SearchException;

    boolean isBinaryAttribute(String attribute);
    boolean isCertificateAttribute(String attribute);

    boolean destroy();

}
