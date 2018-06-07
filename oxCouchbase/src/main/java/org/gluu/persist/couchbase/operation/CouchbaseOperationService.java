/*
 /*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.persist.couchbase.operation;

/**
 * Couchbase operation service interface
 *
 * @author Yuriy Movchan Date: 05/14/2018
 */
public interface CouchbaseOperationService<P, T, M, E, S> extends BaseOperationService<P, T, M, E, S> {

    final String DN = "dn";
    final String UID = "uid";
    final String SUCCESS = "success";
    final String USER_PASSWORD = "userPassword";
    final String OBJECT_CLASS = "objectClass";

    final String META_DOC_ID = "meta_doc_id";

}
