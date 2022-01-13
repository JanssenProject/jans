/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model;

/**
 * @author Yuriy Movchan Date: 02/07/2018
 */
public abstract class ProcessBatchOperation<T> implements BatchOperation<T> {

    public boolean collectSearchResult(int size) {
        return false;
    }

}
