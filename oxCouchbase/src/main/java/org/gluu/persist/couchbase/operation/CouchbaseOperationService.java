/*
 /*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.operation;

import java.util.List;

import org.gluu.persist.couchbase.impl.CouchbaseBatchOperationWraper;
import org.gluu.persist.couchbase.model.SearchReturnDataType;
import org.gluu.persist.couchbase.operation.impl.CouchbaseConnectionProvider;
import org.gluu.persist.exception.AuthenticationException;
import org.gluu.persist.exception.operation.DuplicateEntryException;
import org.gluu.persist.exception.operation.EntryNotFoundException;
import org.gluu.persist.exception.operation.PersistenceException;
import org.gluu.persist.exception.operation.SearchException;
import org.gluu.persist.model.PagedResult;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.operation.PersistenceOperationService;

import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.Sort;
import com.couchbase.client.java.subdoc.MutationSpec;

/**
 * Couchbase operation service interface
 *
 * @author Yuriy Movchan Date: 05/14/2018
 */
public interface CouchbaseOperationService extends PersistenceOperationService {

    String DN = "dn";
    String UID = "uid";
    String USER_PASSWORD = "userPassword";
    String OBJECT_CLASS = "objectClass";

    String META_DOC_ID = "meta_doc_id";

    CouchbaseConnectionProvider getConnectionProvider();

    boolean authenticate(String key, String password) throws SearchException, AuthenticationException;

    boolean addEntry(String key, JsonObject atts) throws DuplicateEntryException, PersistenceException;

    boolean updateEntry(String key, JsonObject attrs) throws UnsupportedOperationException, PersistenceException;
    boolean updateEntry(String key, List<MutationSpec> mods) throws UnsupportedOperationException, PersistenceException;

    boolean delete(String key) throws EntryNotFoundException;
    boolean deleteRecursively(String key) throws EntryNotFoundException, SearchException;

    JsonObject lookup(String key, String... attributes) throws SearchException;

    <O> PagedResult<JsonObject> search(String key, Expression expression, SearchScope scope,
            String[] attributes, Sort[] orderBy, CouchbaseBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType,
            int start, int count, int pageSize) throws SearchException;

    String[] createStoragePassword(String[] passwords);

    boolean isBinaryAttribute(String attribute);
    boolean isCertificateAttribute(String attribute);

    boolean destroy();

}
