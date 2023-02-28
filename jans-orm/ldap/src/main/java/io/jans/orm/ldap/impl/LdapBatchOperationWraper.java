/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.ldap.impl;

import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.EntryData;
import io.jans.orm.reflect.property.PropertyAnnotation;

import java.util.ArrayList;
import java.util.List;

/**
 * LDAP batch operation wrapper
 *
 * @author Yuriy Movchan Date: 02/07/2010
 */
public class LdapBatchOperationWraper<T> {

    private LdapEntryManager ldapEntryManager;
    private Class<T> entryClass;
    private List<PropertyAnnotation> propertiesAnnotations;

    private BatchOperation<T> batchOperation;

    public LdapBatchOperationWraper(BatchOperation<T> batchOperation) {
        this.batchOperation = batchOperation;
    }

    public LdapBatchOperationWraper(BatchOperation<T> batchOperation, LdapEntryManager ldapEntryManager, Class<T> entryClass,
            List<PropertyAnnotation> propertiesAnnotations) {
        this.batchOperation = batchOperation;
        this.ldapEntryManager = ldapEntryManager;
        this.entryClass = entryClass;
        this.propertiesAnnotations = propertiesAnnotations;
    }

    public final BatchOperation<T> getBatchOperation() {
        return batchOperation;
    }

    public List<T> createEntities(List<EntryData> entryDataList) {
        if (ldapEntryManager == null) {
            return new ArrayList<T>(0);
        }

        return ldapEntryManager.createEntities(entryClass, propertiesAnnotations, entryDataList);
    }

}
