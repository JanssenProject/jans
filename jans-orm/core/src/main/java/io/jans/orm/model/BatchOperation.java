/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model;

import java.util.List;

/**
 * Batch operation
 *
 * @author Yuriy Movchan Date: 01/29/2018
 */
public interface BatchOperation<T> {

    boolean collectSearchResult(int size);

    void performAction(List<T> entries);

}
