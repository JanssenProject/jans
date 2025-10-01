/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase.impl;

import java.util.ArrayList;
import java.util.List;

import io.jans.orm.model.BatchOperation;
import io.jans.orm.reflect.property.PropertyAnnotation;

import com.couchbase.client.java.json.JsonObject;

/**
 * Couchbase batch operation wrapper
 *
 * @author Yuriy Movchan Date: 05/16/2018
 */
public class CouchbaseBatchOperationWraper<T> {

    private CouchbaseEntryManager couchbaseEntryManager;
    private Class<T> entryClass;
    private List<PropertyAnnotation> propertiesAnnotations;

    private BatchOperation<T> batchOperation;

    public CouchbaseBatchOperationWraper(BatchOperation<T> batchOperation) {
        this.batchOperation = batchOperation;
    }

    public CouchbaseBatchOperationWraper(BatchOperation<T> batchOperation, CouchbaseEntryManager couchbaseEntryManager, Class<T> entryClass,
            List<PropertyAnnotation> propertiesAnnotations) {
        this.batchOperation = batchOperation;
        this.couchbaseEntryManager = couchbaseEntryManager;
        this.entryClass = entryClass;
        this.propertiesAnnotations = propertiesAnnotations;
    }

    public final BatchOperation<T> getBatchOperation() {
        return batchOperation;
    }

    public List<T> createEntities(List<JsonObject> searchResult) {
        if (couchbaseEntryManager == null) {
            return new ArrayList<T>(0);
        }

        return couchbaseEntryManager.createEntities(entryClass, propertiesAnnotations, null, searchResult.toArray(new JsonObject[0]));
    }

}
