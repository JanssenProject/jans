/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner.impl;

import java.util.ArrayList;
import java.util.List;

import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.EntryData;
import io.jans.orm.reflect.property.PropertyAnnotation;

/**
 * SQL batch operation wrapper
 *
 * @author Yuriy Movchan Date: 01/10/2020
 */
public class SpannerBatchOperationWraper<T> {

    private SpannerEntryManager SqlEntryManager;
    private Class<T> entryClass;
    private List<PropertyAnnotation> propertiesAnnotations;

    private BatchOperation<T> batchOperation;

    public SpannerBatchOperationWraper(BatchOperation<T> batchOperation) {
        this.batchOperation = batchOperation;
    }

    public SpannerBatchOperationWraper(BatchOperation<T> batchOperation, SpannerEntryManager SqlEntryManager, Class<T> entryClass,
            List<PropertyAnnotation> propertiesAnnotations) {
        this.batchOperation = batchOperation;
        this.SqlEntryManager = SqlEntryManager;
        this.entryClass = entryClass;
        this.propertiesAnnotations = propertiesAnnotations;
    }

    public final BatchOperation<T> getBatchOperation() {
        return batchOperation;
    }

    public List<T> createEntities(List<EntryData> entryDataList) {
        if (SqlEntryManager == null) {
            return new ArrayList<T>(0);
        }

        return SqlEntryManager.createEntities(entryClass, propertiesAnnotations, null, entryDataList.toArray(new EntryData[entryDataList.size()]));
    }

}
