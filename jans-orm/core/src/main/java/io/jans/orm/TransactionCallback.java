/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm;

/**
 * Unit of work run by {@link PersistenceEntryManager#executeInTransaction(String, TransactionCallback)}.
 *
 * @param <T> result type
 */
@FunctionalInterface
public interface TransactionCallback<T> {

	T doInTransaction() throws Exception;

}
